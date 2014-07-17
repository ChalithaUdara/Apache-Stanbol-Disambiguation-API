package mpi.aidalight.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;


public interface AIDALightServer extends Remote {
  
  public Map<Mention, Entity> disambiguate(String text, List<Mention> mentions, String command) throws RemoteException;
  
}
