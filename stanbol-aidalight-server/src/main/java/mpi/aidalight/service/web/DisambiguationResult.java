package mpi.aidalight.service.web;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Disambiguation")
public class DisambiguationResult {
	
	@XmlElement
	protected String mention;
	
	@XmlElement
	protected String entity;
	
	@XmlElement
	protected double disambiguationConfidence;
	
	public DisambiguationResult(){
		
	}
	
}
