package mpi.typerelatedness;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mpi.lsh.intfeature.LSHTable;
import mpi.lsh.utils.Common;
import mpi.util.Utils;



/**
 * 
 * @author datnb
 *
 */
public class TypeTypeRelatednessCooccurrence extends TypeTypeRelatedness{
  private TIntIntHashMap typeCounter = null;
  
  private TLongIntHashMap cooccurrenceCounter = null;
  
  private TObjectIntHashMap<String> type2id = null;
  private TIntObjectHashMap<String> id2type = null; 
  
  private Map<String, TIntHashSet> tokenIndex = null;
  
  private Set<String> blackList = null;
  
  /*
   * If a type is missing, try to find the nearest type of it to compute the relatedness between types
   * 
   * E.g. there is English_football_clubs but Slovak_football_clubs is missing. 
   * sim(Slovak_football_clubs, A) = sim(Slovak_football_clubs, English_football_clubs) x sim(English_football_clubs, A)
   */
  public boolean useNearestTypeForMissingType = false;
  
  public boolean useSmartStatistics = true;
  
//  private static int minFrequency = Integer.MAX_VALUE;
//  private static double constant;
  
  private static int threshold = 10000000;
  
  private static LSHTable lsh = null;
  
  public TypeTypeRelatednessCooccurrence(boolean useNearestTypeForMissingType, boolean useSmartStatistics) {
//    super();
    this.useNearestTypeForMissingType = useNearestTypeForMissingType;
    this.useSmartStatistics = useSmartStatistics;
    
    try {
      init();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  
  private void initType2Id() throws IOException {
    type2id = new TObjectIntHashMap<String>();
    id2type = new TIntObjectHashMap<String>();
    
    for(String line: Utils.getContent(Utils.getProperty("importantType2Id"))) {
      String[] str = line.split("\t");
      type2id.put(str[0], Integer.parseInt(str[1]));
      id2type.put(Integer.parseInt(str[1]), str[0]);
    }
  }
  
  private void initTypeCounter() throws IOException {
    typeCounter = new TIntIntHashMap();
    blackList = new HashSet<String>();
    for(String line: Utils.getContent(Utils.getProperty("importantTypeCounter"))) {
      String[] str = line.split("\t");
      
      int K = Integer.parseInt(str[1]);
      int id = Integer.parseInt(str[0]);
      typeCounter.put(id, K);
      
      if(K > threshold) {
        blackList.add(id2type.get(id));
        System.out.println("Blacklist: " + id2type.get(id));
      }
      
//      if(!useSmartStatistics) {
//        if(K < minFrequency)
//          minFrequency = K;
//      }
    }
//    if(!useSmartStatistics)
//      constant = (double) Math.pow(Math.log(minFrequency), 2);
  }
  
  private void initTypeTypeCounter() throws IOException {
    cooccurrenceCounter = new TLongIntHashMap();
    System.out.print("Loading type-cooccurrence...");
    int counter = 0;
    FileInputStream fis = new FileInputStream(Utils.getProperty("importantTypeTypeCooccurrenceCounter"));
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);

    String line;
    while (true) {
      if(++counter % 10000000 == 0)
        System.out.print(counter + "...");
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      String[] str = line.split("\t");
      cooccurrenceCounter.put(Long.parseLong(str[0]), Integer.parseInt(str[1]));
    }

    isr.close();
    fis.close();
    System.out.println("type-cooccurrence loaded!");
  }
  
  private void init() throws IOException {
    initType2Id();
    initTypeCounter();
    
    if(!useSmartStatistics)
      initTypeTypeCounter();
    else {
      indexTokensInWikiCategory();
    }
    
    if(useNearestTypeForMissingType)
      loadLSH();
  }
  
  public int getIdforType(String type) {
    if(type2id == null) {
      try {
        initType2Id();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if(type2id.contains(type))
      return type2id.get(type);
    return -1;
  }
  
  public int countType(String type) {
    if(typeCounter == null) {
      try {
        initTypeCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    int id = getIdforType(type);
    if(id != -1)
      return typeCounter.get(id);
    return 0;
  }
  
  public int countType(int typeId) {
    if(typeCounter == null) {
      try {
        initTypeCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return typeCounter.get(typeId);
  }
  
  public int countTypeType(String type1, String type2) {
    if(cooccurrenceCounter == null) {
      try {
        initTypeTypeCounter();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    int a = getIdforType(type1);
    int b = getIdforType(type2);
    long id = concatenate(a, b);
    return cooccurrenceCounter.get(id);
  }
  
  
  @Override
  public double getRelatedness (String type1, String type2) {
    if(blackList.contains(type2) || blackList.contains(type1))
      return 0.0;
    if(useSmartStatistics)
      return getRelatednessBySmartStatistics(type1, type2);
    return getRelatednessByCooccurence(type1, type2);
  }
  
  
  private double getRelatednessBySmartStatistics (String type1, String type2) {
    if(type1.equalsIgnoreCase(type2))
      return 1.0;
    
//    if(type2id.containsKey(type1) == false || type2id.containsKey(type2) == false)
//      return 0.0;
    
    String[] str = type1.split("_");
    Set<String> set = new HashSet<String>();
    TIntHashSet res = null;
    for(int i = 1; i < str.length; i ++) // don't count str[0] which is "wikicategory"
      set.add(str[i]);
    String[] dst = type2.split("_");
    TIntHashSet[] arrays = new TIntHashSet[dst.length];
    int size = 0;
    for(int i = 1; i < dst.length; i ++) {
      if(set.contains(dst[i])) {
        // update
        TIntHashSet relatedWikicategories = getRelatedCategories(dst[i]);
        if(relatedWikicategories != null)
          arrays[size++] = relatedWikicategories;
      }
    }
    
    res = union(arrays, size);
    if(res == null || res.size() == 0)
      return 0.0;
    
    int sum = 0;
    for(int wikicategory: res.toArray()) {
      sum += countType(wikicategory);
    }
    
    return (double)Math.max(countType(type1), countType(type2)) / sum;
  }
  
  private TIntHashSet union(TIntHashSet[] arrays, int size) {
    if(size == 0)
      return null;
    if(size == 1) 
      return arrays[0];
    
    // sort
    for(int i = 0; i < size; i ++)
      for(int j = i + 1; j < size; j ++) {
        if(arrays[i].size() > arrays[j].size()) {
          TIntHashSet tmp = arrays[i];
          arrays[i] = arrays[j];
          arrays[j] = tmp;
        }
      }
    
    TIntHashSet res = union(arrays[0], arrays[1]);
    for(int i = 2; i < size; i ++) {
      res = union(res, arrays[i]);
      if(res.size() == 0)
        break;
    }
    return res;
  }
  
  private TIntHashSet union(TIntHashSet src, TIntHashSet dst) {
    TIntHashSet res = new TIntHashSet();
    if(src == null || dst == null)
      return res;
    for(int val: src.toArray())
      if(dst.contains(val))
        res.add(val);
    return res;
  }
  
  private TIntHashSet getRelatedCategories(String token) {
    if(tokenIndex == null) {
      try {
        indexTokensInWikiCategory();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
    return tokenIndex.get(token);
  }
  
  private double getRelatednessByCooccurence (String type1, String type2) {
    if(type2id == null || typeCounter == null || cooccurrenceCounter == null) {
      try {
        init();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } 
    if(type1.equalsIgnoreCase(type2))
      return 1.0;
    double a = 1.0, b = 1.0;
    if(useNearestTypeForMissingType) {
      if(type2id.containsKey(type1) == false) {
        String tmp = getNearestString(type1);
        if(tmp == null)
          return 0.0;
        a = LSHTable.getJaccard(Common.getCounter(type1), Common.getCounter(tmp), LSHTable.CONFIDENCE_THRESHOLD);
        type1 = tmp;
      }
      if(type2id.containsKey(type2) == false) {
        String tmp = getNearestString(type2);
        if(tmp == null)
          return 0.0;
        b = LSHTable.getJaccard(Common.getCounter(type2), Common.getCounter(tmp), LSHTable.CONFIDENCE_THRESHOLD);
        type2 = tmp;
      }
    }
    else {
      if(type2id.contains(type2) == false || type2id.contains(type1) == false)
        return 0.0;
    }
    
    return a * b * (double) countTypeType(type1, type2) / 
        (Math.min(countType(type1), countType(type2)));
        
  }
  
  
  private static void loadLSH() throws IOException{
    System.out.print("Setting up LSH...");
    lsh = new LSHTable(6, 8, 128, 128);
    LSHTable.CONFIDENCE_THRESHOLD = 0.8;
    for(String line: Utils.getContent(Utils.getProperty("importantType2Id"))) {
      String str[] = line.split("\t");
      lsh.put(Common.getCounter(str[0]));
    }
    System.out.println("LSH built!");
  }
  
  private static String getNearestString(String wikiType) {
    if(lsh == null) {
      try {
        loadLSH();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return lsh.getNearest(Common.getCounter(wikiType));
  }
  
  private static long concatenate(int a, int b) {
    long MAX = 4000000l;
    if(a > b)
      return concatenate(b, a);
    return (long) a * MAX + b;
  }
  
  
  
  private void indexTokensInWikiCategory() throws IOException {
    tokenIndex = new HashMap<String, TIntHashSet>();
    for(String line: Utils.getContent(Utils.getProperty("importantType2Id"))) {
      String str[] = line.split("\t");
      String[] tokens = str[0].split("_");
      // don't count tokens[0] which is "wikicategory"
      for(int i = 1; i < tokens.length; i ++) {
        TIntHashSet tmp = tokenIndex.get(tokens[i]);
        if(tmp == null) {
          tmp = new TIntHashSet();
          tmp.add(Integer.parseInt(str[1]));
          tokenIndex.put(tokens[i], tmp);
        }
        else
          tmp.add(Integer.parseInt(str[1]));
      }
    }
    
//    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(Utils.getProperty("wikicategoryInvertedIndex"));
//    for(String type: tokenIndex.keySet()) {
//      writer.write(type);
//      for(int val: tokenIndex.get(type).toArray())
//        writer.write("\t" + Integer.toString(val));
//      writer.newLine();
//    }
//    writer.flush();
//    writer.close();
  }

}
