package mpi.aidalight.kbs;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.IOException;

import mpi.aidalight.DataStore;
import mpi.util.Utils;


public class EntityEntityCooccurrence {
  
  private static TIntIntHashMap entityCounter = null;
  
  private static TLongIntHashMap cooccurrenceCounter = null;
  
  
  public static void initEntityEntityCooccurrenceCounter() throws IOException {
    entityCounter = new TIntIntHashMap();
    for(String line: Utils.getContent(Utils.getProperty("entityCounter"))) {
      String str[] = line.split("\t");
      entityCounter.put(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
    }
    
    cooccurrenceCounter = new TLongIntHashMap();
    for(String line: Utils.getContent(Utils.getProperty("cooccurrenceCounter"))) {
      String str[] = line.split("\t");
      cooccurrenceCounter.put(Long.parseLong(str[0]), Integer.parseInt(str[1]));
    }
  }
  
  
  /***
   * 
   * @param entityCandidate
   * @param givenEntity
   * @return the probability of entityCandidate occurs when givenEntity is given.
   */
  public static double getProbability(int entityCandidate, int givenEntity) {
    if(entityCounter == null || cooccurrenceCounter == null) {
      try {
        initEntityEntityCooccurrenceCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if(entityCandidate == givenEntity)
      return 1.0;
    long id = concatenate(entityCandidate, givenEntity);
    // + 1 for smoothing
    // DataStore.NUMBER_ENTITIES is the size of dictionary. This is for smoothing.
    int K = 1000000;
    if(cooccurrenceCounter.contains(id))
      return (double) K * (cooccurrenceCounter.get(id) + 1) / (entityCounter.get(givenEntity) + DataStore.NUMBER_ENTITIES);
    return (double) K * 1.0 / (entityCounter.get(givenEntity) + DataStore.NUMBER_ENTITIES);
  }
  
  private static long concatenate(int a, int b) {
    long MAX = 4000000l;
    if(a > b)
      return concatenate(b, a);
    return (long) a * MAX + b;
  }
  
}
