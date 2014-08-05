/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true, inherit = true)
@Service
@Properties(value = {
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "aidalight-disambiguation"),
		@Property(name = AidalightDisambiguationEngine.DISAMBIGUATION_SERVICE_URL, value = "http://localhost:9090") })
public class AidalightDisambiguationEngine extends
		AbstractEnhancementEngine<IOException, RuntimeException> implements
		EnhancementEngine, ServiceProperties {

	/**
	 * Using slf4j for logging
	 */
	private static final Logger log = LoggerFactory
			.getLogger(AidalightDisambiguationEngine.class);

	private final static LiteralFactory literalFactory = LiteralFactory
			.getInstance();

	public static final String DISAMBIGUATION_SERVICE_URL = "enhancer.engine.aidalight-disambiguation.service";

	private Map<String, TextAnnotation> textAnnotationMap;

	private Dictionary<String, Object> config;

	private Client client;

	private String service_url;

	private static final String RESOURCE_PATTERN = "http://yago-knowledge.org/resource/";

	/**
	 * This ensures this engine will be used as a post processing engine
	 */
	public static final Integer defaultOrder = ServiceProperties.ORDERING_POST_PROCESSING - 90;

	/**
	 * Returns the properties containing the
	 * {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING}
	 */
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}

	/**
	 * @return if and how (asynchronously) we can enhance a ContentItem
	 */
	public int canEnhance(ContentItem ci) throws EngineException {
		// check if content is present
		try {
			if ((ContentItemHelper.getText(ci.getBlob()) == null)
					|| (ContentItemHelper.getText(ci.getBlob()).trim()
							.isEmpty())) {
				return CANNOT_ENHANCE;
			}
		} catch (IOException e) {
			log.error("Failed to get the text for "
					+ "enhancement of content: " + ci.getUri(), e);
			throw new InvalidContentException(this, ci, e);
		}
		// no reason why we should require to be executed synchronously
		return ENHANCE_ASYNC;
	}

	public void computeEnhancements(ContentItem ci) throws EngineException {

		String text = null;
		try {
			text = ContentItemHelper.getText(ci.getBlob());
		} catch (IOException e) {
			log.warn("Unable to get text content from ContentItem");
		}

		log.info("Text Content: " + text);
		textAnnotationMap = new HashMap<String, TextAnnotation>();

		MGraph graph = ci.getMetadata();
		ci.getLock().readLock().lock();
		try {
			this.processMetaData(graph);
		} finally {
			ci.getLock().readLock().unlock();
		}
		// prepare data to send for disambiguation service
		List<MentionAnnotation> mentionAnnotations = new ArrayList<MentionAnnotation>();
		for (TextAnnotation textAnnotation : textAnnotationMap.values()) {
			MentionAnnotation mentionAnnotation = new MentionAnnotation();
			mentionAnnotation.textAnnotation = textAnnotation.getName();
			mentionAnnotation.start = textAnnotation.getStart();
			mentionAnnotation.end = textAnnotation.getEnd();

			mentionAnnotations.add(mentionAnnotation);
		}

		DisambiguationData disambiguationData = new DisambiguationData();
		disambiguationData.text = text;
		disambiguationData.mentionAnnotations = mentionAnnotations;

		WebTarget target = client.target(service_url).path(
				"/service/disambiguate-default");

		Response response = target.request().post(
				Entity.entity(disambiguationData, MediaType.APPLICATION_XML));

		if (response.getStatus() == 200) {
			List<DisambiguationResult> results = response
					.readEntity(new GenericType<List<DisambiguationResult>>() {
					});
			for (DisambiguationResult disambiguationResult : results) {
				log.info("Disambiguation Result: "
						+ disambiguationResult.toString());
				String key = Integer.toString(disambiguationResult.start) + "_"
						+ Integer.toString(disambiguationResult.end);
				TextAnnotation selectedTextAnnotation = textAnnotationMap
						.get(key);
				Map<String, EntityAnnotation> entityAnnotationMap = selectedTextAnnotation
						.getEntityAnnotationsMap();
				//update confidence values if there are more than one suggested entities
				if(entityAnnotationMap.size() > 1){
					ci.getLock().writeLock().lock();
					try{
						EntityAnnotation disambiguatedEntity = entityAnnotationMap.get(disambiguationResult.entity);
						log.info("Disambiguated Entity: "+disambiguatedEntity);
						if(disambiguatedEntity != null){
							UriRef disambiguatedEntityUri = disambiguatedEntity.getUri();
							EnhancementEngineHelper.set(graph, disambiguatedEntityUri, ENHANCER_CONFIDENCE,
			                        disambiguatedEntity.getDisambiguationConfidence(), literalFactory);
							EnhancementEngineHelper.addContributingEngine(graph, disambiguatedEntityUri, this);
						}
					}
					finally{
						ci.getLock().writeLock().unlock();
					}
				}
			}
		}

	}

	public void processMetaData(MGraph graph) {
		Iterator<Triple> it = graph.filter(null, RDF_TYPE,
				TechnicalClasses.ENHANCER_TEXTANNOTATION);
		while (it.hasNext()) {

			// get the properties of text annotations
			UriRef uri = (UriRef) it.next().getSubject();
			String textName = EnhancementEngineHelper.getString(graph, uri,
					ENHANCER_SELECTED_TEXT);
			if (textName == null) {
				log.debug("Could not create the text annotation for " + uri);
				continue;
			}
			TextAnnotation textAnnotation = new TextAnnotation();
			textAnnotation.setUri(uri);
			textAnnotation.setName(textName);

			textAnnotation.setStart(EnhancementEngineHelper.get(graph, uri,
					ENHANCER_START, Integer.class, literalFactory));
			textAnnotation.setEnd(EnhancementEngineHelper.get(graph, uri,
					ENHANCER_END, Integer.class, literalFactory));

			log.info("Text URI: " + uri + ", Text_Annotation: "
					+ textAnnotation.getName() + ", Start: "
					+ textAnnotation.getStart() + ", End: "
					+ textAnnotation.getEnd());
			// for each text annotation, get selected entity annotations
			Map<String, EntityAnnotation> entityAnnotationMap = new HashMap<String, EntityAnnotation>();
			Iterator<Triple> entityAnnotations = graph.filter(null,
					DC_RELATION, uri);
			while (entityAnnotations.hasNext()) {
				UriRef entityUri = (UriRef) entityAnnotations.next()
						.getSubject();
				UriRef entityRef = EnhancementEngineHelper.getReference(graph,
						entityUri, ENHANCER_ENTITY_REFERENCE);

				if (entityRef == null) {
					continue;
				}
				EntityAnnotation entityAnnotation = new EntityAnnotation();
				entityAnnotation.setUri(entityUri);
				entityAnnotation.setEntityReference(entityRef);
				entityAnnotation
						.setDisambiguationConfidence(EnhancementEngineHelper
								.get(graph, entityUri, ENHANCER_CONFIDENCE,
										Double.class, literalFactory));

				// use entity name as the key derived from entity reference
				String entityUrl = entityRef.toString().substring(1,
						entityRef.toString().length() - 1);
				String[] results = entityUrl.split(RESOURCE_PATTERN);

				entityAnnotationMap.put(results[1], entityAnnotation);
				log.info("Entity URI: " + entityUri + ", Reference: "
						+ entityRef + ", Entity Name: " + results[1]);
			}

			textAnnotation.setEntityAnnotationsMap(entityAnnotationMap);
			String textAnnotationKey = Integer.toString(textAnnotation
					.getStart())
					+ "_"
					+ Integer.toString(textAnnotation.getEnd());
			textAnnotationMap.put(textAnnotationKey, textAnnotation);
			// just to test text annotations are obtained correctly
			// EnhancementEngineHelper.set(graph, uri,
			// ENHANCER_SELECTED_TEXT, "CHANGED_BY_ME",
			// LiteralFactory.getInstance());
			// EnhancementEngineHelper.addContributingEngine(graph, uri,
			// this);
		}
	}

	@Activate
	protected void activate(ComponentContext ce) throws ConfigurationException {
		try {
			super.activate(ce);
			config = ce.getProperties();
			Object value = config.get(DISAMBIGUATION_SERVICE_URL);
			if (value == null) {
				throw new ConfigurationException(DISAMBIGUATION_SERVICE_URL,
						"Disambiguation Service URL is missing in the configuaration");
			}
			service_url = value.toString();
			log.info("Service URL: " + service_url);
			client = ClientBuilder.newClient();
		} catch (IOException e) {
			log.error("Failed to update the configuration", e);
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
		client = null;
	}

}
