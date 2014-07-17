package mpi.aidalight.entitycoherence;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.Function;
import mpi.aidalight.MentionEntityMapping;
import mpi.aidalight.Settings;
import mpi.aidalight.exp.ExpUtils;
import mpi.lingsources.WikiWordnetMapper;
import mpi.util.Utils;


public class NED {
  /*
   * settings for the system related to mention-entity mappings and entity-entity mappings
   */
  private Settings settings;
  
  
  /*
   * settings related to graph
   */
  private GraphSettings graphSettings;
  
  
  private List<String> domains;
  
  private boolean testPreprocessRunningTime = true;
  public static long preprocessTime = 0;
  
  public static boolean fullSettings = true;
  public static boolean testOnEasyMentions = false;
  public static boolean testKWonly = false;
  public static boolean updateLocalSimByChosenEntities = true;
  public static boolean decreaseRelatednessbyMentionDistance = true;
  public static boolean entityEntityRelatedness = true;
  public static boolean expandRelatedNessConstantbyGraph = true;
  
  public static boolean partitionLongText = false;
  
  public static boolean partlyMatchingMentionEntitySearch = true;
  public static boolean lshMeansTableRMI = true;
//  public static String lshServer = "titan";
  
  
  public NED(Settings settings, GraphSettings graphSettings) {
    this.settings = settings;
    this.graphSettings = graphSettings;
    
    try {
      domains = Utils.getContent(Utils.getProperty("domainList"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    //DataStore.init(); // load data into MM
  }
  
  
  public synchronized Map<Mention, Entity> disambiguateText(String text, String host) throws Exception {
//    new File("/tmp/datnb").mkdir();
    String tmpDoc = "./data/tmp/tmpDoc_" + host;
    Utils.writeContent(tmpDoc, text);
    Map<Mention, Entity> results = disambiguate(text, null);
    return results;
  }
  
  /**
   * 
   * @param file: the path to input file.
   * @return Map of mention-bestEntity.
   * @throws Exception
   */
  public Map<Mention, Entity> disambiguate(String text, List<Mention> mentions) throws Exception {
    long startTime = Calendar.getInstance().getTimeInMillis();
//    List<Mention> mentions = null;
    Map<Mention, Entity> results = null;
//    for(Mention mention: mentions) {
//      if(DataStore.isLocationMention(mention.getMention()))
//        mention.setType("LOCATION");
//    }
    
//    if(graphSettings.isUsingStanfordNER())
//      mentions = Utils.getMentions(file, settings);
//    else {
//      if(graphSettings.getCorpusName().equalsIgnoreCase("Wiki"))
//        mentions = ExpUtils.getMentionsFromWikiPages(file);
//      else
//        mentions = ExpUtils.getMentions(file, (GraphSettingsExperiment)graphSettings);
//    }
    
    if(mentions == null)
      mentions = Utils.getMentions(text, settings);
    else
      mentions = ExpUtils.getMentions(text, (GraphSettingsExperiment)graphSettings, mentions);
    
//    // check type
//    Utils.preparePosContextForMentions(mentions, file, 1);
//    for(Mention mention: mentions) {
//      Set<String> possibleTypes = DataStore.getPossibleTypes(mention.getContexts());
//      if(possibleTypes == null)
//        continue;
//      if(possibleTypes.contains(mention.getType()) == false)
//        mention.setType("UNCLEAR");
//    }
    
    // update surrounding mentions
    for(Mention mention: mentions) {
      List<String> surroundingMentions = new ArrayList<String>();
      for(Mention m: mentions) {
        if(Math.abs(mention.getSentenceId() - m.getSentenceId()) < settings.getWindowSizeToExtractMentionsSurrounding())
          surroundingMentions.add(m.getMention());
      }
      mention.getMentionContext().setSurroundingMentions(surroundingMentions);
    }
    
    long duration = Calendar.getInstance().getTimeInMillis() - startTime;
    if(testPreprocessRunningTime) {
      preprocessTime += duration;
    }
    
    Map<Mention, Mention> cluster = new MentionCombiner(mentions).deduplicate();
    // update context
    for(Mention mention: mentions) {
      mention.setMentionContext(cluster.get(mention).getMentionContext());
    }
    
    // check if this is a long text or not.
//    List<String> lines = Utils.getContent(file);
//    int textLength = 0;
//    for(String line: lines)
//      textLength += line.length();
    int textLength = text.length();
    
    if(graphSettings.isALongText(textLength) && partitionLongText) {
      // large document
      // partition by paragraph
      String[] lines = text.split("\n");
      List<Integer> pos = new TextPartitioner(lines).partition();
      
      System.out.println("Partition the document into " + pos.size() + " paragraphs.");
      Mention arr[] = new Mention[mentions.size()];
      for(int i = 0; i < mentions.size(); i++)
        arr[i] = mentions.get(i);
      Arrays.sort(arr);
      List<List<Mention>> mentionsByParagraph = new ArrayList<List<Mention>>();
      int i = 0, j = 0;
      while(i < pos.size()) {
        List<Mention> tmp = new ArrayList<Mention>();
        while(j < arr.length && arr[j].getCharOffset() < pos.get(i)) {
          tmp.add(arr[j++]);
        }
        mentionsByParagraph.add(tmp);
        i++;
      }
      
      List<Map<Mention, Entity>> intermediateResults = new ArrayList<Map<Mention,Entity>>();
      BlockingQueue<List<Mention>> queue = new LinkedBlockingQueue<List<Mention>>();
      
      if(graphSettings.isSupportingMultiThread()) {
        List<Thread> threads = new ArrayList<Thread>();
        for(List<Mention> p: mentionsByParagraph) 
          queue.add(p);
        
        for(int id = 0; id < graphSettings.getMaxPartitionsRunInParallel(); id++) 
          threads.add(new PThread(intermediateResults, queue));
        
        
        for(Thread thread: threads) 
          thread.start();
      
        try {
          for(Thread thread: threads) 
            thread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      else {
        for(List<Mention> p: mentionsByParagraph) {
          intermediateResults.add(process(p));
        }
      }
      
      results = new HashMap<Mention, Entity>();
      for(Map<Mention, Entity> res: intermediateResults) {
        for(Mention mention: res.keySet()) {
          results.put(mention, res.get(mention));
        }
      }
    }
    else {
      // this is a short document
      // process the whole document at the same time.
      System.out.println("Process the entire document at the same time.");
      results = process(mentions);
    }
    
    Map<Mention, Entity> finalResults = new HashMap<Mention, Entity>();
    
    Map<Mention, List<Mention>> invertedMap = new HashMap<Mention, List<Mention>>();
    for(Mention mention: cluster.keySet()) {
      Mention val = cluster.get(mention);
      List<Mention> tmp = invertedMap.get(val);
      if(tmp == null) {
        tmp = new ArrayList<Mention>();
        tmp.add(mention);
        invertedMap.put(val, tmp);
      }
      else
        tmp.add(mention);
    }
    
    for(Mention mention: cluster.values()) {
      List<Mention> tmp = invertedMap.get(mention);
      double confidence = -2.0;
      Entity bestCandidate = null;
      for(Mention m: tmp) {
        Entity entity = results.get(m);
        if(testOnEasyMentions && entity == null)
          continue;
//        if(entity.getLocalSimilarity() > confidence) {
          bestCandidate = entity;
          confidence = entity.getLocalSimilarity();
//        }
      }
      
      for(Mention m: tmp) {
//        if(Settings.getTestPrecisionOption() == true) {
//          if(bestCandidate.getName().equalsIgnoreCase(Entity.NO_MATCHING_ENTITY) == false)
//            finalResults.put(m, bestCandidate);
//        }
//        else
        if(testOnEasyMentions && bestCandidate == null)
          continue;
        
        finalResults.put(m, bestCandidate);
      }
    }
    
    return finalResults;
  }
  
  
  
  private Map<Mention, Entity> process(List<Mention> mentions) throws Exception {
    Map<Mention, Mention> cluster = new MentionCombiner(mentions).deduplicate();
    mentions = new ArrayList<Mention>(new HashSet<Mention>(cluster.values()));
    
    Map<Mention, List<MentionEntityMapping>> mentionMappings = new HashMap<Mention, List<MentionEntityMapping>>();
    for(Mention mention: mentions) {
      List<MentionEntityMapping> mappings = Function.getMentionEntityMapping(mention, null , settings);
      for(MentionEntityMapping mapping: mappings) {
        mapping.getEntity().setMention(mention);
      }
      mentionMappings.put(mention, mappings);
    }
    
    
    Map<Mention, Entity> disambiguatedMentionEntity = null;
    
    if(testKWonly) {
      disambiguatedMentionEntity = new HashMap<Mention, Entity>();
    }
    else {
      NEDLight nedlight = new NEDLight(mentionMappings, settings, graphSettings);
      disambiguatedMentionEntity = nedlight.disambiguate();
      
      if(testOnEasyMentions) {
        Map<Mention, Entity> finalResults = new HashMap<Mention, Entity>();
        for(Mention mention: cluster.keySet()) {
          if(disambiguatedMentionEntity.containsKey(cluster.get(mention))) 
            finalResults.put(mention, disambiguatedMentionEntity.get(cluster.get(mention)));
        }
        
        return finalResults;
      }
      
      // decide if should preprocess locations or not
      if(graphSettings.isPreprocessingLocationByLocalsimilarity()) {
        Map<Mention, Entity> locationMentionEntity = new LocationDisambiguator(mentionMappings, disambiguatedMentionEntity).disambiguate();
        for(Mention mention: locationMentionEntity.keySet()) {
          disambiguatedMentionEntity.put(mention, locationMentionEntity.get(mention));
        }
      }
    }
    
    TObjectIntHashMap<String> domainCounter = new TObjectIntHashMap<String>();
    int numberOfLocationEntities = 0;
    
    if(fullSettings) {
      for(Entity entity: disambiguatedMentionEntity.values()) {
        if(graphSettings.isPreprocessingLocationByLocalsimilarity() && entity.getMention().getType().equalsIgnoreCase("LOCATION")){
          numberOfLocationEntities++;
          continue; // locations are in all domains (noise)
        }
        
        Set<String> types = DataStore.getWikiTypes(entity.getId());
        Set<String> countedDomains = new HashSet<String>();
        
        for(String wikiType: types) {
          for(String superDomain: domains) {
            if(WikiWordnetMapper.wikiCategoryIsSubOfDomain(wikiType, superDomain)){
              if(countedDomains.contains(superDomain) == false) {
                countedDomains.add(superDomain);
                if(domainCounter.contains(superDomain))
                  domainCounter.put(superDomain, domainCounter.get(superDomain) + 1);
                else
                  domainCounter.put(superDomain, 1);
              }
            }
          }
          
          
        }
      }
    }
    
    int numberOfEntity = disambiguatedMentionEntity.keySet().size() - numberOfLocationEntities;
    Set<String> relatedDomains = new HashSet<String>();
    
    for(String domain: domainCounter.keySet()) {
      if(domainCounter.get(domain) > Math.max(graphSettings.getThresholdNumEntitiesPredictingDomain(), numberOfEntity / 2))
        relatedDomains.add(domain);
    }
    
    
    if(relatedDomains.size() > 0) {
      System.out.print("Related to :");
      for(String domain: relatedDomains)
        System.out.print("\t" + domain);
      System.out.println();
    }
    else {
//      System.out.println("No domain returned!");
    }
    
    // update local similarity
    for(Mention mention: mentionMappings.keySet()) {
      for(MentionEntityMapping mapping: mentionMappings.get(mention)) {
        double localSimilarity = mapping.getLocalSimilarity();
        localSimilarity += settings.getDomainContribution() * Function.getEntityDomainsRelatedness(mapping.getEntity(), relatedDomains, settings.getEntityDomainDeep());
        mapping.setLocalSimilarity(localSimilarity);
      }
    }
    
    Map<Mention, Entity> results = new BottomUpGreedyGraph(mentionMappings, disambiguatedMentionEntity, settings, graphSettings, true).disambiguate();
    
    
    Map<Mention, Entity> finalResults = new HashMap<Mention, Entity>();
    for(Mention mention: cluster.keySet()) {
      if(results.containsKey(cluster.get(mention))) {
        finalResults.put(mention, results.get(cluster.get(mention)));
      }
      else {
        finalResults.put(mention, new Entity(Entity.NO_MATCHING_ENTITY, -1));
      }
    }
    
    return finalResults;
  }
  
  
  
  class PThread extends Thread {
    private List<Map<Mention, Entity>> intermediateResults;
    private BlockingQueue<List<Mention>> queue;
    
    public PThread(List<Map<Mention, Entity>> intermediateResults, BlockingQueue<List<Mention>> queue) {
      this.intermediateResults = intermediateResults;
      this.queue = queue;
    }
    
    @Override
    public void run() {
      try {
        while(true){
          List<Mention> mentions = queue.poll();
          if(mentions == null)
            break;
          Map<Mention, Entity> mentionEntity = process(mentions);
          synchronized (intermediateResults) {
            intermediateResults.add(mentionEntity);
          }
          
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
