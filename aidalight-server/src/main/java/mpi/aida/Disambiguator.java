package mpi.aida;

import gnu.trove.set.hash.TIntHashSet;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.Entities;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.disambiguationtechnique.AIDALightDisambiguation;
import mpi.aida.disambiguationtechnique.DBpediaSpotlightDisambiguation;
import mpi.aida.disambiguationtechnique.IllinoisWikifierDisambiguation;
import mpi.aida.disambiguationtechnique.LocalDisambiguation;
import mpi.aida.disambiguationtechnique.OracleDisambiguation;
import mpi.aida.disambiguationtechnique.TagMeDisambiguation;
import mpi.aida.graph.Graph;
import mpi.aida.graph.Graph.Edge;
import mpi.aida.graph.GraphGenerator;
import mpi.aida.graph.algorithms.CocktailParty;
import mpi.aida.graph.algorithms.CocktailPartySizeConstrained;
import mpi.aida.graph.algorithms.DisambiguationAlgorithm;
import mpi.aida.graph.algorithms.GraphConfidenceByRerunEstimator;
import mpi.aida.graph.algorithms.GraphConfidenceByRerunEstimator.ConfidenceResult;
import mpi.aida.util.CollectionUtils;
import mpi.aida.util.DocumentCounter;
import mpi.aida.util.RunningTimer;
import mpi.experiment.trace.GraphTracer;
import mpi.experiment.trace.GraphTracer.TracingTarget;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for running the disambiguation. Is thread-safe and can be
 * run in parallel.
 * 
 */
public class Disambiguator implements Runnable {
  private static final Logger logger = 
      LoggerFactory.getLogger(Disambiguator.class);
  
  private PreparedInput input;

  private DisambiguationSettings settings;

  private Tracer tracer;

  private DocumentCounter documentCounter;
  
  private Map<String, DisambiguationResults> resultsMap;
  
  private NumberFormat nf;
  
  /** 
   * Common init.
   */
  private void init(PreparedInput input, DisambiguationSettings settings,
                  Tracer tracer) {
    this.input = input;
    this.settings = settings;
    this.tracer = tracer;
    nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
    nf.setMaximumFractionDigits(2);
  }

  /**
   * tracer is set to NullTracer();
   * @param input
   * @param settings
   */
  public Disambiguator(PreparedInput input, DisambiguationSettings settings) {
    init(input, settings, new NullTracer());
  }

  public Disambiguator(PreparedInput input, DisambiguationSettings settings, Tracer tracer) {
    init(input, settings, tracer);
  }

  /**
   * Use this constructor when running Disambiguator in parallel - results will be stored in the resultsMap.
   * The key is input.getDocId();
   * 
   * @param input
   * @param settings
   * @param resultsMap
   */
  public Disambiguator(PreparedInput input, DisambiguationSettings settings, 
                      Map<String, DisambiguationResults> resultsMap, 
                      Tracer tracer, DocumentCounter dc) {
    init(input, settings, tracer);
    this.resultsMap = resultsMap;
    this.documentCounter = dc;
    dc.setResultsMap(resultsMap);
  }

  public DisambiguationResults disambiguate() throws Exception {
    Integer timerId = RunningTimer.start("Disambiguator");

    logger.info("Disambiguating '" + input.getDocId() + "' with " + 
        input.getMentions().getMentions().size() + " mentions.");    
    Map<ResultMention, List<ResultEntity>> mentionMappings = null;

    switch (settings.getDisambiguationTechnique()) {
      case ORACLE:
        mentionMappings = runOracleDisambiguation(input);
        break;
      case LOCAL:
        mentionMappings = runLocalDisambiguation(input, settings, tracer);
        break;
//      case LOCAL_ITERATIVE:
//        mentionMappings = runLocalDisambiguationIterative(input, settings, tracer);
//        break;
      case GRAPH:
        mentionMappings = runGraphDisambiguation(input, settings);
        break;
//      case CHAKRABARTI:
//        mentionMappings = runChakrabartiDisambiguation(input, settings);
      case TAGME:
        mentionMappings = runTagMeDisambiguation(input, settings);
        break;
      case SL:
        mentionMappings = runSpotLightDisambiguation(input, settings);
        break;
      case AIDALIGHT:
        mentionMappings = runAIDALightDisambiguation(input, settings);
        break;
      case IW:
        mentionMappings = runIllinoisWikifierDisambiguation(input, settings);
        break;
      default:
        break;
    }
    RunningTimer.end("Disambiguator", timerId);   
    
    // do the tracing
    String tracerHtml = null;  //tracer.getHtmlOutput();
    TracingTarget target = settings.getTracingTarget();

    if (GraphTracer.gTracer.canGenerateHtmlFor(input.getDocId())) {
      tracerHtml = GraphTracer.gTracer.generateHtml(input.getDocId(), target);
      GraphTracer.gTracer.removeDocId(input.getDocId());
    } else if (GraphTracer.gTracer.canGenerateHtmlFor(Integer.toString(input.getDocId().hashCode()))) {
      tracerHtml = GraphTracer.gTracer.generateHtml(Integer.toString(input.getDocId().hashCode()), target);
      GraphTracer.gTracer.removeDocId(Integer.toString(input.getDocId().hashCode()));
    }
    
    DisambiguationResults disambiguationResults = new DisambiguationResults(mentionMappings, tracerHtml);

    if (settings.getNullMappingThreshold() >= 0.0) {
      // calc threshold
//      double threshold = settings.getSimilaritySettings().getNormalizedAverageScore() * settings.getNullMappingThresholdFactor();
      double threshold = settings.getNullMappingThreshold();
      logger.info(
          "Dropping all entities below the score threshold of " + threshold);

      // drop anything below the threshold                  
      for (ResultMention rm : disambiguationResults.getResultMentions()) {
        double score = disambiguationResults.getBestEntity(rm).getDisambiguationScore();

        if (score < threshold) {
          List<ResultEntity> nme = new ArrayList<ResultEntity>(1);
          nme.add(ResultEntity.getNoMatchingEntity());
          disambiguationResults.setResultEntities(rm, nme);
        }
      }
    }

    return disambiguationResults;
  }

//  private Map<ResultMention, List<ResultEntity>> runChakrabartiDisambiguation(PreparedInput content, DisambiguationSettings settings) throws Exception {
//    ChakrabartiTechnique chakra = new ChakrabartiTechnique(content, settings.getSimilaritySettings(), 0.0, typeClassifier, tracer);
//    Map<ResultMention, List<ResultEntity>> results = chakra.disambiguate();
//    return results;
//  }

