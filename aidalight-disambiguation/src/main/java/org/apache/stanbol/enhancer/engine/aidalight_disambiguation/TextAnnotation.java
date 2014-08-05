package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;

public class TextAnnotation {
	private UriRef uri;
	private String name;
	private int start;
	private int end;
	
	private Map<String, EntityAnnotation> entityAnnotationsMap;
	
	public TextAnnotation(){
		
	}

	public UriRef getUri() {
		return uri;
	}

	public void setUri(UriRef uri) {
		this.uri = uri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public Map<String, EntityAnnotation> getEntityAnnotationsMap() {
		return entityAnnotationsMap;
	}

	public void setEntityAnnotationsMap(Map<String, EntityAnnotation> entityAnnotationsMap) {
		this.entityAnnotationsMap = entityAnnotationsMap;
	}
}
