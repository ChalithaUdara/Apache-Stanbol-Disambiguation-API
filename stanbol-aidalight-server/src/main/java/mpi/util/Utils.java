package mpi.util;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javatools.util.FileUtils;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.Settings;
import mpi.aidalight.context.EntityGlobalContext;
import mpi.aidalight.context.MentionExtractor;
import mpi.aidalight.context.NounPhraseExtractor;
import mpi.aidalight.entitycoherence.NED;
import mpi.aidalight.kbs.MentionEntityCooccurrence;
import mpi.lsh.intfeature.LSHTable;
import mpi.lsh.utils.Common;
import mpi.tokenizer.data.Tokenizer;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.typerelatedness.TypeTypeRelatedness;
import mpi.typerelatedness.TypeTypeRelatednessCooccurrence;
import mpi.typerelatedness.TypeTypeRelatednessHierarchy;
import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;


public class Utils {

  /*
   * Store all paths to data.
   */
  private static Properties nedProperties;
  
  private static String PROPERTIES_PATH = "ned.properties";
  
  private static TypeTypeRelatedness typeTypeRelatedness;
  
  /**
   * 
   * @param file
   * @return the text in "file" in form of line by line.
   * @throws IOException
   */
  public static List<String> getContent(String file) throws IOException {
    List<String> content = new ArrayList<String>();
    
    FileInputStream fis = new FileInputStream(file);
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);

    String line;
    while (true) {
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      content.add(line);
    }

    isr.close();
    fis.close();
    
