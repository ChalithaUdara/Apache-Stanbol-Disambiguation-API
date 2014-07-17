package mpi.aidalight.entitycoherence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Mention;
import mpi.aidalight.context.MentionContext;


public class MentionCombiner {
  public List<Mention> mentions;
  
  public MentionCombiner(List<Mention> mentions) {
    this.mentions = mentions;
  }
  
  public Map<Mention, Mention> deduplicate(){
    // combine some person names. E.g. Robson & Bobby Robson
    // if a short names can be combined with 2 different names (e.g. Robson, Bobby Robson, Bryan Robson), don't combine.
    Map<Mention, Mention> p_map = new HashMap<Mention, Mention>();
    Set<Mention> blackList = new HashSet<Mention>();
    for(Mention mention1: mentions) {
      if(mention1.getType().equalsIgnoreCase("PERSON")) {
//      if(DataStore.isNation(mention1.getMention()) == false) {
        if(blackList.contains(mention1))
          continue;
        for(Mention mention2: mentions) {
          if(mention2.getType().equalsIgnoreCase("PERSON")) {
//          if(DataStore.isNation(mention2.getMention()) && mention2.getType().equalsIgnoreCase(mention1.getType())) {
            String name1 = mention1.getMention().toLowerCase();
            String name2 = mention2.getMention().toLowerCase();
            if(name1.equalsIgnoreCase(name2) == false) {
              // if name2 holds name1
              if(checkShortForm(name1, name2) && name2.length() > name1.length()){
                Mention tmp = p_map.get(mention1);
                if(tmp == null) {
                  p_map.put(mention1, mention2);
                }
                else{
                  if(checkShortForm(tmp.getMention().toLowerCase(), mention2.getMention().toLowerCase())) {
                    if(mention2.getMention().length() > tmp.getMention().length())
                      p_map.put(mention1, mention2); // update
                  }
                  else {
                    // not good
                    blackList.add(mention1);
                    p_map.remove(mention1);
                  }
                }
              }
            }
          }
        }
      }
    }
    
    Map<Mention, Mention> res = new HashMap<Mention, Mention>();
    Map<String, Mention> cluster = new HashMap<String, Mention>();
    for(Mention mention: mentions){
      // concatenate mention & type
      String str = mention.getMention().toLowerCase() + "$" + mention.getType();
      if(cluster.containsKey(str)){
        // the same entity
        Mention m = cluster.get(str);
        // update context
        MentionContext m_context = m.getMentionContext();
        m_context.mergeLocalContext(mention.getMentionContext());
        
        while(p_map.containsKey(m))
          m = p_map.get(m);
        res.put(mention, m);
      }
      else{
        // new key
        cluster.put(str, mention);
        if(p_map.containsKey(mention)) {
          Mention iter = p_map.get(mention);
          while(p_map.containsKey(iter))
            iter = p_map.get(iter);
          res.put(mention, iter);
        }
        else {
          res.put(mention, mention);
        }
      }
    }
    
    return res;
  }
  
  /*
   * return true if a string is a short form of the other.
   */
  private boolean checkShortForm(String str1, String str2) {
    String[] tok1 = str1.split(" ");
    String[] tok2 = str2.split(" ");
    Set<String> set = new HashSet<String>();
    for(String token: tok1)
      set.add(token);
    int counter = 0;
    for(String token: tok2)
      if(set.contains(token))
        counter++;
    if(counter == Math.min(tok1.length, tok2.length))
      return true;
    return false;
  }
}
