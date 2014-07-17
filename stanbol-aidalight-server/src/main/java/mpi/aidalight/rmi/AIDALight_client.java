package mpi.aidalight.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;


public class AIDALight_client {
  
  /**
   * 
   * @param text - this is clean text.
   * @param mentions: set null if mentions are not annotated. In this case, StanfordNER will be used to annotate the text.
   * @throws RemoteException
   */
  public static void disambiguate(String text, List<Mention> mentions, String host) throws RemoteException {
    if(host == null)
      host = "localhost"; // default
    // set up server
    AIDALightServer server = null;
    try {
      Registry registry = LocateRegistry.getRegistry(host, 52365);
      server = (AIDALightServer) registry.lookup("NEDServer_" + host);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    String command = "fullSettings"; // = key-words + 2-phase mapping + domain

    Map<Mention, Entity> annotations = server.disambiguate(text, mentions, command);
    
    // do whatever here...
    for(Mention mention: annotations.keySet()) {
      String wikipediaEntityId = "http://en.wikipedia.org/wiki/" + annotations.get(mention).getName();
      System.out.println(mention.getMention() + "\t" + wikipediaEntityId);
    }
  }
  
  public static void main(String args[]) throws Exception {
    AIDALight_client.disambiguate("With United, Beckham won the Premier League title 6 times.", null, null);
  }
}
