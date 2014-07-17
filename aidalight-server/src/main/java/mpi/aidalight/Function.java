package mpi.aidalight;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.context.EntityGlobalContext;
import mpi.aidalight.context.MentionContext;
import mpi.aidalight.context.MentionGlobalContext;
import mpi.aidalight.entitycoherence.NED;
import mpi.aidalight.kbs.MentionEntityCooccurrence;
import mpi.util.Utils;


/**
 * This basically consists of functions which are used to get all possible mapping (mention-entity, entity-entity mappings)
 * 
 * @author datnb
 *
 */

public class Function {
  
  /**
   * This lists all possible mapping for a mention in a given local context, and a given domain.
   * 
   * localSimilarity should be a combination of context_similiarity, prior(mention, entity), 
   * prob(entity|surrounding_mentions), relatedness(mention, domains) and even sim(mention_string, entity_string).
   * 
   * @param mention
   * @param localContext
   * @param domains
   * @param settings
   * @return all possible mapping for a mention.
   */
  public static List<MentionEntityMapping> getMentionEntityMapping(Mention mention, 
      Set<String> domains, Settings settings) {
    MentionContext localContext = mention.getMentionContext();
    // probability(entity | given_mentions)
    TIntDoubleHashMap entity2prob = new TIntDoubleHashMap();
    
    // to normalize
    double sum = 0;
    
    MentionGlobalContext mentionGlobalContext = null;
    if(settings.getFilterByMentionTypeOption())
      mentionGlobalContext = new MentionGlobalContext(mention.getMention(), mention.getType(), 
          localContext.getSurroundingTokens(), settings.getOnTheFlyIDFTokenWeightOption());
    else 
      mentionGlobalContext = new MentionGlobalContext(mention.getMention(), null, 
          localContext.getSurroundingTokens(), settings.getOnTheFlyIDFTokenWeightOption());
    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
      double probabilityEntitySurroundingMentions 
        = getProbabilityEntitySurroundingMentions(entity.getId(), localContext.getSurroundingMentions());
      sum += probabilityEntitySurroundingMentions;
      entity2prob.put(entity.getId(), probabilityEntitySurroundingMentions);
    }
    
    List<MentionEntityMapping> res = new ArrayList<MentionEntityMapping>();
    TObjectDoubleHashMap<Entity> similarities = new TObjectDoubleHashMap<Entity>();
    double similaritySum = 0;
    
    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
      EntityGlobalContext entityGlobalContext = new EntityGlobalContext(entity.getId());
      
//      double contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens());
      double contextSimilarity;
      if(settings.getOnTheFlyIDFTokenWeightOption())
        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), 
            entityGlobalContext.getKeyPhraseTokens(), mentionGlobalContext.getTokenWeights());
      else
        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens());
      double prior; 
      double probabilityEntitySurroundingMentions;
      if(NED.fullSettings) {
        probabilityEntitySurroundingMentions = entity2prob.get(entity.getId()) / sum; // normalize
        prior = mentionGlobalContext.getPriors().get(entity.getId());
      }
      else {
        probabilityEntitySurroundingMentions = contextSimilarity;
        prior = contextSimilarity;
      }
      double domainRelatedness = getEntityDomainsRelatedness(entity, domains, settings.getEntityDomainDeep());
      double stringMatching = Utils.getStringMatchingSimilarity(mention.getMention(), entity.getName());
      
      double localSimilarity = settings.getContextSimilarityContribution() * contextSimilarity + 
          settings.getPriorContribution() * prior + 
          settings.getProbabilityEntityGivenMentionsContribution() * probabilityEntitySurroundingMentions + 
          settings.getDomainContribution() * domainRelatedness + 
          settings.getMatchingContribution() * stringMatching;
      
      
      similarities.put(entity, localSimilarity);
      similaritySum += localSimilarity;
