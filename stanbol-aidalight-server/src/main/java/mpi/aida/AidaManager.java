package mpi.aida;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.OokbEntity;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.preparation.AidaTokenizerManager;
import mpi.aida.preparation.mentionrecognition.FilterMentions;
import mpi.aida.preparation.mentionrecognition.FilterMentions.FilterType;
import mpi.aida.util.ClassPathUtils;
import mpi.aida.util.YagoUtil.Gender;
import mpi.aidalight.DataStore;
import mpi.database.DBConnection;
import mpi.database.DBSettings;
import mpi.database.MultipleDBManager;
import mpi.tokenizer.data.Tokenizer;
import mpi.tokenizer.data.Tokens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basics.Normalize;

public class AidaManager {
  private static Logger slogger_ = LoggerFactory.getLogger(AidaManager.class);
  
  // This is more couple to SQL than it should be. Works for now.
  public static final String DB_AIDA = "DatabaseAida";
  public static final String DB_YAGO2_FULL = "DatabaseYago2Full";
  public static final String DB_YAGO2_SPOTLX = "DatabaseYago2SPOTLX";
  public static final String DB_RMI_LOGGER = "DatabaseRMILogger";

  private static String databaseAidaConfig = "database_aida.properties";
  private static String databaseOokbeConfig = "database_ookbe.properties";
  private static String databaseHarvestConfig = "database_harvest.properties";
  private static String databaseYAGO2FullConfig = "database_yago2full.properties";
  private static String databaseYAGO2SPOTLXConfig = "database_yago2spotlx.properties";
  private static String databaseRMILoggerConfig = "databaseRmiLogger.properties";

  public static final String WIKIPEDIA_PREFIX = "http://en.wikipedia.org/wiki/";
  public static final String YAGO_PREFIX = "http://yago-knowledge.org/resource/";

  private static final String WORD_EXPANSION_CACHE = "aida-word_expansions.cache";

  private static final String DATABASE_AIDA_CONFIG_CACHE = "database_aida.cache";

  private static Map<String, String> dbIdToConfig = new HashMap<String, String>();

  static {
    dbIdToConfig.put(DB_AIDA, databaseAidaConfig);
    dbIdToConfig.put(DB_YAGO2_FULL, databaseYAGO2FullConfig);   
    dbIdToConfig.put(DB_YAGO2_SPOTLX, databaseYAGO2SPOTLXConfig);
    dbIdToConfig.put(DB_RMI_LOGGER, databaseRMILoggerConfig);
  }
  
  private static AidaManager tasks = null;
  
  // Blacklist for entities used when retrieving candidates.
  // Empty by default.
  public static Set<Entity> entityBlacklist = new HashSet<Entity>();

  /** Preloaded word expansions. */
  private static int[] wordExpansions_ = null;

  public static enum language {
    english, german
  }

  private static final Set<String> malePronouns = new HashSet<String>() {

    private static final long serialVersionUID = 2L;
    {
      add("He");
      add("he");
      add("Him");
      add("him");
      add("His");
      add("his");
    }
  };

  private static final Set<String> femalePronouns = new HashSet<String>() {

    private static final long serialVersionUID = 3L;
    {
      add("she");
      add("she");
      add("Her");
      add("her");
      add("Hers");
      add("hers");
    }
  };

  public static void init() {
    getTasksInstance();
  }

