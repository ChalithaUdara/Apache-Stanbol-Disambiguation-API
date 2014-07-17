package mpi.lsh.utils;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.Arrays;

import mpi.lsh.intfeature.IntWeightedCounter;
import mpi.lsh.intfeature.IntWeightedLSHTable;
import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;


public class WeighedLSHUtils {
//  private static TObjectIntHashMap<String> shingles = new TObjectIntHashMap<String>();
  
  private static TIntDoubleHashMap tokenWeight = new TIntDoubleHashMap();

//  private static int keys = 1;
  
  public static double getTokenWeight(int feature) {
    return tokenWeight.get(feature);
  }
  
  
  public static IntWeightedCounter getIntWeightedCounter(int[] tokens){
    OpenIntIntHashMap entries = new OpenIntIntHashMap();
    
    for(int i = 0; i < tokens.length; i++){
      if(entries.containsKey(tokens[i])){
        entries.put(tokens[i], entries.get(tokens[i]) + 1);
      }
      else{
        entries.put(tokens[i], 1);
      }
    }
    
    int[] keys = new int[entries.size()];
    IntArrayList l = entries.keys();
    for (int i = 0; i < entries.size(); i++)
      keys[i] = l.get(i);

    // sort keys arrays
    Arrays.sort(keys);
    int[] vals = new int[keys.length];
    for (int i = 0; i < keys.length; i++)
      vals[i] = entries.get(keys[i]);

    entries = null;
    return new IntWeightedCounter(tokens.hashCode(), keys);
  }
  
  
  public static void main(String args[]) throws Exception {
    int[][] toks = {{0, 1, 2, 3}, // "the president of the US" 
        {0, 1, 3}, // "the US president"
        {1, 3}, // "US president"
        {0, 1, 4}, // "the president Obama"
        {0, 1, 2, 5}}; // "the president of B"
    
    // "the president A", "Barack Obama"
    String[] queries = {"the A", "the president", "Barack Obama"};
    int[][] quries = {{0, 6}, {0, 1}, {4, 7}};
    
    tokenWeight.put(0, 0.8); // "the" 0.1
    tokenWeight.put(1, 0.3); // "president" 0.3
    tokenWeight.put(2, 0.1); // "of" 0.1
    tokenWeight.put(3, 0.6); // "US" 0.6
    tokenWeight.put(4, 0.8); // "Obama" 0.8
    tokenWeight.put(5, 0.3); // "B" 0.3
    
    tokenWeight.put(6, 0.4); // "A" 0.4
    tokenWeight.put(7, 0.5); // "Barack" 0.5
    
    IntWeightedLSHTable lsh = new IntWeightedLSHTable(1, 6, 100, 199);
    for(int i = 0; i < toks.length; i ++)
      lsh.put(getIntWeightedCounter(toks[i]));
    
    for(int i = 0; i < quries.length; i ++) {
      System.out.println("results for \"" + queries[i] + "\":");
      for(IntWeightedCounter counter: lsh.deduplicate(getIntWeightedCounter(quries[i]))) {
        int keys[] = counter.keySet();
        for(int key: keys)
          System.out.print(key + "\t");
        System.out.println();
      }
    }
  }
}
