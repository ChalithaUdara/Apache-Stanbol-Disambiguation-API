package mpi.aidalight.entitycoherence;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;

public class EntityNode implements Comparable<EntityNode>{
	private Entity entity;
	private Mention mention;
	private double weight = 0.0;
	private int N = 0; // number of related entities
	
	public EntityNode(Entity entity, Mention mention, double weight){
		this.entity = entity;
		this.mention = mention;
		this.weight = weight;
	}
	
	public int getNumberOfRelatedEntities() {
	  return N;
	}
	
	public void setNumberOfRelatedEntities(int N) {
	  this.N = N;
	}
	
	public EntityNode(Entity entity){
		this.entity = entity;
	}
	
	public Mention getMention(){
		return mention;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public void setWeight(double weight){
		this.weight = weight;
	}
	
	public double getWeight(){
		return weight;
	}
	
	@Override
	public int compareTo(EntityNode e){
		if(weight > e.getWeight())
			return 1;
		return weight == e.getWeight() ? 0:-1;
	}
}