  private static synchronized AidaManager getTasksInstance() {
    if (tasks == null) {
      tasks = new AidaManager();
    }
    return tasks;
  }

//  private static synchronized void initWordExpansion() {
//    if (AidaConfig.getBoolean(AidaConfig.CACHE_WORD_EXPANSIONS)) {
//      try {
//        wordExpansions_ = createAndLoadCache();
//      } catch (IOException e) {
//        slogger_.warn("Could not read cache file, reading from DB.", e);
//        wordExpansions_ = DataAccess.getAllWordExpansions();
//      }
//    } else {
//      slogger_.info("Loading word_expansions from DB.");
//      wordExpansions_ = DataAccess.getAllWordExpansions();
//    }
//    slogger_.info("Done loading word_expansions.");
//  }

//  private static int[] createAndLoadCache() throws IOException {
//    boolean requireReadFromDB = false;
//    int[] wordExps = null;
//    File wordExpansionCache = new File(WORD_EXPANSION_CACHE);
//    if (wordExpansionCache.exists()) {
//      File cachedDBConfigFile = new File(DATABASE_AIDA_CONFIG_CACHE);
//      Properties currentAIDADBConfig = ClassPathUtils.getPropertiesFromClasspath(databaseAidaConfig);
//      Properties cachedAIDADBConfig = new Properties();
//      cachedAIDADBConfig.load(new BufferedInputStream(new FileInputStream(cachedDBConfigFile)));
//
//      if (currentAIDADBConfig.equals(cachedAIDADBConfig)) {
//
//        slogger_.info("Loading word_expansions from cache.");
//        DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(wordExpansionCache))));
//        wordExps = new int[in.readInt()];
//        for (int i = 0; i < wordExps.length; ++i) {
//          wordExps[i] = in.readInt();
//        }
//        in.close();
//      } else {
//        slogger_.info("Word expansion cache file exists, but DB config has been changed since it was created; DB access is unavoidable!");
//        //there is a change in the DB config
//        // do a clean up and require a DB access
//        wordExpansionCache.delete();
//        cachedDBConfigFile.delete();
//        requireReadFromDB = true;
//      }
//    } else {
//      //no cached file exists
//      slogger_.info("Word expansions cache file doesn't exist.");
//      requireReadFromDB = true;
//    }
//
//    if (requireReadFromDB) {
//      slogger_.info("Loading word_expansions from DB.");
//      wordExps = DataAccess.getAllWordExpansions();
//      slogger_.info("Caching word_expansions to disk.");
//      // Cache to disk for next run.
//      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(wordExpansionCache))));
//      out.writeInt(wordExps.length);
//      for (int exp : wordExps) {
//        out.writeInt(exp);
//      }
//
//      out.flush();
//      out.close();
//      slogger_.info("Caching DB Config!");
//      Properties currentAIDADBConfig = ClassPathUtils.getPropertiesFromClasspath(databaseAidaConfig);
//      File cachedDBConfigFile = new File(DATABASE_AIDA_CONFIG_CACHE);
//      currentAIDADBConfig.store(new BufferedOutputStream(new FileOutputStream(cachedDBConfigFile)), "cached aida DB config");
//    }
//    return wordExps;
//  }

  public static PreparedInput prepareInputData(String text, String docId, FilterType by) {
    return AidaManager.getTasksInstance().createPrepareInputData(text, docId, by);
  }

  /**
   * tokenizes only the text,
   * 
   * @param docId
   * @param text
   * @return
   */
  public static Tokens tokenize(String docId, String text, boolean lemmatize) {
    return AidaManager.getTasksInstance().tokenize(docId, text, Tokenizer.type.tokens, lemmatize);
  }

  public static Tokens tokenize(String docId, String text) {
    return AidaManager.getTasksInstance().tokenize(docId, text, Tokenizer.type.tokens, false);
  }

  /**
   * tokenizes the text with POS and NER
   * 
   * @param docId
   * @param text
   * @return
   */
  public static Tokens tokenizeNER(String docId, String text, boolean lemmatize) {
    return AidaManager.getTasksInstance().tokenize(docId, text, Tokenizer.type.ner, lemmatize);
  }

  /**
   * tokenizes the text with POS
   * 
   * @param docId
   * @param text
   * @return
   */
  public static Tokens tokenizePOS(String docId, String text, boolean lemmatize) {
    return AidaManager.getTasksInstance().tokenize(docId, text, Tokenizer.type.pos, lemmatize);
  }

  /**
   * tokenizes the text with PARSE
   * 
   * @param docId
   * @param text
   * @return
   */
  public static Tokens tokenizePARSE(String docId, String text, boolean lemmatize) {
    return AidaManager.getTasksInstance().tokenize(docId, text, Tokenizer.type.parse, lemmatize);
  }

