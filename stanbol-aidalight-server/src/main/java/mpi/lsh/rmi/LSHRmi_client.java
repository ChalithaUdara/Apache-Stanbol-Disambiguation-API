package mpi.lsh.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;




public class LSHRmi_client {
  public static void main(String args[]) {
    LSHServer server = null;
    String host = "titan";
    try {
      Registry registry = LocateRegistry.getRegistry(host, 52378);
      server = (LSHServer) registry.lookup("LSHServer_" + host);
      
      Set<String> names = server.getSimilarName("THECAMBRIDGESHIRE REGIMENT");
      if(names != null) {
        for(String name: names) {
          System.out.println(name);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
