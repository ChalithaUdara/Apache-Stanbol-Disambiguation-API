package mpi.experiment;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import mpi.aida.AidaManager;
import mpi.aida.Disambiguator;
import mpi.aida.config.settings.ConfidenceSettings;
import mpi.aida.config.settings.ConfidenceSettings.SCORE_TYPE;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.GraphSettings;
import mpi.aida.config.settings.Settings;
import mpi.aida.config.settings.Settings.TECHNIQUE;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.util.DocumentCounter;
import mpi.aida.util.RunningTimer;
import mpi.experiment.evaluation.Evaluator;
import mpi.experiment.reader.CollectionReader;
import mpi.experiment.reader.CollectionReader.DataSource;
import mpi.experiment.reader.CollectionReaderSettings;
import mpi.experiment.reader.SolutionReader;
import mpi.experiment.trace.GraphTracer;
import mpi.experiment.trace.GraphTracer.TracingTarget;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO stopwords flag IS NOT USED at the moment - include in disambiguationsettings
 * 
 *
 */
public class RunExperiment implements Observer {
  private static final Logger logger = 
      LoggerFactory.getLogger(RunExperiment.class);

  public static final String coherence_test_simsetting_name = "coherence_robustness_similarity.properties";

  public static String experimentSimilarityCachePath = null;

  public static boolean forceScoreCache = false;
  
