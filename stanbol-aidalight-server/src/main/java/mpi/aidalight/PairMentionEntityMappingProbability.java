package mpi.aidalight;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Mention;

/**
 * 
 * @author datnb
 *
 */

public class PairMentionEntityMappingProbability {
  /*
   * mention and K best candidates.
   */
  private Map<Mention, List<MentionEntityMapping>> mentionEntityCandidates;
  
  /*
   * mentions are sorted by offsets.
   */
  private Mention[] mentions;
  
  
  /*
   * due to relatedness between two entities might be 0, should add an epsilon value to this.
   */
  private double epsilon;
  
  
  private Settings settings;
  
 
  private TObjectDoubleHashMap<LongPair> probabilities;
  
  
  public PairMentionEntityMappingProbability(Map<Mention, List<MentionEntityMapping>> mentionEntityCandidates, Settings settings, double epsilon) {
    this.mentionEntityCandidates = mentionEntityCandidates;
    this.settings = settings;
    this.epsilon = epsilon;
    
    mentions = new Mention[mentionEntityCandidates.size()];
    int vt = 0;
    for(Mention mention: mentionEntityCandidates.keySet())
      mentions[vt++] = mention;
    Arrays.sort(mentions);
    
    init();
  }
  
  
  private void init() {
    probabilities = new TObjectDoubleHashMap<LongPair>();
    
    for(int i = 0; i < mentions.length; i ++)
      for(int j = i + 1; j < mentions.length; j ++) {
        double sum = 0.0;
        for(MentionEntityMapping mep1: mentionEntityCandidates.get(mentions[i])) {
          for(MentionEntityMapping mep2: mentionEntityCandidates.get(mentions[j])) {
            LongPair lp = new LongPair(mep1.getId(), mep2.getId());
            double d = mep1.getProbability() * mep2.getProbability() * 
                (Function.getEntityEntityRelatedness(mep1.getEntity(), mep2.getEntity(), settings, null) + epsilon);
            sum += d;
            probabilities.put(lp, d);
          }
        }
        
        for(MentionEntityMapping mep1: mentionEntityCandidates.get(mentions[i])) {
          for(MentionEntityMapping mep2: mentionEntityCandidates.get(mentions[j])) {
            LongPair lp = new LongPair(mep1.getId(), mep2.getId());
            double d = probabilities.get(lp) / sum;
            probabilities.put(lp, d);
          }
        }
      }
    
//    for(int i = 0; i < mentions.length - 1; i ++) {
//      double sum = 0.0;
//      for(MentionEntityMapping mep1: mentionEntityCandidates.get(mentions[i])) {
//        for(MentionEntityMapping mep2: mentionEntityCandidates.get(mentions[i+1])) {
//          LongPair lp = new LongPair(mep1.getId(), mep2.getId());
//          double d = mep1.getProbability() * mep2.getProbability() * 
//              (Function.getEntityEntityRelatedness(mep1.getEntity(), mep2.getEntity(), settings) + epsilon);
//          sum += d;
//          probabilities.put(lp, d);
//        }
//      }
//      
//      for(MentionEntityMapping mep1: mentionEntityCandidates.get(mentions[i])) {
//        for(MentionEntityMapping mep2: mentionEntityCandidates.get(mentions[i+1])) {
//          LongPair lp = new LongPair(mep1.getId(), mep2.getId());
//          double d = probabilities.get(lp) / sum;
//          probabilities.put(lp, d);
//        }
//      }
//    }
  }
  
  
  public double getProbability(MentionEntityMapping mep1, MentionEntityMapping mep2) {
    LongPair lp = new LongPair(mep1.getId(), mep2.getId());
    if(probabilities.containsKey(lp))
      return probabilities.get(lp);
    return 0;
  }
  
  
  class LongPair implements Comparable<LongPair>{
    long first;
    long second;
    
    public LongPair(long first, long second) {
      this.first = first;
      this.second = second;
    }
    
    @Override
    public int compareTo(LongPair lp) {
      if(first > lp.first)
        return 1;
      if(first == lp.first) {
        if(second > lp.second)
          return 1;
        if(second == lp.second)
          return 0;
      }
      return -1;
    }
    
    @Override
    public int hashCode() {
      return new Long(first * 1000000000000l + second).hashCode();
    }
    
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LongPair) {
        LongPair lp = (LongPair) obj;
        return first == lp.first && second == lp.second;
      } else {
        return false;
      }
    }
  }
}
