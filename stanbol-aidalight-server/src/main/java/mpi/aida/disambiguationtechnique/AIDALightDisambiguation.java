package mpi.aida.disambiguationtechnique;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aidalight.rmi.AIDALightServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIDALightDisambiguation implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AIDALightDisambiguation.class);

  protected SimilaritySettings ss;

  protected PreparedInput input;

  protected String docId;

  protected Map<String, Map<ResultMention, List<ResultEntity>>> solutions;

  //  private static final String tagMeAuthToken = "tiro1013";

  private NumberFormat nf;


  private int callAidaLightService() throws IOException {
    String text = input.getTokens().toText();
    List<Mention> mentions = input.getMentions().getMentions();
    
    // set up server
    AIDALightServer server = null;
    String host = "d5blade06";
    try {
      Registry registry = LocateRegistry.getRegistry(host, 52365);
      server = (AIDALightServer) registry.lookup("NEDServer_" + host);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
//    String command = "testKWonly"; // = token
//    String command = ""; // token + easy mentions
//    String command = "updateLocalSimByChosenEntities decreaseRelatednessbyMentionDistance entityEntityRelatedness expandRelatedNessConstantbyGraph"; //  = token + easy mentions
    String command = "fullSettings"; // = token + easy mentions + update local_sim by chosen entities + domain coherence + etc.

    Map<Mention, Entity> annotations = server.disambiguate(text, mentions, command);
    Map<ResultMention, List<ResultEntity>> results = new HashMap<ResultMention, List<ResultEntity>>();
    
    // Remap spots to annotated mentions in the PreparedInput. Use
    // weak overlap.
//    Map<Integer, Mention> offset2mentions = getMentionsMap(input.getMentions());

    if (annotations == null) 
      return input.getMentions().getMentions().size();
//    Set<Mention> foundMentions = annotations.keySet();
    for (Mention mention: annotations.keySet()) {
      ResultMention rm = new ResultMention(docId, mention.getMention(), mention.getCharOffset(), mention.getMention().length());
      Entity entity = annotations.get(mention);
      ResultEntity re = new ResultEntity(entity.getName(), 1.0);
      results.put(rm, ResultEntity.getResultEntityAsList(re));
    }

//    // Add OOKBE for missed mentions.
//    Set<Mention> missedMentions = new HashSet<Mention>(input.getMentions().getMentions());
//    TIntHashSet set = new TIntHashSet();
//    for (Mention mention : foundMentions)
//      set.add(mention.getCharOffset());
//    //    missedMentions.removeAll(foundMentions);
//    for (Mention m : missedMentions) {
//      if (set.contains(m.getCharOffset())) continue;
//      //      System.out.println("Missing mention: " + m.getMention());
//      ResultMention rm = new ResultMention(docId, m.getMention(), m.getCharOffset(), m.getMention().length());
//      ResultEntity re = ResultEntity.getNoMatchingEntity();
//      results.put(rm, ResultEntity.getResultEntityAsList(re));
//    }
    
    solutions.put(docId, results);
    
    return input.getMentions().getMentions().size();

  }

//  private Map<Integer, Mention> getMentionsMap(Mentions mentions) {
//    Map<Integer, Mention> mentionsMap = new HashMap<Integer, Mention>();
//    for (Mention m : mentions.getMentions()) {
//      int start = m.getCharOffset();
//      int end = m.getCharOffset() + m.getCharLength();
//      for (int i = start; i < end; ++i) {
//        mentionsMap.put(i, m);
//      }
//    }
//    return mentionsMap;
//  }
//
//  private Mention getExistingMention(Map<Integer, Mention> offset2mentions, int start, int end) {
//    Map<Mention, Integer> mentionCount = new HashMap<Mention, Integer>();
//    for (int i = start; i < end; ++i) {
//      Mention mention = offset2mentions.get(i);
//      if (mention != null) {
//        Integer count = mentionCount.get(mention);
//        if (count == null) {
//          count = 0;
//        }
//        ++count;
//        mentionCount.put(mention, count);
//      }
//    }
//    if (mentionCount.size() > 0) {
//      LinkedHashMap<Mention, Integer> sorted = CollectionUtils.sortMapByValue(mentionCount);
//      return sorted.keySet().iterator().next();
//    } else {
//      return null;
//    }
//  }


  public AIDALightDisambiguation(PreparedInput input, SimilaritySettings settings, String docId,
      Map<String, Map<ResultMention, List<ResultEntity>>> solutions) {
    nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
    nf.setMaximumFractionDigits(2);
    logger.debug("Preparing '" + docId + "' (" + input.getMentions().getMentions().size() + " mentions)");
    this.ss = settings;
    this.docId = docId;
    this.solutions = solutions;
    this.input = input;
    logger.debug("Finished preparing '" + docId + "'");
  }

  @Override
  public void run() {
    long beginTime = System.currentTimeMillis();
    int numMentions = 0;
    try {
      numMentions = callAidaLightService();
    } catch (Exception e) {
      logger.error("Error: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
    double runTime = (System.currentTimeMillis() - beginTime) / (double) 1000;
    logger.info("Document '" + docId + "' done in " + nf.format(runTime) + "s");
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream("/local/datnb/AIDALight.log", true), "utf-8"));
      writer.write(docId + "\t" + numMentions + "\t" + (System.currentTimeMillis() - beginTime) + "\n");
      writer.flush();
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