  /**
   * tokenizes the text with PARSE
   * 
   * @param docId
   * @param text
   * @return
   */
  public static Tokens tokenizePARSE(String docId, String text, Tokenizer.type type, boolean lemmatize) {
    return AidaManager.getTasksInstance().tokenize(docId, text, type, lemmatize);
  }

//  public static synchronized DBConnection getConnectionForDatabase(String dbId, String req) throws SQLException {
//    if (!MultipleDBManager.isConnected(dbId)) {
//      try {
//        Properties prop = ClassPathUtils
//            .getPropertiesFromClasspath(dbIdToConfig.get(dbId));
//        String type = prop.getProperty("type");
//        String service = null;
//        if (type.equalsIgnoreCase("Oracle")) {
//          service = prop.getProperty("serviceName");
//        } else if (type.equalsIgnoreCase("PostGres")) {
//          service = prop.getProperty("schema");
//        }
//        String hostname = prop.getProperty("hostname");
//        Integer port = Integer.parseInt(prop.getProperty("port"));
//        String username = prop.getProperty("username");
//        String password = prop.getProperty("password");
//        Integer maxCon = Integer.parseInt(prop.getProperty("maxConnection"));
//        
//        DBSettings settings = new DBSettings(
//            hostname, port, username, password, maxCon, type, service);
//        MultipleDBManager.addDatabase(dbId, settings);
//        slogger_.info("Connecting to " + type + " database " + username + "@" +
//                       hostname + ":" + port + "/" + service);
//      } catch (Exception e) {
//    	  slogger_.error(
//    	      "Error connecting to the AIDA database: " + e.getLocalizedMessage());
//    	}
//    }
//    if (!MultipleDBManager.isConnected(dbId)) {
//      slogger_.error("Could not connect to the AIDA database. " +
//      		"Please check the settings in 'settings/database_aida.properties'" +
//      		"and make sure the Postgres server is up and running.");
//      return null;
//    }    
//    return MultipleDBManager.getConnection(dbId, req);
//  }

  public static void releaseConnection(String dbId, DBConnection con) {
    MultipleDBManager.releaseConnection(dbId, con);
  }
  
  public static String getOokbeDatabasePath() throws IOException {
    Properties prop = ClassPathUtils.getPropertiesFromClasspath(databaseOokbeConfig);
    return prop.getProperty("path");
  }
  
  public static String getHarvestDatabasePath() throws IOException {
    Properties prop = ClassPathUtils.getPropertiesFromClasspath(databaseHarvestConfig);
    return prop.getProperty("path");
  }

  /**
   * Gets an AIDA entity for the given YAGO entity id.
   * This is slow, as it accesses the DB for each call.
   * Do in batch using DataAccess directly for a larger number 
   * of entities.
   * 
   * @param yagoEntityId  ID in YAGO2 format
   * @return              AIDA Entity
   */
  public static Entity getEntity(String yagoEntityId) {
    return new Entity(yagoEntityId, DataStore.getIdForEntity(yagoEntityId));
  }
  
  /**
   * Gets an AIDA OOKB entity for the given name.
   * 
   * @param name  Name as found in text (UTF-8)
   * @return      AIDA OOKB Entity
   */
  public static OokbEntity getOokbEntity(String name) {
    return new OokbEntity(name, DataStore.getIdForEntity(name));
  }
  
  /**
   * Gets an AIDA entity for the given AIDA entity id.
   * This is slow, as it accesses the DB for each call.
   * Do in batch using DataAccess directly for a larger number 
   * of entities.
   * 
   * @param entityId  Internal AIDA int ID 
   * @return          AIDA Entity
   */
  public static Entity getEntity(int entityId) {
    return new Entity(DataStore.getEntityForId(entityId), entityId);
  }
  
  /**
   * Creates the Wikipedia link for a given ResultEntity.
   * 
   * @param resultEntity  Given AIDA ResultEntity.
   * @return  Wikipedia Link for the entity.
   */
  public static String getWikipediaUrl(ResultEntity resultEntity) {
    Entity entity = getEntity(resultEntity.getEntity());
    String titlePart = Normalize.unEntity(entity.getName()).replace(' ', '_');
    return WIKIPEDIA_PREFIX + titlePart;
  }
  
  /**
   * Creates the YAGO identifier for a given ResultEntity.
   * 
   * @param resultEntity  Given AIDA ResultEntity.
   * @return  YAGO Resource for the entity.
   */
  public static String getYAGOIdentifier(ResultEntity resultEntity) {
    Entity entity = getEntity(resultEntity.getEntity());
    String titlePart = Normalize.unEntity(entity.getName()).replace(' ', '_');
    return YAGO_PREFIX + titlePart;
  }
    
