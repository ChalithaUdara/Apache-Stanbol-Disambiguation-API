package mpi.aidalight.context;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aidalight.DataStore;
import mpi.aidalight.entitycoherence.NED;
import mpi.lsh.rmi.LSHServer;


/**
 * This is information of a mention from the global view. For example, from ontologies.
 * 
 * 
 * @author datnb
 *
 */
public class MentionGlobalContext {
  /*
   * the id of this global context. It's a concatenation of mention and type.
   */
  private String id;
  
  
  /*
   * all possible candidates for this mention.
   */
  private Entities entityCandidates;
  
  
  
  /*
   * the entity-prior map
   */
  private TIntDoubleHashMap priors;
  
  
  
  /*
   * in case of on-the-fly compute idf token weights
   */
  private TIntDoubleHashMap tokenWeights;
  
  
  
  public MentionGlobalContext(String mention, String type, int[] tokens, boolean onTheFlyIDFTokenWeights) {
    this.id = mention + "$" + type;
    
    /*
     * extract all data required including the candidate set and the entity-prior map 
     * from GlobalContext (e.g. in Wikipedia KBs).
     */
    Entities entities = DataStore.getEntitiesForMention(mention, type);
    if(entities == null || entities.size() == 0) {
      if(NED.partlyMatchingMentionEntitySearch) {
        // try to find similar names
        Set<String> names = null;
        if(NED.lshMeansTableRMI) {
          LSHServer server = null;
          String host = "localhost";
          try {
            Registry registry = LocateRegistry.getRegistry(host, 52378);
            server = (LSHServer) registry.lookup("LSHServer_" + host);
            names = server.getSimilarName(mention);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        else {
          names = DataStore.getSimilarName(mention);
        }
        if(names != null) {
          for(String name: names) {
            entities.addAll(DataStore.getEntitiesForMention(name, type));
          }
        }
      }
    }
    this.entityCandidates = entities;
    
    
    priors = DataStore.getEntityPriors(mention);
    
    if(onTheFlyIDFTokenWeights) {
      tokenWeights = new TIntDoubleHashMap();
      TIntIntHashMap counter = new TIntIntHashMap();
      for(int token: tokens)
        counter.put(token, 1);
      for(Entity entity: entityCandidates) {
        for(int token: DataStore.getTokens(entity.getId())) {
          if(counter.containsKey(token)) {
            counter.put(token, counter.get(token) + 1);
          }
          else {
            counter.put(token, 1);
          }
        }
      }
      
      for(int token: counter.keys()) {
        tokenWeights.put(token, Math.log((entityCandidates.size() + 1) / counter.get(token)));
      }
      counter = null;
    }
  }
  
  
  public TIntDoubleHashMap getTokenWeights() {
    return tokenWeights;
  }
  
  
  
  
  public String getId() {
    return id;
  }
  
  
  /**
   * 
   * @return entity candidates for this mention.
   */
  public Entities getEntityCandidates() {
    return entityCandidates;
  }
  
  
  
  /**
   * 
   * @return the entityCandidate-prior map.
   */
  public TIntDoubleHashMap getPriors() {
    return priors;
  }
}
