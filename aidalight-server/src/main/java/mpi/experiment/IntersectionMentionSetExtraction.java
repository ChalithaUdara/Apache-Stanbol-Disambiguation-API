package mpi.experiment;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javatools.util.FileUtils;
import mpi.util.Utils;


public class IntersectionMentionSetExtraction {
  
  // ------------- CONLL-YAGO
//  private String ROOT = "./data/experiment/CONLL/runs/results/";
//  
//  private String[] corpora = {"20131206-1717_-s_SwitchedKP-MW_-l_CPSC_-a_0.6_-x_-size_5_normObj_c0.9_1163-1393_results.tsv", // AIDA
//      "20140111-2230_-s_none_-l_IW_-l_IW_1163-1393_results.tsv", // Wikifier
//      "20140113-1616_-s_none_1163-1393_results.tsv" // Spotlight
//      }; 
//  
//  public static String INTERSECTION_MENTIONS = "./data/experiment/CONLL/intersection_mentions";
  

  private String ROOT = "../bned/data/WIKI-LINKS/runs_10000/results/";
  
  private String[] corpora = {"20140113-1240_-s_prior_-l_LOCAL_results.tsv", // AIDA
//      "20140113-2050_-s_none_-l_IW_-l_IW_results.tsv", // Wikifier
//      "20140116-0144_-s_none_-l_AIDALIGHT_results.tsv", // AIDALight
//      "20140113-1829_-s_none_results.tsv" // Spotlight
      }; 
  
  public static String INTERSECTION_MENTIONS = "../bned/data/WIKI-LINKS/intersection_mentions";
  
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
  
  
  private TObjectIntHashMap<String> entity2id = null;
  
  private void loadEntityId() throws IOException {
    System.out.print("Loading entityId...");
    entity2id = new TObjectIntHashMap<String>();
    int counter = 0;
    for(String line: Utils.getContent("../bned/data/bned/resources/entity_id")) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      entity2id.put(str[0], Integer.parseInt(str[1]));
    }
    System.out.println("EntityId loaded!");
  }
  
  public int getIdForEntity(String entity) {
    if(entity2id == null) {
      try {
        loadEntityId();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    return entity2id.get(entity);
  }
  
  
  private Map<String, TIntHashSet> extractFile2Mentions(String file) throws IOException {
    Map<String, TIntHashSet> file2mentions = new HashMap<String, TIntHashSet>();
    int counter = 0, numEntities = 0;
    for(String line: getContent(file)) {
      counter++;
      String str[] = line.split("\t");
//      if(str[4].equalsIgnoreCase(Entity.OOKBE) || str[4].equalsIgnoreCase("--NME--"))
//        continue;
//      numEntities++;
      if(getIdForEntity(str[4]) == 0)
        continue;
      
      TIntHashSet offsets = file2mentions.get(str[0]);
      if(offsets == null) {
        offsets = new TIntHashSet();
        // check ookb entity
        offsets.add(Integer.parseInt(str[1]));
        file2mentions.put(str[0], offsets);
      }
      else {
        // check ookb entity
        offsets.add(Integer.parseInt(str[1]));
      }
    }
    System.out.println(file + "\t" + counter + "\t" + numEntities);
    return file2mentions;
  }
  
  private Map<String, TIntHashSet> join(Map<String, TIntHashSet> src, Map<String, TIntHashSet> dst) {
    Map<String, TIntHashSet> file2mentions = new HashMap<String, TIntHashSet>();
    for(String file: src.keySet()) {
      if(dst.containsKey(file)) {
        TIntHashSet set1 = src.get(file);
        TIntHashSet set2 = dst.get(file);
        TIntHashSet set = new TIntHashSet();
        for(int key: set1.toArray()) {
          if(set2.contains(key))
            set.add(key);
        }
        file2mentions.put(file, set);
      }
    }
    return file2mentions;
  }
  
  public void extract() throws IOException {
    Map<String, TIntHashSet> file2mentions = extractFile2Mentions(ROOT + corpora[0]);
    for(int i = 1; i < corpora.length; i ++) 
      file2mentions = join(file2mentions, extractFile2Mentions(ROOT + corpora[i]));
    
    int counter = 0;
    // write to file
    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(INTERSECTION_MENTIONS);
    for(String file: file2mentions.keySet()) {
      writer.write(file);
      counter += file2mentions.get(file).size();
      for(int val: file2mentions.get(file).toArray())
        writer.write("\t" + Integer.toString(val));
      writer.newLine();
    }
    writer.flush();
    writer.close();
    System.out.println("Intersection set consisting of " + counter + " mentions was written to " + INTERSECTION_MENTIONS);
  }
  
  public static void main(String args[]) throws Exception {
    new IntersectionMentionSetExtraction().extract();
  }
}
