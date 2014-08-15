package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import static org.junit.Assert.*;

public class TestAidalightDisambiguationEngine {

	private static final String TEXT = "With United, Beckham won the Premier League title 6 times";

	private static ContentItemFactory ciFactory = InMemoryContentItemFactory
			.getInstance();

	private AidalightDisambiguationEngine disambiguationEngine;

	@Before
	public void setup() {
		disambiguationEngine = new AidalightDisambiguationEngine();
	}

	public static ContentItem prepareContentItem() throws IOException {
		ContentItem ci = ciFactory.createContentItem(new UriRef(
				"urn:test:content-item"), new StringSource(TEXT));

		LiteralFactory lf = LiteralFactory.getInstance();
		// add text annotations for contentItem
		MGraph graph = ci.getMetadata();

		UriRef textAnnotationUnited = EnhancementEngineHelper
				.createTextEnhancement(ci, new AidalightDisambiguationEngine());
		graph.add(new TripleImpl(textAnnotationUnited, ENHANCER_SELECTED_TEXT,
				new PlainLiteralImpl("United")));
		graph.add(new TripleImpl(textAnnotationUnited, ENHANCER_START, lf
				.createTypedLiteral(5)));
		graph.add(new TripleImpl(textAnnotationUnited, ENHANCER_END, lf
				.createTypedLiteral(11)));

		UriRef textAnnotationBeckham = EnhancementEngineHelper
				.createTextEnhancement(ci, new AidalightDisambiguationEngine());
		graph.add(new TripleImpl(textAnnotationBeckham, ENHANCER_SELECTED_TEXT,
				new PlainLiteralImpl("Beckham")));
		graph.add(new TripleImpl(textAnnotationBeckham, ENHANCER_START, lf
				.createTypedLiteral(13)));
		graph.add(new TripleImpl(textAnnotationBeckham, ENHANCER_END, lf
				.createTypedLiteral(20)));

		UriRef textAnnotationLeague = EnhancementEngineHelper
				.createTextEnhancement(ci, new AidalightDisambiguationEngine());
		graph.add(new TripleImpl(textAnnotationLeague, ENHANCER_SELECTED_TEXT,
				new PlainLiteralImpl("Premier League")));
		graph.add(new TripleImpl(textAnnotationLeague, ENHANCER_START, lf
				.createTypedLiteral(29)));
		graph.add(new TripleImpl(textAnnotationLeague, ENHANCER_END, lf
				.createTypedLiteral(43)));

		return ci;
	}

	@Test
	public void testProcessMetadata() throws IOException, EngineException,
			ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME,
				"aidalight-disambiguation");
		properties.put(
				AidalightDisambiguationEngine.DISAMBIGUATION_SERVICE_URL,
				"http://localhost:9090");

		disambiguationEngine.activate(new MockComponentContext(properties));

		ContentItem ci = TestAidalightDisambiguationEngine.prepareContentItem();
		disambiguationEngine.processMetaData(ci.getMetadata());
		Map<String, TextAnnotation> textAnnotationMap = disambiguationEngine
				.getTextMap();

		// test keys
		assertTrue(textAnnotationMap.containsKey("5_11"));
		assertTrue(textAnnotationMap.containsKey("13_20"));
		assertTrue(textAnnotationMap.containsKey("29_43"));

		// test values
		assertEquals(textAnnotationMap.get("5_11").getName(), "United");
		assertEquals(textAnnotationMap.get("13_20").getName(), "Beckham");
		assertEquals(textAnnotationMap.get("29_43").getName(), "Premier League");
	}
	
	@Test(expected = ConfigurationException.class)
	public void testEmptyServerURL() throws ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME,
				"aidalight-disambiguation");

		disambiguationEngine.activate(new MockComponentContext(properties));
	}
}
