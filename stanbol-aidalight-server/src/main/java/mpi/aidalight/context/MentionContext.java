package mpi.aidalight.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * The LocalContext of a mention is the surrounding context in the input text including text (tokens), 
 * mentions, etc.
 * 
 * @author datnb
 *
 */

public class MentionContext implements Serializable{
  
  /*
   * ids of tokens surrounding the mention. Basically, stopwords are not counted. 
   * surroundingTokens is always sorted.
   */
  private int[] surroundingTokens;
  
  
  /*
   * strings of mentions surrounding. This is to compute the probability of an entity candidate 
   * with some given mentions in the context.
   */
  private List<String> surroundingMentions;
  
  
  public MentionContext(int[] surroundingTokens, List<String> surroundingMentions) {
    this.surroundingTokens = surroundingTokens;
    Arrays.sort(this.surroundingTokens);
    this.surroundingMentions = surroundingMentions;
  }
  
  
  /**
   * 
   * @return tokens around the mention. This is the feature vector of a mention.
   */
  public int[] getSurroundingTokens() {
    return surroundingTokens;
  }
  
  /**
   * add surroundingTokens.
   * 
   * @param toks: surroundingTokens of another mention which points to the same entity.
   * toks is already sorted.
   */
  private void addTokensToSurroundingTokens(int[] toks) {
    int[] res = new int[surroundingTokens.length + toks.length];
    int size = 0;
    int i = 0, j = 0;
    while(i < surroundingTokens.length && j < toks.length) {
      if(surroundingTokens[i] < toks[j]) {
        res[size++] = surroundingTokens[i++];
      }
      else if(surroundingTokens[i] == toks[j]) {
        res[size++] = surroundingTokens[i++];
        j++;
      }
      else {
        res[size++] = toks[j++];
      }
    }
    while(i < surroundingTokens.length)
      res[size++] = surroundingTokens[i++];
    while(j < toks.length)
      res[size++] = toks[j++];
    
    // remove empty elements at the end, and update
    surroundingTokens = new int[size];
    System.arraycopy(res, 0, surroundingTokens, 0, size);
  }
  
  
  /**
   * 
   * @return list of mentions nearby. This is to compute the probability of an entity candidate 
   * with some given mentions in the context.
   */
  public List<String> getSurroundingMentions() {
    return surroundingMentions;
  }
  
  public void setSurroundingMentions(List<String> surroundingMentions) {
    this.surroundingMentions = surroundingMentions;
  }
  
  
  /**
   * 
   * add surroundingMentions.
   * 
   * @param mentions
   */
  private void addMentionsToSurroundingMentions(List<String> mentions) {
    Set<String> tmp = new HashSet<String>(this.surroundingMentions);
    tmp.addAll(mentions);
    surroundingMentions = new ArrayList<String>(tmp);
  }
  

  
  /**
   * When 2 mentions are the same, the surrounding context should be merged. By doing this,
   * we have a richer feature vector, and thus, helps to improve the quality.
   * 
   * @param localContext
   */
  public void mergeLocalContext(MentionContext localContext) {
    addMentionsToSurroundingMentions(localContext.getSurroundingMentions());
    addTokensToSurroundingTokens(localContext.getSurroundingTokens());
  }
  
  
}
