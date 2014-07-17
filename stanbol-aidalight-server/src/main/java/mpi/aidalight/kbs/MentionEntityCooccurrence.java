package mpi.aidalight.kbs;


import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;

import mpi.aidalight.DataStore;
import mpi.util.Standardize;
import mpi.util.Utils;
import basics.Normalize;


/**
 * 
 * @author datnb
 *
 */

public class MentionEntityCooccurrence {
  
  private static TIntIntHashMap mentionCounter = null;
  
  private static TIntObjectHashMap<TIntIntHashMap> mentionEntityCooccurrenceCounter = null;
  
  private static TObjectIntHashMap<String> mentionDictionary = null;
  
  
  public static void initMentionEntityCooccurrenceCounter() throws IOException {
    mentionDictionary = new TObjectIntHashMap<String>();
    for(String line: Utils.getContent(Utils.getProperty("mentionDictionary"))) {
      String str[] = line.split("\t");
      mentionDictionary.put(str[0], Integer.parseInt(str[1]));
    }
    
    mentionCounter = new TIntIntHashMap();
    for(String line: Utils.getContent(Utils.getProperty("mentionCounter"))) {
      String str[] = line.split("\t");
      mentionCounter.put(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
    }
    
    mentionEntityCooccurrenceCounter = new TIntObjectHashMap<TIntIntHashMap>();
    for(String line: Utils.getContent(Utils.getProperty("mentionEntityCooccurrenceCounter"))) {
      String str[] = line.split("\t");
      TIntIntHashMap entityCounter = new TIntIntHashMap();
      for(int i = 1; i < str.length; i +=2)
        entityCounter.put(Integer.parseInt(str[i]), Integer.parseInt(str[i+1]));
      mentionEntityCooccurrenceCounter.put(Integer.parseInt(str[0]), entityCounter);
    }
  }
  
  
  /***
   * 
   * @param entityCandidate
   * @param givenMention
   * @return the probability of entityCandidate occurs when a mention is given.
   */
  public static double getProbability(int entityCandidate, String givenMention) {
    String mention = Standardize.conflateMention(givenMention);
    String normMention = 
        Standardize.getPostgresEscapedString(Normalize.string(mention));
    if(mentionCounter == null || mentionEntityCooccurrenceCounter == null) {
      try {
        initMentionEntityCooccurrenceCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    // + 1 for smoothing
    // DataStore.NUMBER_ENTITIES is the size of dictionary. This is for smoothing.
    int mentionId = mentionDictionary.get(normMention);
    int mentionCount = mentionCounter.get(mentionId);
    TIntIntHashMap entityCounter = mentionEntityCooccurrenceCounter.get(mentionId);
    
    
 // The value might be too small
    // scale up. It doesn't affect the results at the end because we do normalize these values.
    int K = 1000000;
    if(entityCounter == null)
      return (double) K * 1.0 / (mentionCount + DataStore.NUMBER_ENTITIES);
    
    if(entityCounter.containsKey(entityCandidate)) 
      return (double) K * (entityCounter.get(entityCandidate) + 1) / (mentionCount + DataStore.NUMBER_ENTITIES);
    return (double) K * 1.0 / (mentionCount + DataStore.NUMBER_ENTITIES);
  }
  
  
  public static void main(String args[]) throws Exception {
//    countMentionEntityCooccurrence();
  }
}