  private Map<ResultMention, List<ResultEntity>> runGraphDisambiguation(
      PreparedInput content, DisambiguationSettings settings) throws Exception {
    long beginTime = System.currentTimeMillis();

    GraphGenerator gg = new GraphGenerator(content, settings, tracer);
    Graph gData = gg.run();
    
    // Below is for confidence.
    Graph gCopy = null;
    boolean runMultipleRounds = false;
    if (settings.isComputeReRunConfidence()) {
      runMultipleRounds = true;
      gCopy = 
          gData.copyRemovingNodesAndEdges(
              new TIntHashSet(), new HashSet<Edge>());
    }

    // Run the algorithm.
    DisambiguationAlgorithm da = null;
    switch (settings.getDisambiguationAlgorithm()) {
      case COCKTAIL_PARTY:
        da = new CocktailParty(gData, settings.getGraphSettings(),
            settings.shouldComputeConfidence(),
            settings.getConfidenceSettings());
        break;
      case COCKTAIL_PARTY_SIZE_CONSTRAINED:
        da = new CocktailPartySizeConstrained(gData, 
            settings.getGraphSettings(),
            settings.shouldComputeConfidence(),
            settings.getConfidenceSettings());
        break;
//      case RANDOM_WALK:
//        da = new RandomWalk(gData, 0.15, 10000);
      default:
        break;
    }

    logger.info("Disambiguating '" + content.getDocId() + "' (" + content.getMentions().getMentions().size() + " mentions)");
    Map<ResultMention, List<ResultEntity>> results = da.disambiguate();

    // Replace name-OOKBE placeholders by plain OOKBE placeholders. 
    if (settings.isIncludeNullAsEntityCandidate()) {
      Map<ResultMention, List<ResultEntity>> nmeCleanedResults = new HashMap<ResultMention, List<ResultEntity>>();

      for (Entry<ResultMention, List<ResultEntity>> e : results.entrySet()) {
        if (Entities.isOokbeName(e.getValue().get(0).getEntity())) {
          List<ResultEntity> nme = new ArrayList<ResultEntity>(1);
          nme.add(ResultEntity.getNoMatchingEntity());
          nmeCleanedResults.put(e.getKey(), nme);
        } else {
          nmeCleanedResults.put(e.getKey(), e.getValue());
        }
      }
      results = nmeCleanedResults;
    }
    
    if (runMultipleRounds) {
      final int rounds = 500;
      ConfidenceResult confidence = 
          new GraphConfidenceByRerunEstimator().run(gCopy, rounds, 0.4, settings, results);
      
      // TODO jhoffart IMPORTANT: only the first ResultEntity is modified, if it works, adjust all.
      for (ResultMention rm : results.keySet()) {
        // Combine scores of graph algo and sampling half and half.
        double samplingConf = confidence.getConfidence(rm);
        double graphConf = results.get(rm).get(0).getDisambiguationScore();
        double finalConf = 0.434 * samplingConf + 0.566 * graphConf;
        
        if (settings.getConfidenceSettings().isCombineReRunConfidence()) {
          results.get(rm).get(0).setDisambiguationScore(finalConf);
        } else {
          results.get(rm).get(0).setDisambiguationScore(samplingConf);
        }
      }
    }    

    double runTime = (System.currentTimeMillis() - beginTime) / (double) 1000;
    logger.info("Document '" + content.getDocId() + "' done in " + 
                nf.format(runTime) + "s");
    return results;
  }

