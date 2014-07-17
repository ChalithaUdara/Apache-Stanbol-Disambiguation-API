package mpi.aidalight.exp;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;


public interface Reader {
  public TIntObjectHashMap<String> getTargetEntities(String file, boolean useOffset) throws IOException;
}
