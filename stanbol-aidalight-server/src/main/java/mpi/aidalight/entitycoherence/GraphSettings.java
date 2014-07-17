package mpi.aidalight.entitycoherence;


public class GraphSettings {
  /*
   * use StandordNED to annotate mentions if true.
   */
  private boolean useStanfordNER = true;
  
  
  public boolean isUsingStanfordNER() {
    return useStanfordNER;
  }
  
  /*
   * the corpus used to run experiment. It could be CoNLL, Kore or Wiki.
   */
  private String corpus;
  
  public String getCorpusName() {
    return corpus;
  }
  
  public void setCorpusName(String corpus) {
    this.corpus = corpus;
  }
  
  
  /*
   * true means remove entity candidates which doesn't hold the same type with the mention.
   */
  private boolean extractCandidatesByType = true;
  
  public boolean isExtractingEntityCandidateByType() {
    return extractCandidatesByType;
  }
  
  
  /*
   * Because a mention, especially with Person names, can have too many entity candidates, which
   * might slow down the system. We may keep at most K candidates for each mention. 
   * 
   * Candidates might be ranked by local_similarities.
   */
  private int numberCandidatesPerMention = 5; // limitation of number of candidates in graph-round
  
  public int getNumberCandidatesPerMention() {
    return numberCandidatesPerMention;
  }
  
  
  
//  /*
//   * if flexibleEntityCandidateSet, we might allow the set of entity candidate bigger in some cases. 
//   * 
//   * E.g. we are extracting K best candidate for a mention, but candidate_K, candidate_K+1, etc. hold the same
//   * local similarity value. In this case, candidate_K+1 is also added.
//   */
//  private boolean flexibleEntityCandidateSet = true;
  
  
  /*
   * MAX is the threshold to decide if a document is long or not.
   */
  private int MAX = 3000;
  
  public boolean isALongText(int textLength) {
    return textLength > MAX;
  }
  
  
  
  private int maxThreads = 8; // number of threads which can run in parallel.
  
  public int getMaxPartitionsRunInParallel() {
    return maxThreads;
  }
  
  
  /*
   * support multi-threading or not?
   */
  private boolean threadProgramming = false;
  
  public boolean isSupportingMultiThread() {
    return threadProgramming;
  }
  
  
  /*
   * if true, the relatedness between mentions (entities) is decreased by the distance between them.
   * The distance might be computed by character distance, token distance, sentence distance, etc.
   */
  public boolean usePositionalDistance = true;
  
  public boolean isDecreasingRelatednessByDistance() {
    return usePositionalDistance;
  }
  
  
  /*
   * For each mention, consider a window of 1000 characters surrounding.
   */
  public int windowLengthAtCharacterLevel = 1000;
  
  public int getWindowLenghthAtCharacterLevel() {
    return windowLengthAtCharacterLevel;
  }
  
  
  /*
   * To predict the domains from a set of entities. Only predict if number of entities is at least thresholdNumEntitiesPredictingDomain
   */
  private int thresholdNumEntitiesPredictingDomain = 2; 
  
  
  public int getThresholdNumEntitiesPredictingDomain() {
    return thresholdNumEntitiesPredictingDomain;
  }
  
  
  
  /*
   * disambiguate some mentions with low entropies first.
   */
  private boolean preprocessSomeMentionsRankedByEntropies = true;
  
  
  public boolean isPreporcessingMentionsRankedByEntropies() {
    return preprocessSomeMentionsRankedByEntropies;
  }
  
  
  /*
   * percentage of mentions to process in the first round.
   */
  private double percentageOfMentionsProcessedInFirstRound = 0.1; // 10%
  
  
  public double getPercentageOfMentionsProcessedInFirstRound() {
    return percentageOfMentionsProcessedInFirstRound;
  }
  
  
  
  private double localSimilarityThreshold = 0.02;
  
  /**
   * threshold to accept a mention-entity mapping.
   */
  public double getLocalSimilarityThreshold() {
    return localSimilarityThreshold;
  }
  
  
  
  private double nedLightLocalSimilarityThreshold = 0.05;
  
  /**
   * threshold to accept a mention-entity mapping in the first round.
   * 
   * it should be higher than localSimilarityThreshold to accept only safe (highly confident) mappings.
   */
  public double getnedLightLocalSimilarityThreshold() {
    return nedLightLocalSimilarityThreshold;
  }
  
  
  
  /**
   * Locations are not really ambiguous. In almost cases, the locality similarity + the string itself are enough
   * to get the right entity.
   * 
   * However, the relatedness between locations and others is noise in many circumstances. If true, we disambiguate locations
   * in a preprocessing step, and thus, it doesn't contribute in the graph round.
   */
  private boolean preprocessLocationsByLocalsimilarity = false;
  
  
  public boolean isPreprocessingLocationByLocalsimilarity() {
    return preprocessLocationsByLocalsimilarity;
  }
  
}
