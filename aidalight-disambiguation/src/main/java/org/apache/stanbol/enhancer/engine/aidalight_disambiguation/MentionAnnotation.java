package org.apache.stanbol.enhancer.engine.aidalight_disambiguation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MentionAnnotation")
public class MentionAnnotation {

	@XmlElement
	protected String textAnnotation;
	
	@XmlElement
	protected int start;
	
	@XmlElement
	protected int end;
	
	public MentionAnnotation(){
		
	}
}
