package mpi.typerelatedness;


import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;

import mpi.util.Utils;

/**
 * This class is to compute the relatedness between 2 types (wiki-categories).
 * 
 * @author datnb
 *
 */
public abstract class TypeTypeRelatedness {
  
  TObjectIntHashMap<String> type2id;
  
  public TypeTypeRelatedness() {
    try {
      type2id = loadTypeDictionary();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private TObjectIntHashMap<String> loadTypeDictionary() throws IOException{
    TObjectIntHashMap<String>  type2id = new TObjectIntHashMap<String> ();
    int counter = 0;
    for(String line: Utils.getContent(Utils.getProperty("typeDictionary"))) {
      type2id.put(line, counter++);
    }
    return type2id;
  }
  
  protected long getIdforTypePair(String type1, String type2) {
    long id = (long)type2id.get(type1) * type2id.size() + type2id.get(type2);
    return id;
  }
  
  public abstract double getRelatedness(String type1, String type2);
  
}