//      // create a new mapping
//      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entity, localSimilarity);
//      res.add(mentionEntityMapping);
      
    }
    
    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
      // create a new mapping
      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entity, similarities.get(entity), similarities.get(entity)/similaritySum);
      res.add(mentionEntityMapping);
    }
    
    return res;
  }
  
  
  
  /**
   * This lists all possible mapping for a mention in a given local context, and a given domain.
   * 
   * localSimilarity should be a combination of context_similiarity, prior(mention, entity), 
   * prob(entity|surrounding_mentions), relatedness(mention, domains) and even sim(mention_string, entity_string).
   * 
   * @param mention
   * @param localContext
   * @param domains
   * @param settings
   * @param K - upper bound for number of mappings for a mention.
   * @return all possible mapping for a mention.
   */
  public static List<MentionEntityMapping> getMentionEntityMapping(Mention mention, 
      Set<String> domains, Settings settings, int K) {
    MentionContext localContext = mention.getMentionContext();
    // probability(entity | given_mentions)
    TIntDoubleHashMap entity2prob = new TIntDoubleHashMap();
    
    // to normalize
    double sum = 0;
    
    MentionGlobalContext mentionGlobalContext = null;
    if(settings.getFilterByMentionTypeOption())
      mentionGlobalContext = new MentionGlobalContext(mention.getMention(), mention.getType(), 
          localContext.getSurroundingTokens(), settings.getOnTheFlyIDFTokenWeightOption());
    else 
      mentionGlobalContext = new MentionGlobalContext(mention.getMention(), null, 
          localContext.getSurroundingTokens(), settings.getOnTheFlyIDFTokenWeightOption());
    
    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
      double probabilityEntitySurroundingMentions 
        = getProbabilityEntitySurroundingMentions(entity.getId(), localContext.getSurroundingMentions());
      sum += probabilityEntitySurroundingMentions;
      entity2prob.put(entity.getId(), probabilityEntitySurroundingMentions);
    }
    
    List<MentionEntityMapping> res = new ArrayList<MentionEntityMapping>();
    Entity[] entities = new Entity[K];
    double[] localSimilarities = new double[K];
    double[] priorThreshold = new double[K];
    for(int i = 0; i < K; i ++) {
      localSimilarities[i] = -1.0;
      priorThreshold[i] = -1.0;
    }
    
    TObjectDoubleHashMap<Entity> similarities = new TObjectDoubleHashMap<Entity>();
    double similaritySum = 0;
    
    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
      EntityGlobalContext entityGlobalContext = new EntityGlobalContext(entity.getId());
      
      double contextSimilarity;
      if(settings.getOnTheFlyIDFTokenWeightOption())
        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens(), mentionGlobalContext.getTokenWeights());
      else
        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens());
      double prior = mentionGlobalContext.getPriors().get(entity.getId());
      double probabilityEntitySurroundingMentions = entity2prob.get(entity.getId()) / sum; // normalize
      double domainRelatedness = getEntityDomainsRelatedness(entity, domains, settings.getEntityDomainDeep());
      double stringMatching = Utils.getStringMatchingSimilarity(mention.getMention(), entity.getName());
      
      double localSimilarity = settings.getContextSimilarityContribution() * contextSimilarity + 
          settings.getPriorContribution() * prior + 
          settings.getProbabilityEntityGivenMentionsContribution() * probabilityEntitySurroundingMentions + 
          settings.getDomainContribution() * domainRelatedness + 
          settings.getMatchingContribution() * stringMatching;
      
      
      for(int i = 0; i < K; i ++) {
        if((localSimilarities[i] < localSimilarity) || 
            (localSimilarities[i] == localSimilarity && priorThreshold[i] < prior)) {
          
          if(localSimilarities[K-1] == -1.0)
            similaritySum += localSimilarity;
          else
            similaritySum += localSimilarity - localSimilarities[K-1];
          
          for(int j = K-1; j > i; j--) {
            localSimilarities[j] = localSimilarities[j-1];
            entities[j] = entities[j-1];
            priorThreshold[j] = priorThreshold[j-1];
          }
          localSimilarities[i] = localSimilarity;
          entities[i] = entity;
          priorThreshold[i] = prior;
          
          similarities.put(entity, localSimilarity);
          break;
        }
      }
      
      
//      // create a new mapping
//      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entity, localSimilarity);
//      res.add(mentionEntityMapping);
      
    }
    
    for(int i = 0; i < K; i ++) {
      // create a new mapping
      if(entities[i] == null)
        break;
      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entities[i], similarities.get(entities[i]), 
          similarities.get(entities[i])/similaritySum);
      res.add(mentionEntityMapping);
    }
    
    return res;
  }
  
  
  
