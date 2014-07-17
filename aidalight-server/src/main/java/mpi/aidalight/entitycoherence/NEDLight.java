package mpi.aidalight.entitycoherence;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.MentionEntityMapping;
import mpi.aidalight.Settings;

/**
 * 
 * @author datnb
 * 
 * This class process un/less ambiguous mentions.
 * Mentions with very few candidates or represented by a long string are processed here.
 */
public class NEDLight {
  private Map<Mention, List<MentionEntityMapping>> mentionMappings;
  
  private GraphSettings graphSettings;
  
  private Settings settings;
  
  public NEDLight(Map<Mention, List<MentionEntityMapping>> mentionMappings, Settings settings, GraphSettings graphSettings) {
    this.mentionMappings = mentionMappings;
    this.settings = settings;
    this.graphSettings = graphSettings;
  }
  
  private Map<Mention, List<MentionEntityMapping>> extractUnambiguousMentionEntities() throws Exception {
    Map<Mention, List<MentionEntityMapping>> unambiguousMention2entities = new HashMap<Mention, List<MentionEntityMapping>>();
    
    for(Mention mention: mentionMappings.keySet()) {
      List<MentionEntityMapping> mappings = mentionMappings.get(mention);
      if(mappings.size() > 0 && mappings.size() <= 3) 
        unambiguousMention2entities.put(mention, mentionMappings.get(mention));
    }
    
    if(unambiguousMention2entities.size() == 0 && graphSettings.isPreporcessingMentionsRankedByEntropies()) {
      double percentage = graphSettings.getPercentageOfMentionsProcessedInFirstRound();
      int K = (int) (mentionMappings.size() * percentage);
      if(K == 0)
        return unambiguousMention2entities;
      
      double max[] = new double[K];
      Mention[] bestMentions = new Mention[K];
      for(Mention mention: mentionMappings.keySet()) {
        double entropy = 0.0;
        TIntDoubleHashMap priors = DataStore.getEntityPriors(mention.getMention());
        for(int id: priors.keys()) {
          double d = priors.get(id);
          entropy -= d * Math.log(d) / Math.log(2);
        }
        mention.setEntropy(entropy);
        
        for(int i = 0; i < K; i++){
          if(max[i] < entropy){
            // update
            for(int j = K - 1; j > i; j--){
              max[j] = max[j-1];
              bestMentions[j] = bestMentions[j-1];
            }
            max[i] = entropy;
            bestMentions[i] = mention;
            break;
          }
        }
        
      }

      for(int i = 0; i < K; i++) {
         if(bestMentions[i] != null) {
           unambiguousMention2entities.put(bestMentions[i], mentionMappings.get(bestMentions[i]));
         }
         else
           break;
      }
    }
    return unambiguousMention2entities;
  }
  
  
  public Map<Mention, Entity> disambiguate() throws Exception {
    Map<Mention, List<MentionEntityMapping>> unambiguousMention2entities = extractUnambiguousMentionEntities();
    // no disambiguated entity, no related domain
    return new BottomUpGreedyGraph(unambiguousMention2entities, new HashMap<Mention, Entity>(), settings, graphSettings).disambiguate();
  }
}
