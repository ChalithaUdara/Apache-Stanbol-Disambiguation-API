package mpi.aidalight.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.entitycoherence.NED;


public class AIDALight_main {
  public static String host;
  
  public static void main(String args[]) throws Exception {
    if(args.length < 1) {
      host = "localhost"; // default
      NED.partlyMatchingMentionEntitySearch = true;
    }
    else if(args.length == 1){
      host = args[0];
      NED.partlyMatchingMentionEntitySearch = true;
    }
    else {
      host = args[0];
      NED.partlyMatchingMentionEntitySearch = Boolean.parseBoolean(args[1]);
    }
    
//    if(NED.lshMeansTableRMI) {
//      LSHServer lshserver = new LSHServerImpl();
//      try {
//        LSHServer stub = (LSHServer) UnicastRemoteObject.exportObject(lshserver, 0);
//        // bind the remote object's stub in the registry
//        LocateRegistry.createRegistry(52378);
//        LocateRegistry.getRegistry(52378).bind("LSHServer_localhost", stub);
//      } catch (RemoteException e) {
//        e.printStackTrace();
//      } catch (AlreadyBoundException e) {
//        e.printStackTrace();
//      }
//      System.out.println("LSHServer started at " + host + "!");
//    }
    
    AIDALightServer server = new AIDALightServerImpl();
    AIDALightServer stub = (AIDALightServer) UnicastRemoteObject.exportObject(server, 0);

    // bind the remote object's stub in the registry
    LocateRegistry.createRegistry(52365);
    LocateRegistry.getRegistry(52365).bind("NEDServer_" + host, stub);
    System.out.println("Server started at " + host + "!");
    System.out.println("Disambiguate a sample sentence: With United, Beckham won the Premier League title 6 times.");
    Map<Mention, Entity> annotations = server.disambiguate("With United, Beckham won the Premier League title 6 times.", null, "fullSettings");
    // do whatever here...
    for(Mention mention: annotations.keySet()) {
      System.out.println(mention.getMention() + "\t" + annotations.get(mention).getName());
    }
  }
}