  private Map<ResultMention, List<ResultEntity>> runLocalDisambiguation(PreparedInput content, DisambiguationSettings settings, Tracer tracer) throws SQLException {
    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
    LocalDisambiguation ld = 
        new LocalDisambiguation(
            content, settings.getSimilaritySettings(), 
            settings.isIncludeNullAsEntityCandidate(), 
            settings.isIncludeContextMentions(), 
            settings.shouldComputeConfidence(),
            settings.getMaxEntityRank(),
            content.getDocId(), solutions, tracer);
    ld.run();
    return solutions.get(content.getDocId());
  }

//  private Map<ResultMention, List<ResultEntity>> runLocalDisambiguationIterative(PreparedInput content, DisambiguationSettings settings, Tracer tracer) throws SQLException {
//    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
//    LocalDisambiguation ld = new LocalDisambiguationIterative(content, settings.getSimilaritySettings(), settings.isIncludeNullAsEntityCandidate(), settings.isIncludeContextMentions(), content.getDocId(), solutions, tracer);
//    ld.run();
//    return solutions.get(content.getDocId());
//  }

  //  private Map<ResultMention, List<ResultEntity>> runChakrabartiDisambiguation(PreparedInput content, DisambiguationSettings settings) throws Exception {
  //    ChakrabartiTechnique chakra = new ChakrabartiTechnique(content, settings.getSimilaritySettings(), 0.0, typeClassifier, tracer);
  //    Map<ResultMention, List<ResultEntity>> results = chakra.disambiguate();
  //    return results;
  //  }
  
  private Map<ResultMention, List<ResultEntity>> runOracleDisambiguation(
      PreparedInput content) {
    OracleDisambiguation od = new OracleDisambiguation(content);
    return od.run();
  }

  //  private Map<ResultMention, List<ResultEntity>> runChakrabartiDisambiguation(PreparedInput content, DisambiguationSettings settings) throws Exception {
    //    ChakrabartiTechnique chakra = new ChakrabartiTechnique(content, settings.getSimilaritySettings(), 0.0, typeClassifier, tracer);
    //    Map<ResultMention, List<ResultEntity>> results = chakra.disambiguate();
    //    return results;
    //  }
    
  private Map<ResultMention, List<ResultEntity>> runTagMeDisambiguation(
      PreparedInput content, DisambiguationSettings disSettings) {
    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = 
        Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
    TagMeDisambiguation tm = 
        new TagMeDisambiguation(
            content, settings.getSimilaritySettings(), 
            content.getDocId(), solutions);
    tm.run();
    return solutions.get(content.getDocId());
  }
  
  
  private Map<ResultMention, List<ResultEntity>> runSpotLightDisambiguation(
      PreparedInput content, DisambiguationSettings disSettings) {
    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = 
        Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
    DBpediaSpotlightDisambiguation st = 
        new DBpediaSpotlightDisambiguation(
            content, settings.getSimilaritySettings(), 
            content.getDocId(), solutions);
    st.run();
    return solutions.get(content.getDocId());
  }
  
  
  private Map<ResultMention, List<ResultEntity>> runAIDALightDisambiguation(
      PreparedInput content, DisambiguationSettings disSettings) {
    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = 
        Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
    AIDALightDisambiguation st = 
        new AIDALightDisambiguation(
            content, settings.getSimilaritySettings(), 
            content.getDocId(), solutions);
    st.run();
    return solutions.get(content.getDocId());
  }
  
  private Map<ResultMention, List<ResultEntity>> runIllinoisWikifierDisambiguation(
      PreparedInput content, DisambiguationSettings disSettings) {
    Map<String, Map<ResultMention, List<ResultEntity>>> solutions = 
        Collections.synchronizedMap(new HashMap<String, Map<ResultMention, List<ResultEntity>>>());
    IllinoisWikifierDisambiguation tm = 
        new IllinoisWikifierDisambiguation(
            content, settings.getSimilaritySettings(), 
            content.getDocId(), solutions);
    tm.run();
    Map<ResultMention, List<ResultEntity>> solution = solutions.get(content.getDocId());
    
    if (disSettings.shouldComputeConfidence()) {
      Map<ResultEntity, Double> scores = 
          new HashMap<ResultEntity, Double>();
      for (ResultMention rm : solution.keySet()) {
        for (ResultEntity re : solution.get(rm)) {
          scores.put(re, re.getDisambiguationScore());
        }
        Map<ResultEntity, Double> normScores = 
            CollectionUtils.shiftAndNormalizeScores(scores);
        for (ResultEntity re : solution.get(rm)) {
          re.setDisambiguationScore(normScores.get(re));
        }
      }
    }
    
    return solution;
  }

  @Override
  public void run() {
    try {
      DisambiguationResults result = disambiguate();
      result.setTracer(tracer);
      if (documentCounter != null) {
        // This provides a means of knowing where we are
        // and how long it took until now.
        documentCounter.oneDone();
      }
      resultsMap.put(input.getDocId(), result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}