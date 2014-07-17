package mpi.aidalight;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aidalight.entitycoherence.NED;
import mpi.aidalight.kbs.EntityEntityCooccurrence;
import mpi.aidalight.kbs.MentionEntityCooccurrence;
import mpi.lingsources.WikiWordnetMapper;
import mpi.lsh.intfeature.LSHTable;
import mpi.lsh.rmi.LSHServer;
import mpi.lsh.rmi.LSHServerImpl;
import mpi.lsh.utils.Common;
import mpi.tokenizer.data.Token;
import mpi.util.Standardize;
import mpi.util.Utils;
import basics.Normalize;


/**
 * This is where all data is loaded and stored in Main Memory.
 * 
 * @author datnb
 *
 */

public class DataStore {
  private static TObjectIntHashMap<String> token2Id = null;
  
  private static TIntIntHashMap tokenCounter = null;
  
  private static TIntHashSet stopwordIDs = null;
  
  private static LSHTable categoryTable = null;
  
  private static LSHTable meansTable = null;
  
  private static TIntObjectHashMap<Set<String>> entity2Types = null;
  
  private static Map<String, TIntDoubleHashMap> priors = null;
  
  private static TIntObjectHashMap<int[]> entityTokens = null;
  
  private static Set<String> nations = null;
  
  private static TIntObjectHashMap<Set<String>> entity2WikiTypes = null;
  
  private static Map<String, TIntHashSet> mention2entities = null;
  
  private static TIntObjectHashMap<String> id2entity = null;
  
  private static TObjectIntHashMap<String> entity2id = null;
  
  public static int NUMBER_ENTITIES = 2638982;
  
  private static TIntObjectHashMap<TIntHashSet> relatedEntities = null;
  
  private static Map<String, TIntHashSet> type2relatedEntities = null;
  
  private static Map<String, TIntHashSet> domainEntities = null;
  
//  private static String DOMAIN_ROOT = "..//data/bned/resources/domain_data/";
  
  
//  public static boolean useWeightedToken = false;

  private static Set<String> locationMentions = null;
  
