package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import org.apache.clerezza.rdf.core.UriRef;

public class EntityAnnotation {
	private UriRef uri;
	private UriRef entityReference;
	private double disambiguationConfidence;

	public EntityAnnotation(){
		
	}
	
	public UriRef getUri() {
		return uri;
	}

	public void setUri(UriRef uri) {
		this.uri = uri;
	}

	public UriRef getEntityReference() {
		return entityReference;
	}

	public void setEntityReference(UriRef entityReference) {
		this.entityReference = entityReference;
	}

	public double getDisambiguationConfidence() {
		return disambiguationConfidence;
	}

	public void setDisambiguationConfidence(double disambiguationConfidence) {
		this.disambiguationConfidence = disambiguationConfidence;
	}
	
	public String toString(){
		return "Entity: "+entityReference.toString();
	}
}
