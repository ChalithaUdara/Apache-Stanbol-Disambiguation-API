package mpi.experiment;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javatools.util.FileUtils;
import mpi.util.Utils;


public class IntersectionMentionSetAmongKBsExtraction {
  
  public static TObjectIntHashMap<String> entity2id = null;
  
  private static void loadEntityId() throws IOException {
    System.out.print("Loading entityId...");
    entity2id = new TObjectIntHashMap<String>();
    int counter = 0;
    for(String line: Utils.getContent("../bned/data/bned/resources/entity_id")) {
      if(++counter % 500000 == 0)
        System.out.print(counter + "...");
      String str[] = line.split("\t");
      entity2id.put(Standardize.unicodeConverter(str[0]), Integer.parseInt(str[1]));
    }
    System.out.println("EntityId loaded!");
  }
  
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
  
  public void extract() throws Exception {
//    FileInputStream fis = new FileInputStream("../bned/data/WIKI-LINKS/CoNLL-YAGO.tsv");
    FileInputStream fis = new FileInputStream("./data/experiment/WIKIPEDIA_YAGO2_HEAVYMETAL_SENTENCES_FAMILY/AIDA-original.tsv");
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);
    
    BufferedWriter writer = FileUtils.getBufferedUTF8Writer("./data/experiment/WIKIPEDIA_YAGO2_HEAVYMETAL_SENTENCES_FAMILY/AIDA.tsv");

    String line;
    int counter = 0;
    int numLine = 0;
    int ookb = 0;
    while (true) {
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      if(++numLine % 10000 == 0)
        System.out.println(numLine);
      String str[] = line.split("\t");
      if(str.length > 1) {
        String entity = Standardize.unicodeConverter(str[3]);
        // check if it is in YAGO
        if(getIdForEntity(entity) != 0) {
//        if(entity.equalsIgnoreCase("--NME--") == false) {
          writer.write(line);
          writer.newLine();
          if(str[1].equalsIgnoreCase("B"))
            counter ++;
        }
        else {
          writer.write(str[0]);
          writer.newLine();
        }
      }
      else {
        writer.write(line);
        writer.newLine();
      }
    }
    
    writer.flush();
    writer.close();

    isr.close();
    fis.close();
    System.out.println(counter + "\t" + ookb);
  }
  
  public static void main(String args[]) throws Exception {
    new IntersectionMentionSetAmongKBsExtraction().extract();
  }
}
