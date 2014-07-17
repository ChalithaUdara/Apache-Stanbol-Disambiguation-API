package mpi.aidalight;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;


/**
 * This is a mapping between a mention and an entity candidate.
 * The confidence and localSimilarity are to rank the candidate list.
 * 
 * @author datnb
 *
 */

public class MentionEntityMapping {
  /*
   * The mention in this mapping
   */
  private Mention mention;
  
  
  /*
   * The entity candidate is mapped
   */
  private Entity entity;
  
  
  /*
   * Local similarity is the similarity of the local context of a mention and the context of an entity. 
   */
  private double localSimilarity;
  
  
  /*
   * The probability of this mapping.
   */
  private double probability;
  
  
  public MentionEntityMapping(Mention mention, Entity entity, double localSimilarity, double probability) {
    this.mention = mention;
    this.entity = entity;
    this.entity.setLocalSimilarity(localSimilarity);
    this.localSimilarity = localSimilarity;
    this.probability = probability;
  }
  
  
  public Mention getMention() {
    return mention;
  }
  
  
  public Entity getEntity() {
    return entity;
  }
  
  
  public double getLocalSimilarity() {
    return localSimilarity;
  }
  
  
  public void setLocalSimilarity(double localSimilarity) {
    this.localSimilarity = localSimilarity;
    this.entity.setLocalSimilarity(localSimilarity);
  }
  
  public double getProbability() {
    return probability;
  }
  
  public long getId() {
    return (long)mention.hashCode() * 6000011 + entity.hashCode();
  }
  
}