    return content;
  }
  
  
  /**
   * Write "content" to "file".
   * @param file
   * @param content
   * @throws IOException
   */
  public static void writeContent(String file, String content) throws IOException {
    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(file);
    writer.write(content);
    writer.flush();
    writer.close();
  }
  
  
  public static String getProperty(String key) {
    try {
      if(nedProperties == null){
        nedProperties = ClassPathLoader.getPropertiesFromClasspath(PROPERTIES_PATH);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return nedProperties.getProperty(key);
  }
  
  
  
  /**
   * Calculate the overlap similarity between 2 sorted arrays.
   * 
   * @param set1
   * @param set2
   * @return overlap similarity between set1, set2 
   */
  public static double getOverlapSimiarity(int[] set1, int[] set2) {
	  if(set1 == null || set2 == null)
	    return 0.0;
	  int counter = 0;
    int i = 0, j = 0;
    while(i < set1.length) {
      while(j < set2.length && set2[j] < set1[i])
        j++;
      if(j == set2.length)
        break;
      if(set2[j] == set1[i])
        if(DataStore.isStopword(set2[j]) == false)
          counter++;
      i++;
    }
    return (double)counter / (Math.min(set1.length, set2.length));
  }
  
  
  /**
   * Calculate the overlap similarity between 2 sorted arrays.
   * 
   * @param set1
   * @param set2
   * @param tokenWeights: Idf weights
   * @return overlap similarity between set1, set2 
   */
  public static double getOverlapSimiarity(int[] set1, int[] set2, TIntDoubleHashMap tokenWeights) {
    if(set1 == null || set2 == null)
      return 0.0;
//    int counter = 0;
    int i = 0, j = 0;
    double d = 0, d1 = 0, d2 = 0;
    while(i < set1.length) {
      while(j < set2.length && set2[j] < set1[i]) {
        d2 += tokenWeights.get(set2[j]);
        j++;
      }
        
      if(j < set2.length && set2[j] == set1[i])
        if(DataStore.isStopword(set2[j]) == false) {
          d += tokenWeights.get(set2[j]);
//          counter++;
        }
      d1 += tokenWeights.get(set1[i]);
      i++;
    }
    return d / (Math.min(d1, d2));
  }
  
  
  /**
   * Calculate the overlap similarity between 2 sorted arrays.
   * 
   * @param set1
   * @param set2
   * @return overlap similarity between set1, set2 
   */
  public static double getOverlapSimiarity(long[] set1, long[] set2) {
    if(set1 == null || set2 == null)
      return 0.0;
    int counter = 0;
    int i = 0, j = 0;
    while(i < set1.length) {
      while(j < set2.length && set2[j] < set1[i])
        j++;
      if(j == set2.length)
        break;
      if(set2[j] == set1[i])
        counter++;
      i++;
    }
    return (double)counter / (Math.min(set1.length, set2.length));
  }
  
  
  /**
   * Calculate the overlap similarity between 2 sorted arrays.
   * 
   * @param set1
   * @param set2
   * @param tokenWeights: Idf weights
   * @return overlap similarity between set1, set2 
   */
  public static double getOverlapSimiarity(long[] set1, long[] set2, TLongDoubleHashMap tokenWeights) {
    if(set1 == null || set2 == null)
      return 0.0;
//    int counter = 0;
    int i = 0, j = 0;
    double d = 0, d1 = 0, d2 = 0;
    while(i < set1.length) {
      while(j < set2.length && set2[j] < set1[i]) {
        d2 += tokenWeights.get(set2[j]);
        j++;
      }
        
      if(j < set2.length && set2[j] == set1[i])
        d += tokenWeights.get(set2[j]);
//          counter++;
      d1 += tokenWeights.get(set1[i]);
      i++;
    }
    return d / (Math.min(d1, d2));
  }
  
  
  /**
   * 
   * @param mention
   * @param entity
   * @return Jaccard similarity between mention and entity at 3-grams 
   */
  public static double getStringMatchingSimilarity(String mention, String entity) {
    return LSHTable.getExactJaccard(Common.getCounter(mention), Common.getCounter(entity));
  }
  
  
  /**
   * 
   * @param entity
   * @param domain
   * @return true if the entity relates to domain.
   */
  public static boolean relateTo(int entityId, String domain) {
    return DataStore.getDomainEntities(domain).contains(entityId);
  }
  
  
  
  /**
   * 
   * @param type1
   * @param type2
   * @return the relatedness between 2 types.
   */
  public static double getTypeTypeRelatedness(String type1, String type2, Settings settings) {
    if(NED.testKWonly || NED.fullSettings == false)
      return 0.0;
    
    if(typeTypeRelatedness == null) {
      if(settings.getTypeTypeRelatednessApproach().equalsIgnoreCase("cooccurrence"))
        typeTypeRelatedness = new TypeTypeRelatednessCooccurrence(false, true); // boolean useNearestTypeForMissingType, boolean useSmartStatistics
      else
        typeTypeRelatedness = new TypeTypeRelatednessHierarchy();
      
    }
      
    return typeTypeRelatedness.getRelatedness(type1, type2);
  }
  
  
  /**
   * This is the first process of an input text. 
   * E.g. extract mentions, build the context, etc.
   * @param file
   * @return 
   * @throws Exception
   */
  public static List<Mention> getMentions(String text, Settings settings) throws Exception{
//    String text = "";
//    for(String str: Utils.getContent(file)){
//      text += str + "\n";
//    }
    String file = "./data/tmp_doc";
    Utils.writeContent(file, text);
    
    Map<Sentence, TIntHashSet> sentence2Nps = new HashMap<Sentence, TIntHashSet>();
    NounPhraseExtractor extractor = new NounPhraseExtractor();
    SentenceSplitter ss = new SentenceSplitter(file);
    Sentence[] sentences = ss.splitAll();
    for(Sentence sentence: sentences){
      List<String> nps = extractor.getNounPhrases(sentence.toString());
      Set<String> tokens = new HashSet<String>();
      for(String np: nps){
        String toks[] = np.split(" ");
        for(String tok: toks)
          if(tokens.contains(tok) == false)
            tokens.add(tok);
      }
      TObjectIntHashMap<String> token2Id = DataStore.getIdsForWords(tokens);
      TIntHashSet ids = new TIntHashSet();
      
      for(int id: token2Id.values())
        if(DataStore.isStopword(id) == false)
          ids.add(id);
      
      sentence2Nps.put(sentence, ids);
    }
    Tokens tokens = TokenizerManager.parse(file, text, Tokenizer.type.ner, false);
    return new MentionExtractor(sentences, sentence2Nps, settings.getWindowSizeToExtractMentionContext()).filter(tokens);
  }
  
  
  
  /**
   * 
   * @param mention1
   * @param mention2
   * @return Jaccard similarity between 2 strings at token level.
   */
  public static double getJaccardSimilarityAtTokenLevel(String mention1, String mention2) {
    Set<String> set = new HashSet<String>();
    for(String s: mention1.toLowerCase().split(" "))
      set.add(s);
    
    int counter = 0;
    Set<String> toks = new HashSet<String>();
    for(String s: mention2.toLowerCase().split(" "))
      toks.add(s);
    
    for(String s: toks)
      if(set.contains(s))
        counter++;
    
    return (double)counter / (set.size() + toks.size() - counter);
  }
  
  
  
  /**
   * extract the context of a mention to predict type. E.g. Germany wins England: Germany can not be a location.
   * 
   * @param mentions
   * @param file
   */
  public static void preparePosContextForMentions(List<Mention> mentions, String file, int windowSize) throws IOException{
    String content = "";
    for(String line: getContent(file))
      content += line + "\n";
    
    Tokens tokens = TokenizerManager.parse("tmpFile", content, Tokenizer.type.ner, true);
    int vt = 0;
    for(int i = 0; i < mentions.size(); i ++) {
      Mention mention = mentions.get(i);
      String context = "", previousContext = "", followingContext = "";
      while(vt < tokens.size() && tokens.getToken(vt).getBeginIndex() != mention.getCharOffset())
        vt ++;
      
      for(int j = windowSize; j > 0; j--) {
        if(vt - j > 0) {
          String pos = tokens.getToken(vt-j).getPOS();
          if(pos.startsWith("V") || pos.equalsIgnoreCase("IN") || pos.equalsIgnoreCase("POS")) {
            context += tokens.getToken(vt-j).getLemma() + "$";
            previousContext += tokens.getToken(vt-j).getLemma() + "$";
          }
        }
      }
      
      context += "Mention$";
      previousContext += "Mention$";
      followingContext += "Mention$";
      
      for(int j = 1; j <= windowSize; j++) {
        if(vt + j < tokens.size()) {
          String pos = tokens.getToken(vt + j).getPOS();
          if(pos.startsWith("V") || pos.equalsIgnoreCase("IN") || pos.equalsIgnoreCase("POS")) {
            context += tokens.getToken(vt + j).getLemma() + "$";
            followingContext += tokens.getToken(vt + j).getLemma() + "$";
          }
        }
      }
      
      if(context.endsWith("$"))
        context = context.substring(0, context.length()-1);
      
      if(previousContext.endsWith("$"))
        previousContext = previousContext.substring(0, previousContext.length()-1);
      
      if(followingContext.endsWith("$"))
        followingContext = followingContext.substring(0, followingContext.length()-1);
      
      mention.setContext(context, previousContext, followingContext);
    }
    
  }
  
  
  // -------------------- Similarity functions------------------------------------
  
  /**
   * 
   * @param mention
   * @param entity
   * @return 
   */
  public static double getStringMatchingSimilarity(Mention mention, Entity entity) {
    return LSHTable.getExactJaccard(Common.getCounter(mention.getMention()), Common.getCounter(entity.getName()));
  }
  
  
  
  /**
   * This is from AIDA. Basically, it is based on counting number of a mention mapped to an entity.
   * @param mention
   * @param entity
   * @return
   */
  public static double getMentionEntityPrior(Mention mention, Entity entity) {
    return DataStore.getEntityPriors(mention.getMention()).get(entity.getId());
  }
  
  
  /**
   * Assumption: mentions are independent.
   * @param entityId
   * @param surroundingMentions
   * @return the probability of an entity in the context of some given mentions.
   */
  public static double getEntitySurroundingMentionsCooccurrence(Entity entity, List<String> surroundingMentions) {
    double probabilityEntitySurroundingMentions = 1.0;
    for(String givenMention: surroundingMentions) 
      probabilityEntitySurroundingMentions *= MentionEntityCooccurrence.getProbability(entity.getId(), givenMention);
    return probabilityEntitySurroundingMentions;
  }
  
  
//  /**
//   * 
//   * @param mention
//   * @param entity
//   * @return
//   */
//  public static double getMentionEntityLocalContextSimilarity(Mention mention, Entity entity) {
//    return getOverlapSimiarity(mention.getMentionContext().getSurroundingTokens(), 
//        new EntityGlobalContext(entity.getId()).getKeyPhraseTokens());
//  }
  
  
//  /**
//   * 
//   * @param entity1
//   * @param entity2
//   * @return
//   */
//  public static double getEntityEntityContextRelatedness(Entity entity1, Entity entity2) {
//    return getOverlapSimiarity(new EntityGlobalContext(entity1.getId()).getKeyPhraseTokens(), 
//        new EntityGlobalContext(entity2.getId()).getKeyPhraseTokens());
//  }
//  
//  
//  public static double getEntityEntityContextRelatedness(Entity entity1, Entity entity2, TIntDoubleHashMap tokenWeights) {
//    
//  }
  
  
  /**
   * 
   * @param entity1
   * @param entity2
   * @return
   */
  public static double getEntityEntityTypeRelatedness(Entity entity1, Entity entity2) {
    double typeRelatedness = 0.0;
    for(String type1: new EntityGlobalContext(entity1.getId()).getWikiCategories())
      for(String type2: new EntityGlobalContext(entity2.getId()).getWikiCategories()) {
        double tmp = Utils.getTypeTypeRelatedness(type1, type2, new Settings());
        if(tmp > typeRelatedness)
          typeRelatedness = tmp;
        if(typeRelatedness == 1.0)
          break;
      }
    return typeRelatedness;
  }
  
  
  public static double getEntityEntityTypeRelatedness(int entity1, int entity2) {
    double typeRelatedness = 0.0;
    for(String type1: new EntityGlobalContext(entity1).getWikiCategories())
      for(String type2: new EntityGlobalContext(entity2).getWikiCategories()) {
        double tmp = Utils.getTypeTypeRelatedness(type1, type2, new Settings());
        if(tmp > typeRelatedness)
          typeRelatedness = tmp;
        if(typeRelatedness == 1.0)
          break;
      }
    return typeRelatedness;
  }
  
  
//  /**
//   * Build a graph based on internal-links.
//   * 
//   * @param entity1
//   * @param entity2
//   * @return
//   */
//  public static double getEntityEntityGraphRelatedness(Entity entity1, Entity entity2) {
//    
//  }
  
  
  
  public static void main(String args[]) throws Exception {
    TIntDoubleHashMap map = DataStore.getEntityPriors("Clinton");
    for(int key: map.keys())
      System.out.println(DataStore.getEntityForId(key) + "\t" + map.get(key));
  }
  
  
}
