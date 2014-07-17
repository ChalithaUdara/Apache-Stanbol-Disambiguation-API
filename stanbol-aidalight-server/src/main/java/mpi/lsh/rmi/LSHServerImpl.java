package mpi.lsh.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import mpi.lsh.intfeature.LSHTable;
import mpi.lsh.utils.Common;
import mpi.util.Utils;


public class LSHServerImpl implements LSHServer {
  private LSHTable lsh;
  
  public LSHServerImpl() {
    LSHTable.CONFIDENCE_THRESHOLD = 0.8;
    lsh = new LSHTable(4, 6, 100, 999999999);
    // init
    init();
  }
  
  private void init() {
    System.out.print("Setting up lsh...");
    int counter = 0;
    try {
      // extract all possible mentions
      for(String line: Utils.getContent("./data/bned/resources/mention_entities")) {
        if(++counter % 500000 == 0) {
          System.out.print(counter + "...");
        }
        
        String str[] = line.split("\t");
        String mention = str[0].substring(1, str[0].length()-1).toLowerCase();
        lsh.put(Common.getCounter(mention));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Done!");
  }
  
  public Set<String> getSimilarName(String name) throws RemoteException {
    return new HashSet<String>(lsh.deduplicate(Common.getCounter(name.toLowerCase())));
  }
}