  private ExperimentalSettings exSettings;
  private DisambiguationSettings disSettings;
  
  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    AidaManager.init();
    new RunExperiment().run(args);
  }

  public String run(String[] args) throws ParseException, InterruptedException, IOException, SQLException {
    logger.info("Running with args: " + StringUtils.join(args, " "));
    // Build DisambiguationSettings from input parameters
    CommandLine cmd = buildCommandLine(args);

    exSettings = buildExperimentalSettings(cmd);
    disSettings = buildDisambiguationSettings(cmd);
    
    if (exSettings.getPreviousResults() != null) {
      // OOKBE is preprocessing, disable ookbe in current run.
      disSettings.setIncludeNullAsEntityCandidate(false);
      disSettings.setNullMappingThreshold(-100.0);
    }

    String resultFilePath = getResultFilePath(exSettings, disSettings);
    String resultFileName = new File(resultFilePath).getName();
    long beginTime = System.currentTimeMillis();
    // execute disambiguation
    if (!exSettings.shouldOnlyDoEvaluation()) {
      Map<String, DisambiguationResults> solutions = runDisambiguation(exSettings, disSettings, resultFilePath);
      writeSolutions(exSettings, resultFilePath, solutions);
      evaluateResult(exSettings, resultFilePath);
      if (exSettings.shouldTrace()) {
        writeTracing(resultFileName, solutions);
      }
      if (exSettings.shouldGraphTrace()) {
        writeGraphTracing(solutions, getExperimentsStorageDirectory(exSettings) +
                         File.separator + "html" + File.separator + "all" + 
                         File.separator + resultFileName);
      }
    } else {
      evaluateAllResults(exSettings, resultFilePath);
    }    
	  long runTime = (System.currentTimeMillis() - beginTime) / 1000;
	  System.out.println("Done processing the collection in " + runTime + "s");
	  
	  if (cmd.hasOption("timing")) {
	    System.out.println(RunningTimer.getOverview());
	  }
	  
	  System.out.println(resultFilePath);
	  return resultFilePath;
  }

  private CommandLine buildCommandLine(String[] args) throws ParseException {
    // Parse Options
    Options options = new Options();

    // Algorithmic options
    options.addOption("a", true, "a is multiplied to ME edges, 1-a to EE edges (a is in [0.0, 1.0])");
    options.addOption("c", true, "Use coherence robustness with the given threshold");
    options.addOption("i", true, "Sets the factor of how many entities to include in the initial graph (i * mentions)");
    options.addOption("j", false, "If present, include context mention when building the Bing keyphrase query");
    options.addOption("k", false, "If present, create all EE edges, regardless of distinct mentions");
    options.addOption("l", true, "specify the algorithm to run: CP, CPSC, LCPWN, LOCAL, ORACLE, TAGME, IW");
    options.addOption("n", false, "If present, use normalized objective for graph - otherwise, divide edge weighted degree by number of remaining entities");
    options.addOption("o", true, "Threshold for NULL mapping (mentions with confidence below threshold are mapped to NULL");
    options.addOption("q", true, "Add NULL entities candidates to graph representation, setting the OOKBE balance to use (in [0.0, \\infty) ) - will be multiplied with the real entity/keyphrase counts and the ratio between OOKBE collection size and real collection size.");
    options.addOption("s", true, "path to similarity.properties to use");
    options.addOption("w", true, "Set the score balance of OOKBE entities. Final OOKBE entity weights will be multiplied by the argument (in [0.0, \\infty) ).");
    options.addOption("x", false, "if present, run exhaustive search to solve disambiguation, otherwise local search");
    options.addOption("y", true, "path to HYENA.properties to use");
    options.addOption("confidence", false, "If present, use confidence computation instead of local similarity for scores.");
    options.addOption("rerunconfidence", false, "If present, use graph re-run confidence computation.");
    options.addOption("combinererunconfidence", false, "If present, combine graph re-run confidence computation with normalized scores.");
    options.addOption("ookbedays", true, "Number of days to go back from current days for accumulating OOKBE keyphrases and counts.");
    options.addOption("graphconfidencebalance", true, "Use the parameter (in [0.0, 1.0]) to balance the LOCAL and COHERENCE confidence in GRAPH mode (param is multiplied to LOCAL, 1-param to COHERENCE).");
    options.addOption("graphconfidencescore", true, "Score to use for confidence: LOCAL or WEIGHTED_DEGREE");
    options.addOption("graphconfidencementionflippercentage", true, "Use the parameter (in [0.0, 1.0]) to set the maximum percentage of mentions to be flipped in the COHERENCE confidence estimation (GRAPH mode).");
    
    // Options to override SimilaritySettings
    options.addOption("maxentitykeyphrasecount", true, "Maximum number of entity keyphrases (ordered by keyphrase score)");
    options.addOption("harvestkps", false, "Set to use harvested keyphrases.");

    // Experimental Options
    options.addOption("d", true, "CHAKRA or CONLL or WIKIPEDIA_YAGO2 or AIDA or NONE");
    options.addOption("e", "numberThreads", true, "Set the number of parallel threads doing a disambiguation. For LOCAL this can be one per CPU, for GRAPH you need to watch the main memory");
    options.addOption("f", true, "Only read collection starting from 'f'");
    options.addOption("m", false, "If present, include empty mentions");
    options.addOption("g", false, "if present, enable graph tracing");
    options.addOption("p", true, "path to collection data (reader needs to be aware of structure)");
    options.addOption("r", false, "if present, enable tracing (this reduces parallel execution due to memory overhead)");
    options.addOption("t", true, "Only read collection up to 't'");
    options.addOption("v", false, "if present, only evaluation is run");
    options.addOption("z", "collectionpart", true, "Use TRAIN, DEV, TEST to specifiy different parts of the " + "collection, instead of f/t");
    options.addOption("restrictmentions", false, "Set to restrict mentions to 'mentions_to_evaluate.txt' in the collection path");
    options.addOption("restrictmentionnames", false, "Set to restrict mentions to 'mention_names_to_evaluate.txt' in the collection path");
    options.addOption("force", "force", false, "Use to force graph generation");
    options.addOption("linkeval", "linkeval", false, "Enable the evaluation based on inlink count");
    options.addOption("timing", "timing", false, "Give the timing summary at the end.");
    options.addOption("nereval", true, "Evaluate NER, use configuration passed as parameter.");
    options.addOption("nermode", true, "Set dictionary NER mode: ORIG, STEM, LEMM.");
    options.addOption("nerheuristic", true, "Set dictionary NER heuristic: NONE, StanfordNER, Nouns, PNouns, PNounsPlusNounsInQMarks, PNounsPlusNounsInBrackets, PNounsPlusNounsInQMarksBrackets.");
    options.addOption("conflated", false, "Use conflated dictionaries with conflated text.");
    options.addOption("excludeoodmentions", false, "Exclude mentions that are not in the dictionary.");
    options.addOption("minmentioncount", true, "Set the number of times a mention has to have occurred in the (chronological) collection up to this document to be considered");
    options.addOption("mentioncountdayspan", true, "Set the number of days to be considered when counting the occurrence of a mention (starting from the day of the input document backwards)");
    options.addOption("dropentities", true, "When working on a dataset where entities have been dropped by ArtificialNullmentionDatasetGenerator, set this to the dropped_entities.txt output file.");
    options.addOption("ookbepreprocess", true, "Set to results of previous OOKBE run to re-use this as preprocessing");
    options.addOption("useookbe", false, "Set to put placeholders for OOKBE when doing the algorithm.");

    CommandLineParser parser = new PosixParser();
    CommandLine cmd = parser.parse(options, args);

    return cmd;
  }

  private ExperimentalSettings buildExperimentalSettings(CommandLine cmd) throws IOException {

    // Fill (static) entity blacklist
    if (cmd.hasOption("dropentities")) {
      fillEntityBlacklist(new File(cmd.getOptionValue("dropentities")));
    }
    
    // Default is to exclude mentions that have --NME-- as GT.
    boolean shouldIncludeNMEMentions = false;

    if (cmd.hasOption("m")) {
      shouldIncludeNMEMentions = true;
    }
        
    CollectionReaderSettings crSettings = new CollectionReaderSettings();
    crSettings.setIncludeNMEMentions(shouldIncludeNMEMentions);
    if (cmd.hasOption("excludeoodmentions")) {
      crSettings.setIncludeOutOfDictionaryMentions(false);
    }

    if (cmd.hasOption("minmentioncount")) {
      crSettings.setMinMentionOccurrence(
          Integer.parseInt(cmd.getOptionValue("minmentioncount")));
    }
    
    if (cmd.hasOption("mentioncountdayspan")) {
      crSettings.setMentionOccurrenceDaySpan(
          Integer.parseInt(cmd.getOptionValue("mentioncountdayspan")));
    }    
       
    String dataSetType = cmd.getOptionValue("d");
    DataSource ds = DataSource.valueOf(dataSetType);

    int from = 0;
    if (cmd.hasOption("f")) {
      from = Integer.parseInt(cmd.getOptionValue("f"));
    }

    int to = Integer.MAX_VALUE;
    if (cmd.hasOption("t")) {
      to = Integer.parseInt(cmd.getOptionValue("t"));
    }

    String collectionPath = cmd.getOptionValue("p");
    ExperimentalSettings es = new ExperimentalSettings(collectionPath, ds, CollectionReader.getCollectionPart(cmd.getOptionValue("z")), from, to, crSettings);

    // set cache path for scores
    experimentSimilarityCachePath = collectionPath + File.separator + "scores_cache";

    // do not cache scores, only use cached scores
    //    File cacheFile = new File(experimentSimilarityCachePath);
    //    if (!cacheFile.exists()) {
    //      cacheFile.mkdirs();
    //    }

    es.setRestrictMentions(cmd.hasOption("restrictmentions"));
    if (es.isRestrictMentions()) {
      Map<String, Set<Mention>> restrictedMentions = 
          new HashMap<String, Set<Mention>>();
      for (String line : new FileLines(collectionPath + "/mentions_to_evaluate.txt")) {
        String[] data = line.split("\t");
        String docId = data[0];
        String name = data[1];
        Integer charOffset = Integer.parseInt(data[2]);
        Mention m = new Mention();
        m.setMention(name);
        m.setCharOffset(charOffset);
        Set<Mention> docMentions = restrictedMentions.get(docId);
        if (docMentions == null) {
          docMentions = new HashSet<Mention>();
          restrictedMentions.put(docId, docMentions);
        }
        docMentions.add(m);
      }
      es.setRestrictedMentions(restrictedMentions);
    }
    
    es.setRestrictMentionNames(cmd.hasOption("restrictmentionnames"));
    if (es.isRestrictMentionNames()) {
      Set<String> restrictedMentionNames = new HashSet<String>();
      for (String mention : new FileLines(collectionPath + "/mention_names_to_evaluate.txt")) {
        restrictedMentionNames.add(mention);
      }
      es.setRestrictedMentionNames(restrictedMentionNames);
    }

    es.setForceGraphCreation(cmd.hasOption("force"));
    
    es.setShouldGenerateLinkCounts(cmd.hasOption("linkeval"));

    if (cmd.hasOption("e")) {
      es.setNumParallelThreads(Integer.parseInt(cmd.getOptionValue("e")));
    }

    if (cmd.hasOption("r")) {
      es.setShouldTrace(true);
    }

    if (cmd.hasOption("g")) {
      es.setShouldGraphTrace(true);
    }
    
    if (cmd.hasOption("v")) {
      es.setShouldOnlyDoEvaluation(true);
    }
    
    if (cmd.hasOption("ookbepreprocess")) {
      es.setPreviousResults(cmd.getOptionValue("ookbepreprocess"));      
    }
        
    return es;
  }

  private DisambiguationSettings buildDisambiguationSettings(CommandLine cmd) {
    DisambiguationSettings ds = new DisambiguationSettings();

    String similaritySettingsFile = cmd.getOptionValue("s");
    SimilaritySettings similaritySettings = new SimilaritySettings(new File(similaritySettingsFile));
    ds.setSimilaritySettings(similaritySettings);
    
    if (cmd.hasOption("q")) {
      ds.setIncludeNullAsEntityCandidate(true);
      // Update the similaritySettings.
      similaritySettings.setOokbeCountBalance(
          Double.parseDouble(cmd.getOptionValue("q")));
    }

    if (cmd.hasOption("w")) {
      // Update the similaritySettings.
      similaritySettings.setOokbeScoreBalance(
          Double.parseDouble(cmd.getOptionValue("w")));
    }
    
    if (cmd.hasOption("ookbedays")) {
      similaritySettings.setOokbeDaysSpan(
          Integer.parseInt(cmd.getOptionValue("ookbedays")));
    }
    
    if (cmd.hasOption("maxentitykeyphrasecount")) {
      similaritySettings.setMaxEntityKeyphraseCount(
          Integer.parseInt(cmd.getOptionValue("maxentitykeyphrasecount")));
    }
    
    GraphSettings gs = new GraphSettings();
    ds.setGraphSettings(gs);
    if (cmd.hasOption("a")) {
      double alpha = Double.parseDouble(cmd.getOptionValue("a"));

      if (alpha < 0.0 || alpha > 1.0) {
        System.err.println("a is in [0.0,1.0]");
        return null;
      }

      gs.setAlpha(alpha);
    }

    if (cmd.hasOption("c")) {
      double thresh = Double.parseDouble(cmd.getOptionValue("c"));

      gs.setUseCoherenceRobustnessTest(true);
      gs.setCohRobustnessThreshold(thresh);
    } else {
      gs.setUseCoherenceRobustnessTest(false);
    }

    gs.setUseExhaustiveSearch(cmd.hasOption("x"));
    gs.setUseNormalizedObjective(cmd.hasOption("n"));

    String disambiguationAlgorithm = cmd.getOptionValue("l");
    // Default technique is GRAPH.
    TECHNIQUE technique = TECHNIQUE.GRAPH;
    try {
      technique = TECHNIQUE.valueOf(disambiguationAlgorithm);
    } catch (IllegalArgumentException e) {
      // Keep default.
    }
    ds.setDisambiguationTechnique(technique);
    
    if (technique == TECHNIQUE.GRAPH) {
      if (disambiguationAlgorithm.equals("CP")) {
        ds.setDisambiguationAlgorithm(Settings.ALGORITHM.COCKTAIL_PARTY);
      } else if (disambiguationAlgorithm.equals("CPSC")) {
        ds.setDisambiguationAlgorithm(Settings.ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED);
      } else if (disambiguationAlgorithm.equals("RW")) {
        ds.setDisambiguationAlgorithm(Settings.ALGORITHM.RANDOM_WALK);
      }

      // add coherence testing similarity settings
      String cohTestSettingsFile = new File(similaritySettingsFile).getParent() + File.separator + coherence_test_simsetting_name;
      SimilaritySettings cohTestSettings = new SimilaritySettings(new File(cohTestSettingsFile));
      gs.setCoherenceSimilaritySetting(cohTestSettings);
    }
    
    if (cmd.hasOption("r") || cmd.hasOption("g")) {
      ds.setTracingTarget(TracingTarget.STATIC);
    }
    
    gs.setEntitiesPerMentionConstraint(Integer.parseInt(cmd.getOptionValue("i", "3")));
    
    if (cmd.hasOption("o")) {
      ds.setNullMappingThreshold(Double.parseDouble(cmd.getOptionValue("o")));
    }
    
    if (cmd.hasOption("confidence")) {
      ds.setComputeConfidence(true);
    }
    
    if (cmd.hasOption("rerunconfidence")) {
      ds.setComputeReRunConfidence(true);
    }
        
    ConfidenceSettings confSettings = new ConfidenceSettings();
    ds.setConfidenceSettings(confSettings);
    if (cmd.hasOption("graphconfidencebalance")) {
      Float balance = Float.parseFloat(cmd.getOptionValue("graphconfidencebalance"));
      if (balance < 0.0 || balance > 1.0) {
        logger.error("--graphconfidencebalance must be in [0.0, 1.0], is " + balance);
      }
      confSettings.setConfidenceBalance(balance);
    }
    if (cmd.hasOption("graphconfidencementionflippercentage")) {
      Float balance = Float.parseFloat(cmd.getOptionValue("graphconfidencementionflippercentage"));
      if (balance < 0.0 || balance > 1.0) {
        logger.error("--graphconfidencementionflippercentage must be in [0.0, 1.0], is " + balance);
      }
      confSettings.setMentionFlipPercentage(balance);
    }
    if (cmd.hasOption("graphconfidencescore")) {
      SCORE_TYPE scoreType = SCORE_TYPE.valueOf(cmd.getOptionValue("graphconfidencescore"));
      if (scoreType == null) {
        logger.error("--graphconfidencescore must be either LOCAL or WEIGHTED_DEGREE, is " + cmd.getOptionValue("graphconfidencescore"));
      }
      confSettings.setScoreType(scoreType);
    }
    if (cmd.hasOption("combinererunconfidence")) {
      ds.getConfidenceSettings().setCombineReRunConfidence(true);
    }
    
    if (cmd.hasOption("harvestkps")) {
      similaritySettings.setUseHarvestedKeyphrases(true);
    }
    
    if (cmd.hasOption("useookbe")) {
      ds.setUseOokbePlaceholdersInPreviousResults(true);
    }
    
    return ds;
  }

  private Map<String, DisambiguationResults> runDisambiguation(ExperimentalSettings exSettings, DisambiguationSettings disSettings, String resultFilePath) throws InterruptedException {
    CollectionReader cr = exSettings.getCollectionReader();

    if (exSettings.shouldGraphTrace()) {
      GraphTracer.gTracer = new GraphTracer();
    }

    Map<String, DisambiguationResults> prevSolutions = new HashMap<String, DisambiguationResults>();
    Map<String, Map<Integer, ResultMention>> removedMentions = new HashMap<String, Map<Integer,ResultMention>>();
    if (exSettings.getPreviousResults() != null) {
      SolutionReader sr = new SolutionReader(exSettings.getPreviousResults(), exSettings.getCollectionReader().getSettings());
      for (PreparedInput p : sr) {
        Map<ResultMention, List<ResultEntity>> mappings =
            new HashMap<ResultMention, List<ResultEntity>>();
        for (Mention m : p.getMentions().getMentions()) {
          ResultMention rm = new ResultMention(p.getDocId(), m);
          ResultEntity re = new ResultEntity(m.getGroundTruthResult(), 1.0);
          mappings.put(rm, ResultEntity.getResultEntityAsList(re));
        }      
        prevSolutions.put(p.getDocId(), new DisambiguationResults(mappings, ""));
      }       
    }
    
    Map<String, DisambiguationResults> solutions = Collections.synchronizedMap(new HashMap<String, DisambiguationResults>());
    logger.info("Starting disambiguation for " + cr.collectionSize() + 
                " documents with " + exSettings.getNumParallelThreads() + 
                " threads");
    logger.info("Collection stats: " + cr.getCollectionStatistics());
    
    // run Disambiguator
    ExecutorService es = Executors.newFixedThreadPool(exSettings.getNumParallelThreads());

    DocumentCounter dc = new DocumentCounter(cr.collectionSize(), resultFilePath);
    dc.addObserver(this);
    
    for (PreparedInput inputDoc : cr) {
      Tracer tracer = null;
      if (exSettings.shouldTrace()) {
        tracer = new Tracer(getExperimentsStorageDirectory(exSettings),
                           inputDoc.getDocId());
      } else {
        tracer = new NullTracer();
      }
            
      // IF SimilaritySettings, EnsembleMentionEntitySimilarity contain appropriate settings
      //for (Mention mention : inputDoc.getMentions().getMentions())
      //{
      // TODO retrieve probabilities for each type  
      //}
    	  
      if (exSettings.getPreviousResults() != null) {
        PreparedInput ookbeRemovedInput = 
          removeOokbe(
              inputDoc, 
              prevSolutions.get(inputDoc.getDocId()), 
              removedMentions);
        if (ookbeRemovedInput != null) {
          if (disSettings.isUseOokbePlaceholdersInPreviousResults()) {
            Map<Integer, ResultMention> ookbeOffsets = removedMentions.get(inputDoc.getDocId());
            if (ookbeOffsets != null) {
              disSettings.setOokbeOffsets(inputDoc.getDocId(), ookbeOffsets.keySet());
            }
          } else {
            inputDoc = ookbeRemovedInput;
          }
        } else {
          logger.warn("No previous doc found for " + inputDoc.getDocId());
        }
      }
      
      Disambiguator d = new Disambiguator(inputDoc, disSettings, solutions, tracer, dc);
      es.execute(d);
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.DAYS);
    
    if (exSettings.getPreviousResults() != null) {
      for (Entry<String, DisambiguationResults> e : prevSolutions.entrySet()) {
        String docId = e.getKey();
        DisambiguationResults current = solutions.get(docId);
//        logger.info("Merging results for " + docId);
//        logger.info("CURRENT: " + current);
        Map<Integer, ResultMention> offset2rm = new HashMap<Integer, ResultMention>();
        for (ResultMention rm : current.getResultMentions()) {
          offset2rm.put(rm.getCharacterOffset(), rm);
        }
//        logger.info("ADDING: " + removedMentions.get(docId));
        for (ResultMention rm : removedMentions.get(docId).values()) {
          current.setResultEntities(rm, ResultEntity.getResultEntityAsList(ResultEntity.getNoMatchingEntity()));
        }
        solutions.put(docId, current);
      }      
    }
    
    return solutions;
  }

  private PreparedInput removeOokbe(PreparedInput p,
      DisambiguationResults disambiguationResults, Map<String, Map<Integer, ResultMention>> docRemovedMentions) {
    if (disambiguationResults == null) {
      return p;
    }
    Set<Integer> nilMentionOffsets = new HashSet<Integer>();
    Map<Integer, ResultMention> removedMentions = new HashMap<Integer, ResultMention>();
    docRemovedMentions.put(p.getDocId(), removedMentions);
    for (ResultMention rm : disambiguationResults.getResultMentions()) {
      if (disambiguationResults.getBestEntity(rm).isNoMatchingEntity()) {
        nilMentionOffsets.add(rm.getCharacterOffset());
      }
    }
    Mentions cleanedMentions = new Mentions();
    PreparedInput cleanedInput = new PreparedInput(p.getDocId());
    cleanedInput.setMentions(cleanedMentions);
    cleanedInput.setTimestamp(p.getTimestamp());
    cleanedInput.setTokens(p.getTokens());
    
    for (Mention m : p.getMentions().getMentions()) {
      if (!nilMentionOffsets.contains(m.getCharOffset())) {
        cleanedMentions.addMention(m);
      } else {
        removedMentions.put(m.getCharOffset(), new ResultMention(p.getDocId(), m));
      }
    }
    return p;
  }
  
  private synchronized void writeSolutions(ExperimentalSettings es, String resultsFilePath, Map<String, DisambiguationResults> solutions) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFilePath), "UTF-8"));

    for (String docId : solutions.keySet()) {
      DisambiguationResults docSolutions = solutions.get(docId);

      for (ResultMention resultMention : docSolutions.getResultMentions()) {
        String mentionString = resultMention.getMention();

        // Skip names that should not be evaluated.
        if (es.isRestrictMentionNames() && 
            !es.getRestrictedMentionNames().contains(mentionString)) {
          continue;
        }
        
        // Skip mentions that should not be evaluated.
        if (es.isRestrictMentions()) {
          Set<Mention> restrictedMentions = 
              es.getRestrictedMentions().get(docId);
          if (restrictedMentions != null) {
            for (Mention m : restrictedMentions) {
              boolean contains = false;
              if (m.getMention().equals(resultMention.getMention()) &&
                  m.getCharOffset() == resultMention.getCharacterOffset()) {
                contains = true;
                break;
              }
              if (!contains) {
                continue;
              }
            }
          }
        }

        int charOffset = resultMention.getCharacterOffset();
        int charLength = resultMention.getCharacterLength();
        
        ResultEntity resultEntity = docSolutions.getBestEntity(resultMention);
        double confidence = 0.0;
        String entityString = null;
        if (resultEntity != null) {
          confidence = resultEntity.getDisambiguationScore();
          entityString = resultEntity.getEntity();
        }

        writer.write(docId + "\t" + charOffset + "\t" + charLength + "\t" + mentionString + "\t" + entityString + "\t" + confidence + "\n");
      }
    }
    writer.flush();
    writer.close();
  }

  private void evaluateAllResults(ExperimentalSettings exSettings, String resultFilePath) throws IOException, SQLException {
    // do eval for all results
    File resultFileDir = new File(getResultFileDirectory(exSettings));

    FileFilter fileFilter = new FileFilter() {

      public boolean accept(File file) {
        return file.isFile() && !file.getName().startsWith(".");
      }
    };
    File[] resultFiles = resultFileDir.listFiles(fileFilter);
    for (File resultFile : resultFiles) {
      evaluateResult(exSettings, resultFile.getAbsolutePath());
    }
  }

  private void evaluateResult(ExperimentalSettings exSettings, String resultFile) throws IOException, SQLException {
    CollectionReader groundTruhReader = exSettings.getCollectionReader();
    SolutionReader solutionReader = new SolutionReader(resultFile, groundTruhReader.getSettings());

    Evaluator eval = new Evaluator(groundTruhReader, solutionReader, exSettings);
    Map<Integer, Double> precAtRec = eval.evaluatePrecisionAndRecall(1);
    Map<String, Map<Integer, Double>> docPrecAtRec = eval.evaluateDocumentPrecisionAndRecall(1);    
    Map<String, Pair<Double, Integer>> docPrecAt95conf = 
        eval.evaluateDocumentPrecisionAtConfidence(0.95);
    Map<String, Pair<Double, Integer>> docPrecAt80conf = 
        eval.evaluateDocumentPrecisionAtConfidence(0.8);
    Map<String, Double> docMAP = eval.evaluateDocMAP(1);
    List<Pair<Mention, Boolean>> correct = eval.evaluateCorrect();    
    Map<String, Pair<Double, Double>> mentionPrecRec = eval.evaluateEntityRecognition();
    double map = eval.evaluateMAP(1);
    Pair<Double, Integer> precAt95conf = eval.evaluatePrecisionAtConfidence(0.95);
    Pair<Double, Integer> precAt80conf = eval.evaluatePrecisionAtConfidence(0.8);

    // Do NME evaluation if NME mentions are included in the ground truth.
    Map<String, Pair<Double, Double>> nmePrecRec = null;
    Map<String, Pair<Double, Double>> inKbPrecRec = null;
    if (exSettings.shouldIncludeNMEMentions()) {
      nmePrecRec = eval.evaluateNMEPrecRec();
      inKbPrecRec = eval.evaluateInKbPrecRec();
    }
    
    //      List<EvaluationRecord> evaluation = new Evaluator(groundTruhReader, solutionReader).evaluatePrecisionAtThreshold(0.5);
    writeEvaluationFile(
        exSettings, new File(resultFile).getName(), precAtRec, docPrecAtRec, 
        docMAP, map, docPrecAt95conf, precAt95conf, docPrecAt80conf, precAt80conf,
        nmePrecRec, inKbPrecRec, mentionPrecRec, 100);
//    writeEvaluationFile(exSettings, new File(resultFile).getName(), precAtRec, docPrecAtRec, docMAP, map, nmePrecRec, mentionPrecRec, 50);
    writeEvaluationCorrectFile(exSettings, new File(resultFile).getName(), correct);
        
    if (exSettings.shouldGenerateLinkCounts()) {
      Map<Mention, Pair<Boolean, Integer>> correctWithLinkCount = eval.getMentionLinkCountAndCorrectness();
      writeAveragePrecForLinkCount(exSettings, new File(resultFile).getName(), correctWithLinkCount);
    }
  }

  private void writeEvaluationCorrectFile(ExperimentalSettings es, 
      String fileName, List<Pair<Mention, Boolean>> correct) throws IOException {
    File evalDir = new File(getExperimentsStorageDirectory(es) +
                   File.separator + "eval");
    File outFile = new File(evalDir.getAbsolutePath() + File.separator + fileName + ".correct");
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
    
    Collections.sort(correct, new Comparator<Pair<Mention, Boolean>>() {

      @Override
      public int compare(Pair<Mention, Boolean> arg0, Pair<Mention, Boolean> arg1) {
        return arg0.first.getMention().compareTo(arg1.first.getMention());
      }
    });
    
    for (Pair<Mention, Boolean> m : correct) {
      int c = m.second ? 1 : 0;
      writer.write(m.first+"\t"+c);
      writer.newLine();
    }
    
    writer.flush();
    writer.close();
  }

  private void writeAveragePrecForLinkCount(ExperimentalSettings es, String name, Map<Mention, Pair<Boolean, Integer>> correctWithLinkCount) throws IOException {
    File outFileCounts = new File(new File(getExperimentsStorageDirectory(es)+"/eval"), name+".linkcounts");
    BufferedWriter writerCounts = FileUtils.getBufferedUTF8Writer(outFileCounts);

    File outFilePrec = new File(new File(getExperimentsStorageDirectory(es)+"/eval"), name+".linkprec");
    BufferedWriter writerPrec = FileUtils.getBufferedUTF8Writer(outFilePrec);
    
    File outFileMacroPrec = new File(new File(getExperimentsStorageDirectory(es)+"/eval"), name+".linkmacroprec");
    BufferedWriter writerMacroPrec = FileUtils.getBufferedUTF8Writer(outFileMacroPrec);
        
    Map<Integer, Integer> linkCountCorrect = new HashMap<Integer, Integer>();
    Map<Integer, Integer> linkCountTotal = new HashMap<Integer, Integer>();
        
    for (Pair<Boolean, Integer> data : correctWithLinkCount.values()) {
      Integer currentCorrect = linkCountCorrect.get(data.second);
      Integer currentTotal = linkCountTotal.get(data.second);
      
      if (currentCorrect == null) { currentCorrect = 0; }
      if (currentTotal == null) { currentTotal = 0; }
           
      linkCountTotal.put(data.second, currentTotal+1);
      
      if (data.first) {
        linkCountCorrect.put(data.second, currentCorrect+1);
      }
    }
        
    TIntIntHashMap belowCountCorrect = new TIntIntHashMap();
    TIntIntHashMap belowCountTotal = new TIntIntHashMap();
    
    int allCountCorrect  = 0;
    int allCountTotal = 0;
    
    double allPrec = 0.0;
    
    TreeSet<Integer> keys = new TreeSet<Integer>(linkCountTotal.keySet());
    int max = keys.last();
    
    writerCounts.write("linknum\tmentionnum\tavgprec");
    writerCounts.newLine();
    for (Integer k : keys) {
      int total = linkCountTotal.get(k);
      
      Integer correct = linkCountCorrect.get(k);
      if (correct == null) {
        correct = 0;
      }
            
      for (int i=k;i<=max;i++) {
        belowCountCorrect.adjustOrPutValue(i, correct, correct);
        belowCountTotal.adjustOrPutValue(i, total, total);
      }
      
      allCountCorrect += correct;
      allCountTotal += total;
            
      double prec = (double) correct / (double) total;
      allPrec += prec; 
      // number_of_links<tab>total_number_mentions_with_count<tab>average_mention_precision
      writerCounts.write(k+"\t"+total+"\t"+prec);
      writerCounts.newLine();
    }
    
    double linkMacroAvgPrec = allPrec/(double)keys.size();
    writerMacroPrec.write(String.valueOf(linkMacroAvgPrec));
    writerMacroPrec.flush();
    writerMacroPrec.close();
        
    writerPrec.write("cutoff\tnumber_of_mentions_below\tprec_below\tnumber_of_mentions_above\tprec_above");
    writerPrec.newLine();
    for (int i=0;i<=max;i++) {
      double precBelow = 0;
      if (belowCountTotal.get(i) > 0) { 
        precBelow = (double) belowCountCorrect.get(i) / (double) belowCountTotal.get(i);
      }
      
      double precAbove = 0;
      int aboveCountTotal = (allCountTotal - belowCountTotal.get(i)); 
      if (aboveCountTotal > 0) {
        precAbove = (double) (allCountCorrect - belowCountCorrect.get(i)) / (double) (allCountTotal - belowCountTotal.get(i));
      }
      
      writerPrec.write(i+"\t"+belowCountTotal.get(i)+"\t"+precBelow+"\t"+aboveCountTotal+"\t"+precAbove);
      writerPrec.newLine();
    }
    writerPrec.flush();
    writerPrec.close();
    
    writerCounts.flush();
    writerCounts.close();
  }

  private void writeTracing(String resultFileName, Map<String, DisambiguationResults> solutions) throws InterruptedException {
    System.out.print("Writing SimilarityTracer output... ");
    // write tracer output
    for (String docId : solutions.keySet()) {
      Tracer tracer = solutions.get(docId).getTracer();
      tracer.writeOutput(resultFileName, false, false);
    }
    System.out.println("done");
  }

  private void writeGraphTracing(Map<String, DisambiguationResults> results, String resultFileDirectory) throws FileNotFoundException, IOException {
    System.out.print("Writing Graph Tracer output... ");

    File dir = new File(resultFileDirectory);

    if (!dir.exists()) {
      dir.mkdirs();
    }

    for (Entry<String, DisambiguationResults> e : results.entrySet()) {
      File outFile = new File(resultFileDirectory + File.separator + e.getKey() + "_graphtrace.html");

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));

      String html = e.getValue().getgTracerHtml();

      if (html != null) {
        writer.write(html);
        writer.flush();
        writer.close();
      }
    }

    System.out.println("done");
  }

  public void writeEvaluationFile(
      ExperimentalSettings es, String fileName, Map<Integer, Double> precAtRec, 
      Map<String, Map<Integer, Double>> docPrecAtRec, Map<String, Double> docMAP, 
      double map, Map<String, Pair<Double, Integer>> docPrecAt95conf, Pair<Double, Integer> precAt95conf, 
      Map<String, Pair<Double, Integer>> docPrecAt80conf, Pair<Double, Integer> precAt80conf, 
      Map<String, Pair<Double, Double>> nmePrecRec, 
      Map<String, Pair<Double, Double>> inKbPrecRec,
      Map<String, Pair<Double, Double>> nerPrecRec, int recallLevel)
      throws IOException {
    String fullPath = getExperimentsStorageDirectory(es) + File.separator;
    if (recallLevel == 100) {
      fullPath += "eval" + File.separator;
    } else {
      fullPath += "eval-" + recallLevel + File.separator;
    }
    File evalDir = new File(fullPath);
    if (!evalDir.exists()) {
      evalDir.mkdirs();
    }
    
    if (docPrecAtRec.size() == 0) {
      // Nothing to do.
      return;
    }

    File outFile = new File(evalDir.getAbsolutePath() + File.separator + fileName);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));

    writer.write("DocId\tPrec.\tRec.\tPrec@0.05rec\tPrec@0.1rec"
                + "\tPrec@0.95conf\t#Men@0.95conf"
                + "\tPrec@0.80conf\t#Men@0.80conf"
                + "\tMAP\tnerPrec\tnerRec\tnerF1");
    if (exSettings.shouldIncludeNMEMentions()) {
      writer.write("\tnmePrec\tnmeRec\tnmeF1");
      writer.write("\tinKbPrec\tinKbRec\tinKbF1");
    }    
    writer.newLine();

    List<String> docIds = new LinkedList<String>(docPrecAtRec.keySet());
    Collections.sort(docIds);

    Map<Integer, Double> totalPrecAtRec = new HashMap<Integer, Double>();
    double totalNmePrec = 0;
    double totalNmeRec = 0;
    double totalNmeF1 = 0;
    int totalNmeCounted = 0;
    
    double totalInKbPrec = 0;
    double totalInKbRec = 0;
    double totalInKbF1 = 0;
    int totalInKbCounted = 0;

    double totalNerPrec = 0;
    double totalNerRec = 0;
    double totalNerF1 = 0;
    int totalNerCounted = 0;
    
    for (String docId : docIds) {
      Map<Integer, Double> par = docPrecAtRec.get(docId);      
      double docPrec = par.get(recallLevel);
      
      // Write per-doc evaluations.
      writer.write(
          docId + "\t" + docPrec + "\t" + recallLevel / 100.0 + "\t" + 
              par.get(5) + "\t" + par.get(10) + "\t" +
              docPrecAt95conf.get(docId).first + "\t" + 
              docPrecAt95conf.get(docId).second + "\t" +
              docPrecAt80conf.get(docId).first + "\t" +
              docPrecAt80conf.get(docId).second + "\t" +
              docMAP.get(docId));
      
      // Evaluate disambiguation precision.
      for (Integer rec : par.keySet()) {
        if (!totalPrecAtRec.containsKey(rec)) {
          totalPrecAtRec.put(rec, par.get(rec));
        } else {
          double p = totalPrecAtRec.get(rec);
          p += par.get(rec);
          totalPrecAtRec.put(rec, p);
        }
      }
     
      // Evaluate named entity recognition.
      Double nerPrec = nerPrecRec.get(docId).first;
      if (nerPrec < 0.0) { nerPrec = 0.0; }
      totalNerPrec += nerPrec;
      Double nerRec = nerPrecRec.get(docId).second;
      if (nerRec < 0.0) { nerRec = 0.0; }
      totalNerRec += nerRec;
      double denominator = nerPrec + nerRec;
      double nerF1 = 0.0; 
      if (denominator > 0.0) {
        nerF1 = 2 * nerPrec * nerRec / (nerPrec + nerRec);
      }
      totalNerF1 += nerF1;
      writer.write( "\t" +  nerPrec + "\t" + nerRec + "\t" + nerF1);
      totalNerCounted++;
      
      // Evaluate out-of-knowledge base recognition.
      if (exSettings.shouldIncludeNMEMentions()) {
        Double nmePrec = nmePrecRec.get(docId).first;
        if (nmePrec < 0.0) { nmePrec = 0.0; }
        totalNmePrec += nmePrec;
        Double nmeRec = nmePrecRec.get(docId).second;
        if (nmeRec < 0.0) { nmeRec = 0.0; }
        totalNmeRec += nmeRec;
        denominator = nmePrec + nmeRec;
        double nmeF1 = 0.0; 
        if (denominator > 0.0) {
          nmeF1 = 2 * nmePrec * nmeRec / (nmePrec + nmeRec);
        }
        totalNmeF1 += nmeF1;
        writer.write( "\t" +  nmePrec + "\t" + nmeRec + "\t" + nmeF1);
        totalNmeCounted++;
        
        Double inKbPrec = inKbPrecRec.get(docId).first;
        if (inKbPrec < 0.0) { inKbPrec = 0.0; }
        totalInKbPrec += inKbPrec;
        Double inKbRec = inKbPrecRec.get(docId).second;
        if (inKbRec < 0.0) { inKbRec = 0.0; }
        totalInKbRec += inKbRec;
        denominator = inKbPrec + inKbRec;
        double inKbF1 = 0.0; 
        if (denominator > 0.0) {
          inKbF1 = 2 * inKbPrec * inKbRec / (inKbPrec + inKbRec);
        }
        totalInKbF1 += inKbF1;
        writer.write( "\t" +  inKbPrec + "\t" + inKbRec + "\t" + inKbF1);
        totalInKbCounted++;
      }
      writer.newLine();
    }

    writer.write("==================================");
    writer.newLine();

    double daPrec = totalPrecAtRec.get(recallLevel) / docIds.size();
    writer.write("Document-Average Precision = " + daPrec);
    writer.newLine();

    writer.write("Mention-Average Precision = " + precAtRec.get(recallLevel));
    writer.newLine();
    
    writer.write("Document-Average Recall = " + recallLevel / 100.0);
    writer.newLine();

    writer.write("Mention-Average Recall = " + recallLevel / 100.0);
    writer.newLine();
    
    writer.write("Mention-Average Precision@95%conf = " + precAt95conf.first);
    writer.newLine();
    
    writer.write("Number of Mentions@95%conf = " + precAt95conf.second);
    writer.newLine();
    
    writer.write("Mention-Average Precision@80%conf = " + precAt80conf.first);
    writer.newLine();
    
    writer.write("Number of Mentions@80%conf = " + precAt80conf.second);
    writer.newLine();
    
    double daPrecAt5 = totalPrecAtRec.get(5) / docIds.size();
    writer.write("Document-Average Precision@5%rec = " + daPrecAt5);
    writer.newLine();
    
    double daPrecAt10 = totalPrecAtRec.get(10) / docIds.size();
    writer.write("Document-Average Precision@10%rec = " + daPrecAt10);
    writer.newLine();
    
    writer.write("Document-Average NER Precision = " + totalNerPrec / totalNerCounted);
    writer.newLine();
  
    writer.write("Document-Average NER Recall = " + totalNerRec / totalNerCounted);
    writer.newLine();
    
    writer.write("Document-Average NER F1 = " + totalNerF1 / totalNerCounted);
    writer.newLine();
    
    if (exSettings.shouldIncludeNMEMentions()) {
      writer.write("Document-Average NME Precision = " + totalNmePrec / totalNmeCounted);
      writer.newLine();
    
      writer.write("Document-Average NME Recall = " + totalNmeRec / totalNmeCounted);
      writer.newLine();
      
      writer.write("Document-Average NME F1 = " + totalNmeF1 / totalNmeCounted);
      writer.newLine();
      
      writer.write("Document-Average INKB Precision = " + totalInKbPrec / totalInKbCounted);
      writer.newLine();
    
      writer.write("Document-Average INKB Recall = " + totalInKbRec / totalInKbCounted);
      writer.newLine();
      
      writer.write("Document-Average INKB F1 = " + totalInKbF1 / totalInKbCounted);
      writer.newLine();
    }

    writer.write("MAP = " + map);

    writer.flush();
    writer.close();

    outFile = new File(evalDir.getAbsolutePath() + File.separator + fileName + ".precrec");
    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));

    for (String docId : docIds) {
      Map<Integer, Double> par = docPrecAtRec.get(docId);

      writer.write(docId);
      writer.newLine();

      for (Integer rec : par.keySet()) {
        writer.write(rec + "\t" + par.get(rec));
        writer.newLine();
      }

      writer.newLine();
    }

    writer.write("AVERAGE");
    writer.newLine();

    List<Integer> recs = new ArrayList<Integer>(totalPrecAtRec.keySet());
    Collections.sort(recs);

    for (Integer rec : recs) {
      double avgPrec = totalPrecAtRec.get(rec) / docIds.size();
      double recPercent = rec / 100.0;
      writer.write(recPercent + "\t" + avgPrec);
      writer.newLine();
    }

    writer.flush();
    writer.close();
  }
  
  private String getExperimentsStorageDirectory(ExperimentalSettings es) {
    return es.getCollectionReader().collectionPath + File.separator + "runs";
  }

  private String getResultFileDirectory(ExperimentalSettings es) {
    return getExperimentsStorageDirectory(es) + File.separator + "results" + File.separator;
  }

  private String getResultFilePath(ExperimentalSettings es, DisambiguationSettings ds) {
    File resultsDir = new File(getResultFileDirectory(es));
    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }
    return resultsDir + File.separator + getResultFileName(es, ds);
  }

  private String getResultFileName(ExperimentalSettings es, DisambiguationSettings ds) {  
    if (es.getPreviousResults() != null) {
      String[] parts = es.getPreviousResults().split("/");
      String add = "__eepreproc";
      if (ds.isUseOokbePlaceholdersInPreviousResults()) {
        add += "_--useee";
      }
      String name = parts[parts.length - 1].replace(".tsv", add + ".tsv");
      return name;
    }
    String name = "";
    CollectionReader cr = es.getCollectionReader();
  
    String DATE_FORMAT = "yyyyMMdd-HHmm";
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Calendar c1 = Calendar.getInstance(); // today
    name = sdf.format(c1.getTime()) + "_-s_" + ds.getSimilaritySettings().getIdentifier();

    if (es.isRestrictMentions()) {
      name += "_restrictmentions";
    }
    
    if (es.isRestrictMentionNames()) {
      name += "_restrictmentionnames";
    }
    
    if (es.getCollectionReader().getSettings().getMinMentionOccurrence() > 0) {
      name += "_minmencount_" + 
          es.getCollectionReader().getSettings().getMinMentionOccurrence();
    }
    
    if (es.getCollectionReader().getSettings().getMentionOccurrenceDaySpan() > 0) {
      name += "_mendayspan_" + 
          es.getCollectionReader().getSettings().getMentionOccurrenceDaySpan();
    }
    
    if (es.shouldIncludeNMEMentions()) {
      name += "_nme";
    }
    
    if (ds.isIncludeNullAsEntityCandidate()) {
      name += "_ookbe";
      name += "-" + ds.getSimilaritySettings().getOokbeCountBalance();
      name += "_-w_" + ds.getSimilaritySettings().getOokbeScoreBalance();
      name += "_-days_" + ds.getSimilaritySettings().getOokbeDaysSpan();
    }
    
    if (ds.getNullMappingThreshold() >= 0.0) {
      name += "_-o_" + ds.getNullMappingThreshold();
    }
    
    if (ds.shouldComputeConfidence()) {
      name += "_--confidence";
      if (ds.getDisambiguationTechnique().equals(TECHNIQUE.GRAPH)) {
        name += "_" + ds.getConfidenceSettings().getConfidenceBalance();          
      }
      if (ds.getDisambiguationTechnique().equals(TECHNIQUE.GRAPH)) {
        name += "_-menFlip_" + ds.getConfidenceSettings().getMentionFlipPercentage();          
      }    
      if (ds.getDisambiguationTechnique().equals(TECHNIQUE.GRAPH)) {
        name += "_-scoreType_" + ds.getConfidenceSettings().getScoreType();
      }
    }
    
    if (ds.getSimilaritySettings().isUseHarvestedKeyphrases()) {
      name += "_--harvestkps";
    }
    
    if (ds.isComputeReRunConfidence()) {
      name += "_--rerunconfidence";
    }
    
    switch (ds.getDisambiguationTechnique()) {
      case ORACLE:
        name += "_-l_ORACLE";
        break;
      case TAGME:
        name += "_-l_TAGME";
        break;
      case IW:
        name += "_-l_IW";
        break;
      case SL:
        name += "_-l_SL";
        break;
      case AIDALIGHT:
        name += "_-l_AIDALIGHT";
        break;
      case LOCAL:
        name += "_-l_" + disambiguationTechniqueAsString(ds.getDisambiguationTechnique());
        break;
      case GRAPH:
        name += "_-l_" + disambiguationAlgorithmAsString(ds.getDisambiguationAlgorithm());
        GraphSettings gs = ds.getGraphSettings();
        name += "_-a_" + gs.getAlpha();
        
        if (gs.shouldUseExhaustiveSearch()) {
          name += "_-x";
        }

        if (ds.getDisambiguationAlgorithm().equals(Settings.ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED)) {
          name += "_-size_" + gs.getEntitiesPerMentionConstraint();
        }

        if (gs.shouldUseNormalizedObjective()) {
          name += "_normObj";
        }

        if (gs.shouldUseCoherenceRobustnessTest()) {
          name += "_c" + gs.getCohRobustnessThreshold();
        }

        break;

      default:
        break;
    }

    if (cr.from != 0 && cr.to != Integer.MAX_VALUE) {
      name += "_" + cr.from + "-" + cr.to;
    }

    name += "_results.tsv";
    return name;
  }

  private String disambiguationTechniqueAsString(Settings.TECHNIQUE t) {
    return t.toString();
  }

  private String disambiguationAlgorithmAsString(Settings.ALGORITHM a) {
    String algorithm = "";

    switch (a) {
      case COCKTAIL_PARTY:
        algorithm = "CP";
        break;
      case COCKTAIL_PARTY_SIZE_CONSTRAINED:
        algorithm = "CPSC";
        break;
      default:
        logger.error("No Algorithm for '" + a + "'");
        break;
    }

    return algorithm;
  }

  private void fillEntityBlacklist(File blacklistFile) throws IOException {
    for (String line : new FileLines(blacklistFile)) {;
      AidaManager.entityBlacklist.add(AidaManager.getEntity(line));
    }
  }

  @Override
  public void update(Observable o, Object arg) {
    if (o instanceof DocumentCounter) {
      DocumentCounter dc = (DocumentCounter) o;
      try {
        synchronized (this) {
          writeSolutions(exSettings, dc.getResultFilePath(), dc.getResultsMap()); 
        }
      } catch (IOException e) {
        logger.warn("Could not write intermediate results");
        e.printStackTrace();
      }
    }    
  }
}