package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Disambiguation")
public class DisambiguationResult {

	@XmlElement
	protected String mention;

	@XmlElement
	protected String entity;

	@XmlElement
	protected int start;

	@XmlElement
	protected int end;

	@XmlElement
	protected double disambiguationConfidence;

	public DisambiguationResult() {

	}

	public String toString() {
		return "mention: " + mention + ", entity: " + entity + ", Confidence: "
				+ disambiguationConfidence+", mention start: "+start
				+", mention end: "+end;
	}

}
