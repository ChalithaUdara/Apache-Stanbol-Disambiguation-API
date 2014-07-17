package mpi.lsh.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;


public interface LSHServer extends Remote {
  
  public Set<String> getSimilarName(String name) throws RemoteException;
  
}
