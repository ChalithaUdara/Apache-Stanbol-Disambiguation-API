package mpi.experiment;

import java.io.File;

import mpi.util.Utils;

public class ExtractFiles {

  private static void extractFiles(String corpus) throws Exception {
    //  GraphSettingsExperiment settings = (GraphSettingsExperiment) gSettings;
//    FileInputStream fis = new FileInputStream(corpus);
//    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//    BufferedReader bufReader = new BufferedReader(isr);
//    String line;
//    String content = "";
//    int counter = 0;
//    while (true) {
//      line = bufReader.readLine();
//      if (line == "" || line == null) break;
//      if (line.startsWith("-DOCSTART-")) {
//        String title = line.substring(12, line.length()-1);
//        System.out.println(title);
//        counter++;
//        content += title + "\n";
//      } 
//    }
//    isr.close();
//    fis.close();
//    Utils.writeContent("./data/file_names/wiki-links", content);
//    System.out.println(counter);
    
    String content = "";
    int counter = 0;
    File folder = new File("./data/experiment/WIKIPEDIA/text");
    for(File file: folder.listFiles()) {
      String line = file.getPath();
      String str[] = line.split("/");
      String title = Integer.toString(counter) + " http://en.wikipedia.org/wiki/" + str[str.length-1];
      counter++;
      content += title + "\n";
    }
    
    Utils.writeContent("./data/file_names/wikipedia", content);
    System.out.println(counter);
  }

  public static void main(String args[]) throws Exception {
    extractFiles("./data/experiment/WIKI-LINKS/WIKI-LINKS.tsv");
  }
}
