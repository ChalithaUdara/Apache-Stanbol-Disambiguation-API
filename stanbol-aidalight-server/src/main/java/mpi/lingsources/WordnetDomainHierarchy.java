package mpi.lingsources;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javatools.util.FileUtils;
import mpi.util.Utils;


public class WordnetDomainHierarchy {
  private String wordnetDomains = "./data/bned/yago/wordnet_domains";
  
  private static Map<String, String> hierarchy = null;
  
  public static void init() throws Exception {
    hierarchy = new HashMap<String, String>();
    for(String line: Utils.getContent(Utils.getProperty("wordnetDomainHierarchy"))) {
      if(line.startsWith("<wordnetDomain")) {
        String str[] = line.split(" ");
        hierarchy.put(WikiWordnetMapper.standardize(str[2]), WikiWordnetMapper.standardize(str[0]));
      }
    }
  }
  
//  public static String getFirstLevelDomain(String wordnetDomain) {
//    if(hierarchy == null) {
//      try {
//        init();
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//    String str = wordnetDomain;
//    while(hierarchy.keySet().contains(str) && hierarchy.get(str).equalsIgnoreCase("wordnetDomain_top") == false)
//      str = hierarchy.get(str);
//    return str;
//  }
  
  
  /**
   * 
   * @param src
   * @param dst
   * @return  1 if src is sub_domain of dst or src = dst; -1 if src is super_domain of dst; 0 otherwise
   */
  public static int getRelation(String src, String dst) {
    if(hierarchy == null) {
      try {
        init();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if(src.equalsIgnoreCase(dst))
      return 1;
    String str = src;
    while(hierarchy.keySet().contains(str)) {
      str = hierarchy.get(str);
      if(str.equalsIgnoreCase(dst))
        return 1;
    }
    str = dst;
    while(hierarchy.keySet().contains(str)) {
      str = hierarchy.get(str);
      if(str.equalsIgnoreCase(src))
        return -1;
    }
    return 0;
  }
  
  public static List<List<String>> traceRoot(String wikiType) {
    if(hierarchy == null) {
      try {
        init();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    List<List<String>> types = new ArrayList<List<String>>();
    String wordnetType = WikiWordnetMapper.getWordnetTypeFromWikiCategory(wikiType);
    List<String> str = WikiWordnetMapper.getWordnetDomainFromWordnetCategory(wordnetType);
    if(str == null || str.size() == 0) {
      // something is wrong here
      // just return something
      List<String> tmp = new ArrayList<String>();
      tmp.add(wikiType);
      tmp.add("wordnetDomain_top");
      List<List<String>> res = new ArrayList<List<String>>();
      res.add(tmp);
      return res;
    }
    
    for(String domain: str) {
      List<String> branch = new ArrayList<String>();
      branch.add(wikiType);
      branch.add(wordnetType);
      String tmp = domain;
      while(tmp != null){
        branch.add(tmp);
        tmp = hierarchy.get(tmp);
      }
      
      types.add(branch);
    }
    
    return types;
  }
  
  
  // Use additional information from wordnet_domain_hierarchy file to map wordnet_domains onto our domains.
  private void process() throws Exception {
    if(hierarchy == null) {
      try {
        init();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    String content = "";
    for(String line: Utils.getContent(wordnetDomains)) {
      String str[] = line.split("\t");
      String domain = str[0];
      content += domain;
      while(hierarchy.keySet().contains(domain)) {
        if(domain.equalsIgnoreCase("wordnetDomain_sport")){
          content += "\tsport";
          break;
        }
        if(domain.equalsIgnoreCase("wordnetDomain_politics")){
          content += "\tpolitics";
          break;
        }
        if(domain.equalsIgnoreCase("wordnetDomain_pure_science")){
          content += "\tscience";
          break;
        }
        if(domain.equalsIgnoreCase("wordnetDomain_applied_science")){
          content += "\tscience";
          break;
        }
        domain = hierarchy.get(domain);
      }
      content += "\n";
    }
    
 // write into file
    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(wordnetDomains);
    writer.write(content);
    writer.flush();
    writer.close();
  }
  
  public static void main(String args[]) throws Exception {
    new WordnetDomainHierarchy().process();
  }
}
