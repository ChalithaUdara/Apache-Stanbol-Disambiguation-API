package mpi.aidalight.exp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import mpi.util.Utils;


public class ExtractSampleDocuments {
  private static void extractSampleDocs() throws IOException {
    List<String> content = new ArrayList<String>();
    
    FileInputStream fis = new FileInputStream("../bned/data/WIKI-LINKS/CoNLL-YAGO.tsv");
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);

    String line;
    boolean read = false;
    String sampleDoc = "";
    int counter = 0;
    String str = "";
    while (true) {
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      if(line.startsWith("-DOCSTART- (")) { // || line.startsWith("-DOCSTART- (1215") || line.startsWith("-DOCSTART- (1250"))
        counter++;
        System.out.println(counter);
      }
//        read = true;
//      if(line.startsWith("-DOCSTART- (1200") || line.startsWith("-DOCSTART- (1216"))
//        read = false;
//      if(line.startsWith("-DOCSTART- (165"))
//        break;
//      if(read)
//        sampleDoc += line + "\n";
//      content.add(line);
      if(counter == 50)
        break;
      str += line + "\n";
    }
    
    Utils.writeContent("../bned/data/WIKI-LINKS-small/CoNLL-YAGO.tsv", str);

    isr.close();
    fis.close();
    System.out.println(sampleDoc);
  }
  public static void main(String args[]) throws Exception {
    extractSampleDocs();
    
//    String mention = "their next adventure";
//    Settings settings = new Settings();
//    Set<String> domains = new HashSet<String>();
//    domains.add("wordnetDomain_football");
//    
//    for(Entity entity: DataStore.getEntitiesForMention(mention, null)) {
//      System.out.println(entity + "\t" + Function.getEntityDomainsRelatedness(entity, domains, settings.getEntityDomainDeep()));
//      System.out.println(DataStore.getEntityPriors(mention).get(entity.getId()));
//    }
//    
//    System.out.println(DataStore.getIdForEntity("The_Creature_from_the_Pit"));
    
  }
}