  /**
   * Creates the Wikipedia link for a given entity.
   * 
   * @param entity  Given AIDA entity.
   * @return  Wikipedia Link for the entity.
   */
  public static String getWikipediaUrl(Entity entity) {
    String titlePart = Normalize.unEntity(entity.getName()).replace(' ', '_');
    return WIKIPEDIA_PREFIX + titlePart;
  }
  
  /**
   * Creates the YAGO identifier for a given entity.
   * 
   * @param entity  Given AIDA entity.
   * @return  YAGO Resource for the entity.
   */
  public static String getYAGOIdentifier(Entity entity) {
    String titlePart = Normalize.unEntity(entity.getName()).replace(' ', '_');
    return YAGO_PREFIX + titlePart;
  }  
  
  
  /**
   * Returns the potential entity candidates for a mention (via the YAGO
   * 'means' relation)
   * 
   * @param mention
   *            Mention to get entity candidates for
   * @return Candidate entities for this mention.
   * 
   */
  public static Entities getEntitiesForMention(String mention) {
    return DataStore.getEntitiesForMention(mention, null);
  }
  /**
   * Returns the potential entity candidates for a mention (via the YAGO
   * 'means' relation)
   * 
   * @param mention
   *            Mention to get entity candidates for
   * @param maxEntityRank Retrieve entities up to a global rank, where rank is 
   * between 0.0 (best) and 1.0 (worst). Setting to 1.0 will retrieve all entities.
   * @return Candidate entities for this mention.
   * 
   */
  public static Entities getEntitiesForMention(Mention mention, double maxEntityRank) {
    return DataStore.getEntitiesForMention(mention.getMention(), null);
  }

  /**
   * Returns the potential entity candidates for a mention (via the YAGO
   * 'means' relation) and filters those candidates against the given list of
   * types
   * 
   * @param mention
   *            Mention to get entity candidates for
   * @return Candidate entities for this mention (in YAGO2 encoding) including
   *         their prior probability
   * @throws SQLException
   */
  public static Entities getEntitiesForMention(Mention  mention, List<String> filteringTypes, double maxEntityRank) throws SQLException {
    Entities entities = getEntitiesForMention(mention, maxEntityRank);
    Entities filteredEntities = new Entities();
    for (Entity entity : entities) {
      String entityName = entity.getName();
      Set<String> entityTypes = DataStore.getTypes(DataStore.getIdForEntity(entityName));
      for (String filteringType : filteringTypes) {
        if (entityTypes.contains(filteringType)) {
          filteredEntities.add(entity);
          break;
        }
      }
    }
    return filteredEntities;
  }

  
  public static Map<String, Gender> getGenderForEntities(Entities entities) {
    return new HashMap<String, Gender>();
  }

  
  public static void fillInCandidateEntities(Mentions mentions) throws SQLException {
    fillInCandidateEntities(null, mentions, false, false, 1.0);
  }

  /**
   * Retrieves all the candidate entities for the given mentions.
   * 
   * @param docId Id of the input doc.
   * @param mentions  All mentions in the input doc.
   * @param includeNullEntityCandidates Set to true to include mentions flagged
   * as NME in the ground-truth data.
   * @param includeContextMentions  Include mentions as context.
   * @param maxEntityRank Fraction of entities to include. Between 0.0 (none)
   * and 1.0 (all). The ranks are taken from the entity_rank table.
   * @throws SQLException
   * 
   * TODO get candidates for all mentions at once.
   */
  public static void fillInCandidateEntities(
      String docId, Mentions mentions, boolean includeNullEntityCandidates, 
      boolean includeContextMentions, double maxEntityRank) throws SQLException {
    List<String> filteringTypes = mentions.getEntitiesTypes();
    for (int i = 0; i < mentions.getMentions().size(); i++) {
      Mention m = mentions.getMentions().get(i);
      Entities mentionCandidateEntities;
      if (malePronouns.contains(m.getMention())
          || femalePronouns.contains(m.getMention())) {
        setCandiatesFromPreviousMentions(mentions, i);
      } else {
        if (filteringTypes != null) {
          mentionCandidateEntities = AidaManager.getEntitiesForMention(m,
              filteringTypes, maxEntityRank);
        } else {
          mentionCandidateEntities = AidaManager.getEntitiesForMention(m,
              maxEntityRank);
        }
        for (Entity e : entityBlacklist) {
          mentionCandidateEntities.remove(e);
        }
        if (includeNullEntityCandidates) {
          OokbEntity ookbEntity = getOokbEntity(m.getMention());
          // Add confusable enities (other candidates of the same mention).
          List<Entity> confusableEntities = 
              new ArrayList<Entity>(mentionCandidateEntities.size());
          for (Entity confusableEntity : mentionCandidateEntities) {
            confusableEntities.add(confusableEntity);
          }
          ookbEntity.setConfusableEntities(confusableEntities);
          
          // Add surrounding mentions as context.
          if (includeContextMentions) {
            List<String> surroundingMentionsNames = new LinkedList<String>();
            int begin = Math.max(i - 2, 0);
            int end = Math.min(i + 3, mentions.getMentions().size());

            for (int s = begin; s < end; s++) {
              if (s == i) continue; // skip mention itself
              surroundingMentionsNames.add(mentions.getMentions().get(s)
                  .getMention());
            }
            ookbEntity.setSurroundingMentionNames(surroundingMentionsNames);
          }
          mentionCandidateEntities.add(ookbEntity);
        }
        m.setCandidateEntities(mentionCandidateEntities);
      }
    }
  }

