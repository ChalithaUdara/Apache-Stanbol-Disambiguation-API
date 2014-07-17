package mpi.aidalight.exp;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.Settings;
import mpi.aidalight.entitycoherence.GraphSettings;
import mpi.aidalight.entitycoherence.GraphSettingsExperiment;
import mpi.aidalight.rmi.AIDALightServer;
import mpi.experiment.reader.WikipediaReader;
import mpi.util.Standardize;


/**
 * 
 * @author datnb
 *
 */
public class RunExp {
  private int numberOfFiles = 0;
  private Map<String, String> file2text = new HashMap<String, String>();
  private Map<String, TIntObjectHashMap<String>> file2entities = new HashMap<String, TIntObjectHashMap<String>>();
  private Map<String, List<Mention>> file2mentions = new HashMap<String, List<Mention>>();
  private String filter; // set null to run all files
  private AIDALightServer server = null;
  private String command = "fullSettings";
  private WikipediaReader wikipediaReader = null;
  
  public RunExp (String corpus, String filter) {
    GraphSettings gSettings = new GraphSettingsExperiment();
    gSettings.setCorpusName(corpus);
    this.filter = filter;
    if(corpus.indexOf("data/experiment/WIKIPEDIA/") == -1) {
      try {
        loadCorpus(corpus, gSettings);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    else {
      loadWikipediaCorpus(corpus);
    }
    String host = "localhost";
    try {
      Registry registry = LocateRegistry.getRegistry(host, 52365);
      server = (AIDALightServer) registry.lookup("NEDServer_" + host);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void loadWikipediaCorpus(String corpus){
    wikipediaReader = new WikipediaReader(corpus);
    for(String docId: wikipediaReader.getAllDocuments().keySet()) {
      file2text.put(docId, wikipediaReader.getText(docId));
      List<Mention> mentions = wikipediaReader.getDocumentMentions(docId).getMentions();
      file2mentions.put(docId, mentions);
      TIntObjectHashMap<String> map = new TIntObjectHashMap<String>();
      for(Mention mention: mentions) {
        map.put(mention.getCharOffset(), mention.getGroundTruthResult());
      }
      file2entities.put(docId, map);
    }
  }
  
  
  private void loadCorpus(String corpus, GraphSettings gSettings)throws Exception {
//    GraphSettingsExperiment settings = (GraphSettingsExperiment) gSettings;
    FileInputStream fis = new FileInputStream(corpus);
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);
    String line;
    String text = "";
    String title = null;
    TIntObjectHashMap<String> targetEntities = null;
    List<Mention> mentions = null;
    int offset = 0;
    while (true) {
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      if(line.startsWith("-DOCSTART-")) {
        if(title != null) {
          file2text.put(title, text);
          file2entities.put(title, targetEntities);
          file2mentions.put(title, mentions);
        }
        title = line.substring(12, line.length()-1);
        text = "";
        targetEntities = new TIntObjectHashMap<String>();
        mentions = new ArrayList<Mention>();
        offset = 0;
      }
      else {
        String[] str = line.split("\t");
        if(str.length > 2) {
          if(str[1].equalsIgnoreCase("B")) {
            Mention mention = new Mention();
            mention.setCharOffset(offset);
            mention.setMention(str[2]);
            mention.setGroundTruthResult(Standardize.unicodeConverter(URLDecoder.decode(str[3], "UTF-8")));
            mentions.add(mention);
            String s2 = Standardize.unicodeConverter(URLDecoder.decode(str[3], "UTF-8"));
            if(corpus.indexOf("WIKI-LINKS") != -1) {
              if(Settings.getTestPrecisionOption() && DataStore.getIdForEntity(s2) == 0)
                continue;
            }
            else {
              if(Settings.getTestPrecisionOption() && s2.equalsIgnoreCase(Entity.NO_MATCHING_ENTITY) )
                continue;
            }
            targetEntities.put(offset, s2);
          }
          text += str[0] + " ";
          offset += str[0].length() + 1;
        }
        else {
          text += line + " ";
          offset += line.length() + 1;
        }
      }
    }
    if(title != null) {
      file2text.put(title, text);
      file2entities.put(title, targetEntities);
      file2mentions.put(title, mentions);
    }
    isr.close();
    fis.close();
  }
 

  private int[] evaluate(TIntObjectHashMap<String> src, TIntObjectHashMap<String> dst) throws Exception {
    int counter = 0;
    int ner_counter = 0;
    for(int mention: src.keySet().toArray()){
      if(dst.containsKey(mention)){
        String s1 = Standardize.unicodeConverter(src.get(mention));
        String s2 = dst.get(mention); 
        try {
          s2 = Standardize.unicodeConverter(URLDecoder.decode(s2, "UTF-8"));
        } catch (IllegalArgumentException e) {
          s2 = Standardize.unicodeConverter(s2);
          ner_counter++;
          if(s1.equalsIgnoreCase(s2)) 
            counter++;
          continue;
        }
        ner_counter++;
        if(s1.equalsIgnoreCase(s2)) 
          counter++;
//        else
//          System.out.println(s1 + "..........--" + s2);
      }
    }
    System.out.println("all: " + (src.size()) + ", without out-of-kbs entities: " + (dst.size()) + ", matching: " + counter);
    int[] results = {(src.size()), dst.size(), ner_counter, counter};
    return results;
  }



  public void runExperiment(String corpus) throws Exception{
    long beginTime = System.currentTimeMillis();
    int r = 0, m = 0;
    double mr = 0;
    for(String title: file2text.keySet()) {
      if(filter != null && title.indexOf(filter) == -1)
        continue;
      System.out.println(title);
      numberOfFiles++;
      TIntObjectHashMap<String> mentionEntity = null;
      Map<Mention, Entity> res = server.disambiguate(file2text.get(title), file2mentions.get(title), command);
      mentionEntity = new TIntObjectHashMap<String>();
      for(Mention mention: res.keySet()) {
        mentionEntity.put(mention.getCharOffset(), res.get(mention).getName());
      }
      int[] tmp = evaluate(mentionEntity, file2entities.get(title));
      r += tmp[1];
      m += tmp[3];
      if(tmp[1] == 0)
        mr += 1;
      else
        mr += (double) tmp[3]/tmp[1];
    }
    System.out.println("==============Disambiguation results===========");
    System.out.println("Pre: " + (double)m/r);
    System.out.println("[Macro] Pre: " + mr/numberOfFiles);
    long time = System.currentTimeMillis() - beginTime;
    System.out.println("Disambiguate " + numberOfFiles + " files with " + r + " mentions in: " + time + "ms.");
    
  }
  
  public static void main(String args[]) throws Exception {
    RunExp exp = null;
    String corpus = null;
    String filter = "";
    if(args.length > 1)
      filter = args[1];
    
    if (args[0].equalsIgnoreCase("conll")) {
      corpus = "./data/experiment/CONLL/CoNLL-YAGO.tsv";
      if(filter == "")
        filter = "testb";
    }
    else if (args[0].equalsIgnoreCase("mw")) 
      corpus = "./data/experiment//WIKIPEDIA_YAGO2_HEAVYMETAL_SENTENCES_FAMILY/AIDA.tsv";
    else if (args[0].equalsIgnoreCase("wiki-links"))
      corpus = "./data/experiment/WIKI-LINKS/WIKI-LINKS.tsv";
    else 
      corpus = "./data/experiment/WIKIPEDIA/";
    
    exp = new RunExp(corpus, filter);
    exp.runExperiment(corpus);
  }
}
