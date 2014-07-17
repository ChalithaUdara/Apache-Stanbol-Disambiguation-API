package mpi.aidalight.context;

import gnu.trove.set.hash.TIntHashSet;

import java.util.Set;

import mpi.aidalight.DataStore;




public class EntityGlobalContext {
  /*
   * the unique id of the entity which holds this context
   */
  private int entityId;
  
  
  
  /*
   * tokens from all key-phrases of this entity.
   */
  private int[] tokens;
  
  
  /*
   * wikicategories, which belongs to this entity, helps to evaluate the relatedness
   * between 2 entities better. 
   */
  private Set<String> wikiCategories;
  
  
  /*
   * all related entities including those which have a relation with this entity, or
   * those which have an internal link with this entity in wikipedia. 
   */
  private TIntHashSet relatedEntities;
  
  
  public EntityGlobalContext(int entityId) {
    this.entityId = entityId;
    init();
  }
  
  
  /**
   * extract from global context all data required including wiki-categories and related entities, etc.
   */
  private void init() {
    wikiCategories = DataStore.getWikiTypes(entityId);
    relatedEntities = DataStore.getRelatedEntities(entityId);
    tokens = DataStore.getTokens(entityId);
  }
  
  
  /**
   * 
   * @return the unique id of the entity holding this context
   */
  public int getEntityId() {
    return entityId;
  }
  
  
  /**
   * 
   * @return all wikicategories belonging to this entity.
   */
  public Set<String> getWikiCategories() {
    return wikiCategories;
  }
  
  
  /**
   * 
   * @return all related entities with this entity. They migh have a relation with this entity, or
   * have an internal link with this entity in wikipedia. 
   */
  public TIntHashSet getRelatedEntities() {
    return relatedEntities;
  }
  
  
  /**
   * 
   * @return the token set extracted from all key-phrases of this entity
   */
  public int[] getKeyPhraseTokens() {
    return tokens;
  }
}