  public static boolean isLocationMention(String mention) {
    if(locationMentions == null) {
      try {
        locationMentions = new HashSet<String>();
        for(String line: Utils.getContent(Utils.getProperty("mentionEntities"))) {
          String str[] = line.split("\t");
          locationMentions.add(str[0]);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    mention = Standardize.conflateMention(mention);
    String normMention = 
        Standardize.getPostgresEscapedString(Normalize.string(mention));
    return locationMentions.contains(normMention);
  }
  
  
  /**
   * Load all data into main memory.
   */
  public static void init() {
    try {
      loadEntityId();
      loadIdEntity();
//      loadNations();
      loadRelatedEntities();
//      loadTypeRelatedEntities();
      loadDomainEntities();
      
      loadTokenCounter();
      
      EntityEntityCooccurrence.initEntityEntityCooccurrenceCounter();
      MentionEntityCooccurrence.initMentionEntityCooccurrenceCounter();
//      TypeTypeRelatednessCooccurrence.init();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    List<Thread> threads = new ArrayList<Thread>();
    
    threads.add(new Thread() {
      @Override
      public void run() {
        try{
          loadEntityTokens();
//          loadEntityWeighedTokens();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    
//    threads.add(new Thread() {
//      @Override
//      public void run() {
//        try{
//          loadEntityTypes();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    });
    
    threads.add(new Thread() {
      @Override
      public void run() {
        try{
          loadTokensToIds();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    
    threads.add(new Thread() {
      @Override
      public void run() {
        try{
          loadEntityWikiTypes();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    
    threads.add(new Thread() {
      @Override
      public void run() {
        try{
          loadMentionEntityPrior();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    
    threads.add(new Thread() {
      @Override
      public void run() {
        try{
          loadMentionEntities();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    
//    threads.add(new Thread() {
//      @Override
//      public void run() {
//        try{
//          loadLSH();
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
//      }
//    });
    
    for(Thread thread: threads) 
      thread.start();
    
    try {
      for(Thread thread: threads) 
        thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    try {
      loadDomainEntities();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if(NED.partlyMatchingMentionEntitySearch) {
      if(NED.lshMeansTableRMI == false)
        initMeansTable();
      else {
        
      }
    }
      
  }
  
  
  private static void initMeansTable() {
    LSHTable.CONFIDENCE_THRESHOLD = 0.8;
    meansTable = new LSHTable(4, 6, 100, 999999999);
    System.out.print("Setting lsh...");
    int counter = 0;
    try {
      // extract all possible mentions
      for(String line: Utils.getContent(Utils.getProperty("mentionEntities"))) {
        if(++counter % 500000 == 0) {
          System.out.print(counter + "...");
        }
        
        String str[] = line.split("\t");
        String mention = str[0].substring(1, str[0].length()-1).toLowerCase();
        meansTable.put(Common.getCounter(mention));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Done!");
  }
  
  public static Set<String> getSimilarName(String name) {
    return new HashSet<String>(meansTable.deduplicate(Common.getCounter(name.toLowerCase())));
  }
  
  
  private static void loadDomainEntities() throws IOException {
    domainEntities = new HashMap<String, TIntHashSet>();
    for(String domain: Utils.getContent(Utils.getProperty("domainList"))) {
      TIntHashSet entities = new TIntHashSet();
      String entityFile = Utils.getProperty("domainData") + "/" + domain + "/entities";
      for(String entity: Utils.getContent(entityFile))
        entities.add(getIdForEntity(entity));
      domainEntities.put(domain, entities);
    }
  }
  
  public static TIntHashSet getDomainEntities(String domain) {
    if(domainEntities == null) {
      try {
        loadDomainEntities();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return domainEntities.get(domain);
  }
  
  
  /**
   * the unique id for an entity-string.
   * @param entity
   * @return id
   */
  public static int getIdForEntity(String entity) {
    if(entity2id == null) {
      try {
        loadEntityId();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return entity2id.get(entity);
  }
  
  
  public static TObjectIntHashMap<String> getAllEntitiesIds() {
    if(entity2id == null) {
      try {
        loadEntityId();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return entity2id;
  }
  
  
  /**
   * Load entity dictionary into MM.
   * @throws IOException
   */
  private static void loadEntityId() throws IOException {
    System.out.print("Loading entityId...");
    entity2id = new TObjectIntHashMap<String>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("entity2Id"))) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      entity2id.put(Standardize.unicodeConverter(str[0]), Integer.parseInt(str[1]));
    }
    System.out.println("EntityId loaded!");
  }
  
  
  /**
   * Get entity in form of string from an id.
   * @param id
   * @return
   */
  public static String getEntityForId(int id) {
    if(id2entity == null) {
      try {
        loadIdEntity();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return id2entity.get(id);
  }
  
  public static TIntObjectHashMap<String> getAllIdsEntities() {
    if(id2entity == null) {
      try {
        loadIdEntity();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return id2entity;
  }
  
  
  /**
   * Load id-entity map into MM.
   * @throws IOException
   */
  private static void loadIdEntity() throws IOException {
    System.out.print("Loading IdEntity...");
    id2entity = new TIntObjectHashMap<String>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("id2Entity"))) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      id2entity.put(Integer.parseInt(str[0]), Standardize.unicodeConverter(str[1]));
    }
    System.out.println("IdEntity loaded!");
  }
  
  
  /**
   * Get entity-priors for a mention.
   * @param mention
   * @return entity-prior map.
   */
  public static TIntDoubleHashMap getEntityPriors(String mention) {
    if(priors == null) {
      try {
        loadMentionEntityPrior();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    mention = Standardize.conflateMention(mention);
    mention = Standardize.getPostgresEscapedString(Normalize.string(mention));
    TIntDoubleHashMap res = priors.get(mention);
    if(res == null)
      return new TIntDoubleHashMap();
    return res;
  }
  
  
  /**
   * Load mention-entityprior map into MM.
   * @throws IOException
   */
  private static void loadMentionEntityPrior() throws IOException {
    System.out.print("Loading Mention-entity-prior...");
    double epsilon = 0.01;
    priors = new HashMap<String, TIntDoubleHashMap>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("mentionEntityPrior"))) {
      if(++counter % 1000000 == 0)
        System.out.print(counter + "...");
      String[] str = line.split("\t");
      TIntDoubleHashMap prior = new TIntDoubleHashMap();
      int N = (str.length - 1) / 2;
      for(int i = 1; i < str.length; i += 2) {
        double d = Double.parseDouble(str[i+1]);
        prior.put(Integer.parseInt(str[i]), (d + epsilon) / (epsilon * N + 1));
      }
      priors.put(str[0], prior);
    }
    System.out.println("Loaded Mention-entity-prior!");
  }
  
  
  public static Set<String> getWikiTypes(int entity) {
    if(entity2WikiTypes == null) {
      try {
        loadEntityWikiTypes();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return entity2WikiTypes.get(entity);//new HashSet<String>(entity2WikiTypes.get(entity));
  }
  
  private static void loadEntityWikiTypes() throws IOException {
    System.out.print("Loading entity-wikiTypes...");
    entity2WikiTypes = new TIntObjectHashMap<Set<String>>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("entityTypes"))) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      int entity = Integer.parseInt(str[0]);
      Set<String> wikiTypes = new HashSet<String>();
      for(int i = 1; i < str.length; i ++)
        wikiTypes.add(str[i]);
      entity2WikiTypes.put(entity, wikiTypes);
    }
    System.out.println("Entity-wikiTypes loaded!");
  }
  
  
  
  
  public static Entities getEntitiesForMention(String mention, String type) {
    if(mention2entities == null) {
      try {
        loadMentionEntities();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    String normMention = Normalize.string(Standardize.conflateMention(mention));
//    String normMention = 
//        Standardize.getPostgresEscapedString(Normalize.string(mention));
    TIntHashSet entities = mention2entities.get(normMention);
    Entities res = new Entities();
    if(entities == null) {
      return res;
    }
    
    for(int entity: entities.toArray()){
      String name = getEntityForId(entity);
      if(name == null || name.equalsIgnoreCase(""))
        continue;
      
      // check type
      if(type == null || type.equalsIgnoreCase("UNCLEAR")) {
        res.add(new Entity(name, entity));
      }
      else {
        if((type.equalsIgnoreCase("LOCATION") || type.equalsIgnoreCase("MISC")) && DataStore.isNation(name)){
          res.add(new Entity(name, entity));
          continue;
        }
        
        String entityType = getType(entity);
        if(entityType.equalsIgnoreCase(type)) {
          res.add(new Entity(name, entity));
        }
      }
    }
    
    return res;
  }
  
  public static Map<String, TIntHashSet> getAllMentionEntities() {
    if(mention2entities == null) {
      try {
        loadMentionEntities();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return mention2entities;
  }
  
  private static void loadMentionEntities() throws IOException {
    System.out.print("Loading Mention-entities...");
    mention2entities = new HashMap<String, TIntHashSet>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("mentionEntities"))) {
      if(++counter % 1000000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      TIntHashSet entities = new TIntHashSet();
      for(int i = 1; i < str.length; i ++)
        entities.add(Integer.parseInt(str[i]));
      
      mention2entities.put(str[0], entities);
    }
    System.out.println("Mention-entities loaded!");
  }
  
  
  
  private static void loadNations() throws IOException {
    nations = new HashSet<String>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("nations"))) {
      if(++counter % 2 == 0) {
        nations.add(line.replaceAll(" ", "_"));
      }
    }
    for(String line: Utils.getContent(Utils.getProperty("nationalities"))) {
      String str[] = line.split("\t");
      nations.add(str[1]);
    }
  }
  
  public static boolean isNation(String entity) {
    if(nations == null) {
      try {
        loadNations();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return nations.contains(entity);
  }
  
  
  
  private static void loadEntityTokens() throws IOException {
    entityTokens = new TIntObjectHashMap<int[]>();
    System.out.print("Loading entity-tokens...");
    int counter = 0;
    FileInputStream fis = new FileInputStream(Utils.getProperty("entityTokens"));
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);

    String line;
    while (true) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      
      String str[] = line.split("\t");
      int[] toks = new int[str.length-1];
      for(int i = 1; i < str.length; i++) 
        toks[i-1] = Integer.parseInt(str[i]);
        
      entityTokens.put(Integer.parseInt(str[0]), toks);
    }

    isr.close();
    fis.close();
    System.out.println("entity-tokens loaded!");
  }
  
  public static int[] getTokens(int entityId) {
    if(entityTokens == null) {
      try {
        loadEntityTokens();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return entityTokens.get(entityId);
  }
  
  
  public static Set<String> getTypes(int entity) {
    if(entity2Types == null) {
      try {
        loadEntityTypes();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
   
    return entity2Types.get(entity);//new HashSet<String>(entity2Types.get(entity));
  }
  
  
  public static String getType(int entity) {
    Set<String> types = getTypes(entity);
    // priority: Org, Loc, Per, Misc
    if(types == null)
      return "MISC";
    if(types.size() == 1)
      for(String type: types)
        return type;
    if(types.contains("ORGANIZATION"))
      return "ORGANIZATION";
    if(types.contains("LOCATION"))
      return "LOCATION";
    if(types.contains("PERSON"))
      return "PERSON";
    return "MISC";
  }
  
  private static void loadEntityTypes() throws IOException {
    System.out.print("Loading entity-types...");
    entity2Types = new TIntObjectHashMap<Set<String>>();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("typeRelation"))) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      Set<String> types = new HashSet<String>();
      for(int i = 1; i < str.length; i++) {
        if(str[i].equalsIgnoreCase("null") == false)
        types.add(str[i]);
      }
      entity2Types.put(getIdForEntity(str[0]), types);
    }
    System.out.println("entity-types loaded!");
  }
  
  private static void loadLSH() throws IOException{
    System.out.print("Setting up LSH...");
    categoryTable = new LSHTable(6, 8, 128, 128);
    LSHTable.CONFIDENCE_THRESHOLD = 0.8;
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("yagoTaxonomy"))) {
      if(++counter % 50000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      if(str[0].length() > 0)
        categoryTable.put(Common.getCounter(WikiWordnetMapper.standardize(str[1])));
    }
    System.out.println("LSH built!");
  }
  
  public static String getNearestString(String wikiType) {
    if(categoryTable == null) {
      try {
        loadLSH();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return categoryTable.getNearest(Common.getCounter(wikiType));
  }
  
  
  public static TObjectIntHashMap<String> getIdsForWords(Set<String> tokens){
    try {
      if(token2Id == null)
        loadTokensToIds();
    } catch (Exception e){
      e.printStackTrace();
    }
    TObjectIntHashMap<String> res = new TObjectIntHashMap<String>();
    for(String token: tokens)
      res.put(token, token2Id.get(token));
    return res;
  }
  
  public static int getIdForWord(String token){
    try {
      if(token2Id == null)
        loadTokensToIds();
    } catch (Exception e){
      e.printStackTrace();
    }
    return token2Id.get(token);
  }
  
  private static void loadTokensToIds() throws Exception{
    System.out.print("Loading token2Id map...");
    token2Id = new TObjectIntHashMap<String>();
    List<String> lines = Utils.getContent(Utils.getProperty("token2Id"));
    int counter = 0;
    for(String line: lines){
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      if(str.length == 2)
        token2Id.put(str[0], Integer.parseInt(str[1]));
    }
    System.out.println("token2Id loaded!");
  }
  
  public static TObjectIntHashMap<String> getAllTokensIds() {
    try {
      if(token2Id == null)
        loadTokensToIds();
    } catch (Exception e){
      e.printStackTrace();
    }
    return token2Id;
  }
  
  public static boolean isStopword(int tokenId){
    if(stopwordIDs == null){
      try{
        loadStopWordIds();
      }catch (IOException e){
        e.printStackTrace();
      }
    }
    return stopwordIDs.contains(tokenId);
  }
  
  public static boolean isStopword(Token token){
    if(token.getOriginal().length() <= 1)
      return true;
    if(token.getOriginal().startsWith("'"))
      return true;
    if(stopwordIDs == null){
      try{
        loadStopWordIds();
      }catch (IOException e){
        e.printStackTrace();
      }
    }
    return stopwordIDs.contains(getIdForWord(token.getOriginal()));
  }
  
  public static void loadStopWordIds() throws IOException{
    if(stopwordIDs != null){
      return;
    }
    List<String> sws = Utils.getContent(Utils.getProperty("stopwords"));
    stopwordIDs = new TIntHashSet();
    for(String sw: sws){
      int id = getIdForWord(sw);
      stopwordIDs.add(id);
    }
  }
  
  
  
  private static void loadRelatedEntities() throws IOException {
    relatedEntities = new TIntObjectHashMap<TIntHashSet>();
    for(String line: Utils.getContent(Utils.getProperty("wikiCategoryGraph"))) {
      String str[] = line.split("\t");
      if(str.length > 1) {
        TIntHashSet set = new TIntHashSet();
        for(int i = 1; i < str.length; i ++)
          set.add(Integer.parseInt(str[i]));
        relatedEntities.put(Integer.parseInt(str[0]), set);
      }
    }
  }
  
  public static TIntHashSet getRelatedEntities(int entity) {
    if(relatedEntities == null) {
      try {
        loadRelatedEntities();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    TIntHashSet set = relatedEntities.get(entity);
    if(set == null)
      return new TIntHashSet();
    return set;
  }
  
  public static boolean isRelated(int entity1, int entity2) {
    TIntHashSet set = relatedEntities.get(entity1);
    if(set != null) {
      if(set.contains(entity2))
        return true;
    }
    set = relatedEntities.get(entity2);
    if(set != null && set.contains(entity1))
      return true;
    return false;
  }
  
  
  
  private static void loadTypeRelatedEntities() throws IOException {
    type2relatedEntities = new HashMap<String, TIntHashSet>();
    for(String line: Utils.getContent(Utils.getProperty("typeRelatedEntities"))) {
      String str[] = line.split("\t");
      TIntHashSet set = new TIntHashSet();
      for(int i = 1; i < str.length; i ++)
        set.add(Integer.parseInt(str[i]));
      type2relatedEntities.put(str[0], set);
    }
  }
  
  public static TIntHashSet getRelatedEntityByWikiCategory(String type) {
    if(type2relatedEntities == null) {
      try {
        loadTypeRelatedEntities();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    TIntHashSet res = type2relatedEntities.get(type);
    if(res == null)
      return new TIntHashSet();
    return res;
  }
  
  
  
  public static TObjectIntHashMap<String> getEntityDictionary() {
    if(entity2id == null) {
      try {
        loadEntityId();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return entity2id;
  }
  
  
  
  public static boolean isMention(String mention) {
    mention = Standardize.conflateMention(mention);
    String normMention = 
        Standardize.getPostgresEscapedString(Normalize.string(mention));
    return mention2entities.containsKey(normMention);
  }
  
  
  private static void loadTokenCounter() throws IOException {
    tokenCounter = new TIntIntHashMap();
    System.out.print("Loading tokenCounter...");
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("tokenWeight"))) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      tokenCounter.put(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
    }
    System.out.println("tokenCounter loaded!");
  }
  
  public static int countOccurrence(int token) {
    if(tokenCounter == null) {
      try {
        loadTokenCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return tokenCounter.get(token);
  }
  
  
  
  public static void main(String args[]) throws Exception {
//    buildEntityTypes();
//    buildEntityTokens();
//    buildEntitiyIdDictionary();
//    buildMentionEntities();
//    buildEntityWikiTypes();
//    sortEntityTokens();
//    buildMentionEntityPrior();
//    buildEntityWeightedTokens();
//    buildTokenWeights();
//    buildEntityTokensFromWikiCategories();
//    buildTypeRelatedEntities();
//    addInvertedIndex();
//    buildLinkTo();
    
  }
}
