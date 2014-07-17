package mpi.aidalight.exp;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;

import mpi.util.Utils;


public class CoNLLReader implements Reader{
  
  public TIntObjectHashMap<String> getTargetEntities(String file, boolean useOffset) throws IOException{
    TIntObjectHashMap<String> targetEntities = new TIntObjectHashMap<String>();
    String[] str = file.split("/");
    String target = str[0];
    for(int i = 1; i < str.length-3; i++)
      target += "/" + str[i];
    target += "/annotated_mentions/" + str[str.length-2] + "/" + str[str.length-1];
    for(String line: Utils.getContent(target)) {
      String s[] = line.split("\t");
      String entity = s[2];
      if(useOffset)
        targetEntities.put(Integer.parseInt(s[0]), entity);
      else
        targetEntities.put(s[1].hashCode(), entity);
    }
    return targetEntities;
  }
}