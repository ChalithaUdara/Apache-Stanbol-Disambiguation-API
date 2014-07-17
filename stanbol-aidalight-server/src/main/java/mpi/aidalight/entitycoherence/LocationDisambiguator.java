package mpi.aidalight.entitycoherence;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.MentionEntityMapping;

/**
 * 
 * @author datnb
 * 
 * This class process location mentions only.
 */
public class LocationDisambiguator {
  private Map<Mention, List<MentionEntityMapping>> mentionMappings;
  
  private Map<Mention, Entity> disambiguatedMentionEntity;
  
//  private GraphSettings graphSettings;
  
//  private Settings settings;
  
  public LocationDisambiguator(Map<Mention, List<MentionEntityMapping>> mentionMappings, Map<Mention, Entity> disambiguatedMentionEntity) {
    this.mentionMappings = mentionMappings;
    this.disambiguatedMentionEntity = disambiguatedMentionEntity;
//    this.settings = settings;
//    this.graphSettings = graphSettings;
  }
  
  private Map<Mention, List<MentionEntityMapping>> extractLocationMentions() throws Exception {
    Map<Mention, List<MentionEntityMapping>> locationMentionEntities = new HashMap<Mention, List<MentionEntityMapping>>();
    
    for(Mention mention: mentionMappings.keySet()) {
      if(disambiguatedMentionEntity.containsKey(mention) == false) {
        if(mention.getType().equalsIgnoreCase("LOCATION"))
          locationMentionEntities.put(mention, mentionMappings.get(mention));
      }
    }
    
    return locationMentionEntities;
  }
  
  
  public Map<Mention, Entity> disambiguate() throws Exception {
    Map<Mention, List<MentionEntityMapping>> locationMentionEntities = extractLocationMentions();
    Map<Mention, Entity> result = new HashMap<Mention, Entity>();
    for(Mention mention: locationMentionEntities.keySet()) {
      Entity bestEntity = null;
      double max = -2.0;
      
      for(MentionEntityMapping mapping: locationMentionEntities.get(mention)) {
        if(mapping.getLocalSimilarity() > max) {
          max = mapping.getLocalSimilarity();
          bestEntity = mapping.getEntity();
        }
      }
      
      if(bestEntity != null)  {
//        bestEntity.setLocalSimilarity(max);
        result.put(mention, bestEntity);
      }
    }
    
    return result;
  }
}