//  /**
//   * This lists all possible mapping for a mention in a given local context, and a given domain.
//   * 
//   * localSimilarity should be a combination of context_similiarity, prior(mention, entity), 
//   * prob(entity|surrounding_mentions), relatedness(mention, domains), sim(mention_string, entity_string)
//   * and the relatedness with disambiguated-entities.
//   * 
//   * @param mention
//   * @param localContext
//   * @param domains
//   * @param settings
//   * @param K - upper bound for number of mappings for a mention.
//   * @return all possible mapping for a mention.
//   */
//  public static List<MentionEntityMapping> getMentionEntityMapping(Mention mention, 
//      Set<String> domains, Map<Mention, Entity> disambiguatedMentionEntity, Settings settings, int K) {
//    
////    /* ----------- Compute the relatedness with disambiguated-entities ------ */
////    TIntIntHashMap relatedEntities = new TIntIntHashMap();
////    int N = 0; // number of disambiguated entities.
////    for(Mention disambiguatedMention: disambiguatedMentionEntity.keySet()) {
////      if(disambiguatedMention.getType().equalsIgnoreCase("LOCATION"))
////        continue;
////      N++;
////      Entity disambiguatedEntity = disambiguatedMentionEntity.get(disambiguatedMention);
////      for(int entityId: DataStore.getRelatedEntities(disambiguatedEntity.getId()).toArray()) {
////        if(relatedEntities.contains(entityId) == false)
////          relatedEntities.put(entityId, 1);
////        else
////          relatedEntities.put(entityId, relatedEntities.get(entityId) + 1);
////      }
////    }
////    /*------------- End of computing the relatedness with disambiguated entities ----- */
//    
//    MentionContext localContext = mention.getMentionContext();
//    // probability(entity | given_mentions)
//    TIntDoubleHashMap entity2prob = new TIntDoubleHashMap();
//    
//    // to normalize
//    double sum = 0;
//    
//    MentionGlobalContext mentionGlobalContext = new MentionGlobalContext(mention.getMention(), mention.getType(), 
//        localContext.getSurroundingTokens(), settings.getOnTheFlyIDFTokenWeightOption());
//    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
//      double probabilityEntitySurroundingMentions 
//        = getProbabilityEntitySurroundingMentions(entity.getId(), localContext.getSurroundingMentions());
//      sum += probabilityEntitySurroundingMentions;
//      entity2prob.put(entity.getId(), probabilityEntitySurroundingMentions);
//    }
//    
//    List<MentionEntityMapping> res = new ArrayList<MentionEntityMapping>();
//    Entity[] entities = new Entity[K];
//    double[] localSimilarities = new double[K];
//    double[] priorThreshold = new double[K];
//    for(int i = 0; i < K; i ++) {
//      localSimilarities[i] = -1.0;
//      priorThreshold[i] = -1.0;
//    }
//    
//    TObjectDoubleHashMap<Entity> similarities = new TObjectDoubleHashMap<Entity>();
//    double similaritySum = 0;
//    
//    for(Entity entity: mentionGlobalContext.getEntityCandidates()) {
//      EntityGlobalContext entityGlobalContext = new EntityGlobalContext(entity.getId());
//      
////      double contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens());
//      double contextSimilarity;
//      if(settings.getOnTheFlyIDFTokenWeightOption())
//        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens(), mentionGlobalContext.getTokenWeights());
//      else
//        contextSimilarity = Utils.getOverlapSimiarity(localContext.getSurroundingTokens(), entityGlobalContext.getKeyPhraseTokens());
//      double prior = mentionGlobalContext.getPriors().get(entity.getId());
//      double probabilityEntitySurroundingMentions = entity2prob.get(entity.getId()) / sum; // normalize
//      double domainRelatedness = getEntityDomainsRelatedness(entity, domains, settings.getEntityDomainDeep());
//      double stringMatching = Utils.getStringMatchingSimilarity(mention.getMention(), entity.getName());
//      
//      double localSimilarity = settings.getContextSimilarityContribution() * contextSimilarity + 
//          settings.getPriorContribution() * prior + 
//          settings.getProbabilityEntityGivenMentionsContribution() * probabilityEntitySurroundingMentions + 
//          settings.getDomainContribution() * domainRelatedness + 
//          settings.getMatchingContribution() * stringMatching;
//      
//      double relatedness = 0.0;
//      for(Entity disambiguatedEntity: disambiguatedMentionEntity.values()) {
//        relatedness += getEntityEntityRelatedness(disambiguatedEntity, entity, settings, mentionGlobalContext.getTokenWeights());
//      }
//      localSimilarity = Math.min(1.0, localSimilarity + relatedness);
//      
//      for(int i = 0; i < K; i ++) {
//        if((localSimilarities[i] < localSimilarity) || 
//            (localSimilarities[i] == localSimilarity && priorThreshold[i] < prior)) {
//          
//          if(localSimilarities[K-1] == -1.0)
//            similaritySum += localSimilarity;
//          else
//            similaritySum += localSimilarity - localSimilarities[K-1];
//          
//          for(int j = K-1; j > i; j--) {
//            localSimilarities[j] = localSimilarities[j-1];
//            entities[j] = entities[j-1];
//            priorThreshold[j] = priorThreshold[j-1];
//          }
//          localSimilarities[i] = localSimilarity;
//          entities[i] = entity;
//          priorThreshold[i] = prior;
//          
//          similarities.put(entity, localSimilarity);
//          break;
//        }
//      }
//      
//      
////      // create a new mapping
////      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entity, localSimilarity);
////      res.add(mentionEntityMapping);
//      
//    }
//    
//    for(int i = 0; i < K; i ++) {
//      // create a new mapping
//      if(entities[i] == null)
//        break;
//      MentionEntityMapping mentionEntityMapping = new MentionEntityMapping(mention, entities[i], similarities.get(entities[i]), 
//          similarities.get(entities[i])/similaritySum);
//      res.add(mentionEntityMapping);
//    }
//    
//    return res;
//  }
  
  
  
  /**
   * Assumption: mentions are independent.
   * @param entityId
   * @param surroundingMentions
   * @return the probability of an entity in the context of some given mentions.
   */
  public static double getProbabilityEntitySurroundingMentions(int entityId, List<String> surroundingMentions) {
    double probabilityEntitySurroundingMentions = 1.0;
    for(String givenMention: surroundingMentions) 
      probabilityEntitySurroundingMentions *= MentionEntityCooccurrence.getProbability(entityId, givenMention);
    return probabilityEntitySurroundingMentions;
  }
  
  
  
  /**
   * The simplest way to evaluate is just 1, 0, meaning if this entity
   * belongs to this domain or not.
   * 
   * However, in some case, we need something better. E.g. Victoria Beckham does not
   * belongs to "football" but still relate to "football".
   * 
   * @param entity
   * @param domain
   * @return the relatedness of an entity with a domain.
   */
  private static double getEntityDomainRelatedness(Entity entity, String domain, int deep) {
    TIntHashSet relatedEntities = new TIntHashSet();
    relatedEntities.add(entity.getId());
    for(int i = 0; i < deep; i ++) {
      // check in relatedEntities
      for(int currentEntity: relatedEntities.toArray()) {
        if(Utils.relateTo(currentEntity, domain))
          return (double) 1/(i+1);
      }
      
      // update
      TIntHashSet tmp = new TIntHashSet();
      for(int currentEntity: relatedEntities.toArray()) 
        tmp.addAll(DataStore.getRelatedEntities(currentEntity));
      
      relatedEntities = tmp;
    }
    return 0.0;
  }
  

  
  /**
   * locations can be in everywhere. Just return 0.5 for all locations.
   * @param entity
   * @param domains
   * @return the max relatedness of this entity with a domain in domains.
   */
  public static double getEntityDomainsRelatedness(Entity entity, Set<String> domains, int deep) {
    if(domains == null || domains.size() == 0)
      return 0.0;
//    if(DataStore.getType(entity.getId()).equalsIgnoreCase("LOCATION"))
//      return 0.5;
    double maxRelatedness = -1.0;
    for(String domain: domains) {
      double relatedness = getEntityDomainRelatedness(entity, domain, deep);
      if(maxRelatedness < relatedness)
        maxRelatedness = relatedness;
    }
    return maxRelatedness;
  }
  
  
  
  /**
   * The relatedness of two entities is a combination between context relatedness and type relatedness.
   * Weights are defined in settings. 
   * 
   * @param src
   * @param dst
   * @param settings
   * @return the relatedness between 2 entities.
   */
  public static double getEntityEntityRelatedness(Entity src, Entity dst, Settings settings, TIntDoubleHashMap tokenWeights) {
    EntityGlobalContext contextSrc = new EntityGlobalContext(src.getId());
    EntityGlobalContext contextDst = new EntityGlobalContext(dst.getId());
    return getEntityEntityRelatedness(contextSrc, contextDst, settings, tokenWeights);
  }
  
  
  /**
   * The relatedness is a combination between context relatedness and type relatedness.
   * Weights are defined in settings.
   * 
   * @param src
   * @param dst
   * @param settings
   * @return the relatedness between 2 entities which hold these contexts. 
   */
  public static double getEntityEntityRelatedness(EntityGlobalContext src, EntityGlobalContext dst, 
      Settings settings, TIntDoubleHashMap tokenWeights) {
    double contextRelatedness;
    if(settings.getOnTheFlyIDFTokenWeightOption() && tokenWeights != null)
      contextRelatedness = Utils.getOverlapSimiarity(src.getKeyPhraseTokens(), dst.getKeyPhraseTokens(), tokenWeights);
    else
      contextRelatedness = Utils.getOverlapSimiarity(src.getKeyPhraseTokens(), dst.getKeyPhraseTokens());
    
    if(NED.entityEntityRelatedness) {
      double typeRelatedness = 0.0;
      for(String type1: src.getWikiCategories())
        for(String type2: dst.getWikiCategories()) {
          double tmp = Utils.getTypeTypeRelatedness(type1, type2, settings);
          if(tmp > typeRelatedness)
            typeRelatedness = tmp;
          if(typeRelatedness == 1.0)
            break;
        }
      return settings.getEntityEntityContextRelatednessContribution() * contextRelatedness 
        + settings.getEntityEntityTypeRelatednessContribution() * typeRelatedness;
    }
    
    return (settings.getEntityEntityContextRelatednessContribution()  
        + settings.getEntityEntityTypeRelatednessContribution()) * contextRelatedness;
  }
  
  
  
  
}
