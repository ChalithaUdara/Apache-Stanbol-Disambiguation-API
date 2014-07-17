package mpi.aidalight;

/**
 * 
 * localSimilarity should be a combination of context_similiarity, prior(mention, entity), 
 * prob(entity|surrounding_mentions), relatedness(mention, domain) and even sim(mention_string, entity_string).
 * 
 * @author datnb
 *
 */

public class Settings {
  
  /*
   * local_similarity = ... + contextSimilarityContribution x context_similarity(mention, entity) + ...
   */
  public static double contextSimilarityContribution = 0.15;
  
  
  /*
   * local_similarity = ... + priorContribution x prior(mention, entity) + ...
   */
  public static double priorContribution = 0.25;
  
  /*
   * local_similarity = ... + domainContribution x relatedness(mention, domain) + ...
   */
  public static double domainContribution = 0.2;
  
  
  /*
   * local_similarity = ... + probabilityEntityGivenMentionsContribution x prob(entity|surround_mentions) + ...
   */
  public static double probabilityEntityGivenMentionsContribution = 0.15;
  
  
  /*
   * local_similarity = ... + matchingContribution x sim(mention_string, entity_string) + ...
   */
  public static double matchingContribution = 1 - contextSimilarityContribution - priorContribution
      - domainContribution - probabilityEntityGivenMentionsContribution;
  
  
  /*
   * The simplest way to evaluate is just 1, 0, meaning if this entity
   * belongs to this domain or not. This is at deep 1.
   * 
   * However, in some case, we need something better. E.g. Victoria Beckham does not
   * belongs to "football" but still relate to "football" because of David Beckham.
   * Victoria and David are connected directly. This is at deep 2.
   */
  private int entityDomainDeep = 1;
  
  
  
  /*
   * eitity_entity_relatedness = entityEntityContextRelatednessContribution x contextRelatedness
   * + entityEntityTypeRelatednessContribution x typeRelatedness
   */
  public static double entityEntityContextRelatednessContribution = 0.5;
  public static double entityEntityTypeRelatednessContribution = 1 - entityEntityContextRelatednessContribution;
  
  
  
  /*
   * Only extract features (noun-phrases) in a window surrounding a mention.
   * E.g. in 5 sentences before and after.
   */
  public static int windowSizeToExtractMentionContext = 1;
  
  
  
  private int windowSizeToExtractMentionsSurrounding = 1; // this sentence, one sentence before and one sentence after;
  
  
  
  /*
   * the technique is used to compute type-type relatedness between 2 entities.
   * At the moment, there are 2 options: hierarchy or cooccurrence 
   */
  private String typeTypeRelatedness = "hierarchy";
  
  
  
  /*
   * might expand relatedness of 2 entities by EXPAND_RELATEDNESS if one has a internal link to the other.
   */
  private double expandRelatedNessConstant = 2.0;
  
  
  
  /*
   * This is to decide if 2 mentions relate to each other!
   */
  private double entropyThreshold = 2.0;
  
  
  private boolean filterByMentionType = false;
  
  private static boolean testPrecision = true;
  
  private boolean onTheFlyIDFTokenWeight = false;
  
  
  public boolean getOnTheFlyIDFTokenWeightOption() {
    return onTheFlyIDFTokenWeight;
  }
  
  
  
  public boolean getFilterByMentionTypeOption() {
    return filterByMentionType;
  }
  
  
  public static boolean getTestPrecisionOption() {
    return testPrecision;
  }
  
  public Settings() {
    
  }
  
  
  public double getContextSimilarityContribution() {
    return contextSimilarityContribution;
  }
  
  public double getPriorContribution() {
    return priorContribution;
  }
  
  public double getProbabilityEntityGivenMentionsContribution() {
    return probabilityEntityGivenMentionsContribution;
  }
  
  public double getMatchingContribution() {
    return matchingContribution;
  }
  
  public double getDomainContribution() {
    return domainContribution;
  }
  
  /**
   * The simplest way to evaluate is just 1, 0, meaning if this entity
   * belongs to this domain or not. This is at deep 1.
   * 
   * However, in some case, we need something better. E.g. Victoria Beckham does not
   * belongs to "football" but still relate to "football" because of David Beckham.
   * Victoria and David are connected directly. This is at deep 2.
   * 
   * @return deep
   */
  public int getEntityDomainDeep() {
    return entityDomainDeep;
  }
  
  
  public double getEntityEntityContextRelatednessContribution() {
    return entityEntityContextRelatednessContribution;
  }
  
  public double getEntityEntityTypeRelatednessContribution() {
    return entityEntityTypeRelatednessContribution;
  }
  
  public int getWindowSizeToExtractMentionContext() {
    return windowSizeToExtractMentionContext;
  }
  
  public int getWindowSizeToExtractMentionsSurrounding() {
    return windowSizeToExtractMentionsSurrounding;
  }
  
  public double getExpandRelatednessConstant() {
    return expandRelatedNessConstant;
  }
  
  public String getTypeTypeRelatednessApproach() {
    return typeTypeRelatedness;
  }
  
  
  public double getEntropyThreshold() {
    return entropyThreshold;
  }
}



