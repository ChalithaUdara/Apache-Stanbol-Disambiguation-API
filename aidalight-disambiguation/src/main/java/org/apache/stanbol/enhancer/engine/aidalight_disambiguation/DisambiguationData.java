package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="DisambiguationData")
public class DisambiguationData {
	
	@XmlElement
	protected List<MentionAnnotation> mentionAnnotations;
	
	@XmlElement
	protected String text;
	
	public DisambiguationData(){
		
	}
	
}