  private static void setCandiatesFromPreviousMentions(Mentions mentions, int mentionIndex) {
    Mention mention = mentions.getMentions().get(mentionIndex);
    Entities allPrevCandidates = new Entities();
    if (mentionIndex == 0) {
      mention.setCandidateEntities(allPrevCandidates);
      return;
    }

    for (int i = 0; i < mentionIndex; i++) {
      Mention m = mentions.getMentions().get(i);
      for (Entity e : m.getCandidateEntities()) {
        allPrevCandidates.add(new Entity(e.getName(), e.getId()));
      }
    }

    Map<String, Gender> entitiesGenders = AidaManager.getGenderForEntities(allPrevCandidates);

    Gender targetGender = null;
    if (malePronouns.contains(mention.getMention())) targetGender = Gender.MALE;
    else if (femalePronouns.contains(mention.getMention())) targetGender = Gender.FEMALE;

    Entities filteredCandidates = new Entities();
    for (Entity e : allPrevCandidates) {
      if (entitiesGenders != null && entitiesGenders.containsKey(e.getName()) && entitiesGenders.get(e.getName()) == targetGender) filteredCandidates.add(e);
    }
    mention.setCandidateEntities(filteredCandidates);
  }

  public static boolean isNamedEntity(String entity) {
    return AidaManager.getTasksInstance().checkIsNamedEntity(entity);
  }

  public static void main(String[] args) throws SQLException {
    Entities entities = getEntitiesForMention("Germany");
    for (Entity entity : entities) {
      System.out.println(entity.getName());
    }
  }

  // singleton class methods private methods  
  
  private FilterMentions filterMention = null;

  private AidaManager() {
    filterMention = new FilterMentions();
    AidaTokenizerManager.init();
//    initWordExpansion();
  }

  private PreparedInput createPrepareInputData(String text, String docId, FilterType by) {
    PreparedInput preparedInput = null;
    if (by.equals(FilterType.Manual) || by.equals(FilterType.Hybrid)) {
      preparedInput = filterMention.filter(text, docId, null, by);
    } else {
      Tokens tokens = tokenize(docId, text, Tokenizer.type.ner, false);
      preparedInput = filterMention.filter(text, docId, tokens, by);
      preparedInput.setTokens(tokens);
    }
    return preparedInput;
  }

  private Tokens tokenize(String docId, String text, Tokenizer.type type, boolean lemmatize) {
    return AidaTokenizerManager.tokenize(docId, text, type, lemmatize);
  }

  private boolean checkIsNamedEntity(String entity) {
    if (Normalize.unWordNetEntity(entity) == null 
        && Normalize.unWikiCategory(entity) == null 
        && Normalize.unGeonamesClass(entity) == null 
        && Normalize.unGeonamesEntity(entity) == null) {
      return true;
    } else {
      return false;
    }
  }

  public static String expandTerm(String term) {
    return term.toUpperCase(Locale.ENGLISH);
  }

  /** 
   Gets the ALL_UPPERCASE wordId for the input wordId.
   NEED TO CALL INIT FIRST!.
   */
  public static int expandTerm(int wordId) {
    assert wordId >= 0 : "wordId must not be negative.";
    assert wordId < wordExpansions_.length : "wordId out of range.";
    return wordExpansions_[wordId];
  }
}
