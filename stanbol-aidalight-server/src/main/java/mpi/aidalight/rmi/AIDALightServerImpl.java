package mpi.aidalight.rmi;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.Settings;
import mpi.aidalight.entitycoherence.GraphSettings;
import mpi.aidalight.entitycoherence.GraphSettingsExperiment;
import mpi.aidalight.entitycoherence.NED;


public class AIDALightServerImpl implements AIDALightServer {
  
  private NED disambiguator;
  
  public AIDALightServerImpl() {
    GraphSettings gSettings = new GraphSettingsExperiment();
    this.disambiguator = new NED(new Settings(), gSettings);
  }
  
  @Override
  public Map<Mention, Entity> disambiguate(String text, List<Mention> mentions, String command) throws RemoteException {
    NED.fullSettings = false;
    NED.testKWonly = false;
    NED.testOnEasyMentions = false;
    NED.updateLocalSimByChosenEntities = false;
    NED.decreaseRelatednessbyMentionDistance = false;
    NED.entityEntityRelatedness = false;
    NED.expandRelatedNessConstantbyGraph = false;
    
    if(command.indexOf("fullSettings") != -1) {
      NED.fullSettings = true;
      NED.updateLocalSimByChosenEntities = true;
      NED.decreaseRelatednessbyMentionDistance = true;
      NED.entityEntityRelatedness = true;
      NED.expandRelatedNessConstantbyGraph = true;
      System.out.println("Set NED.fullSettings=true");
    }
    if(command.indexOf("testKWonly") != -1) {
      NED.testKWonly = true;
      System.out.println("Set NED.testKWonly=true");
    }
    if(command.indexOf("testOnEasyMentions") != -1) {
      NED.testOnEasyMentions = true;
      System.out.println("Set NED.testOnEasyMentions=true");
    }
    if(command.indexOf("updateLocalSimByChosenEntities") != -1) {
      NED.updateLocalSimByChosenEntities = true;
      System.out.println("Set NED.updateLocalSimByChosenEntities=true");
    }
    if(command.indexOf("decreaseRelatednessbyMentionDistance") != -1) {
      NED.decreaseRelatednessbyMentionDistance = true;
      System.out.println("Set NED.decreaseRelatednessbyMentionDistance=true");
    }
    if(command.indexOf("entityEntityRelatedness") != -1) {
      NED.entityEntityRelatedness = true;
      System.out.println("Set NED.entityEntityRelatedness=true");
    }
    if(command.indexOf("expandRelatedNessConstantbyGraph") != -1) {
      NED.expandRelatedNessConstantbyGraph = true;
      System.out.println("Set NED.expandRelatedNessConstantbyGraph=true");
    }
    
//    // setting parameters
//    if(parameters != null) {
//      Settings.contextSimilarityContribution = parameters[0];
//      Settings.priorContribution = parameters[1];
//      Settings.domainContribution = parameters[2];
//      Settings.probabilityEntityGivenMentionsContribution = parameters[3];
//      Settings.matchingContribution = 1 - Settings.contextSimilarityContribution - Settings.priorContribution
//          - Settings.domainContribution - Settings.probabilityEntityGivenMentionsContribution;
//      
//      Settings.entityEntityContextRelatednessContribution = parameters[4];
//      Settings.entityEntityTypeRelatednessContribution = 1 - Settings.entityEntityContextRelatednessContribution;
//    }
    
    Map<Mention, Entity> results = new HashMap<Mention, Entity>();
    try {
      results = disambiguator.disambiguate(text, mentions); 
    } catch(Exception e) {
      e.printStackTrace();
    }
    return results;
  }

}
