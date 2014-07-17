package mpi.lsh.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;



public class LSHRmi_main {
  public static void main(String args[]) throws Exception {
    String host;
    if(args.length < 1) {
      host = "localhost"; // default
    }
    else {
      host = args[0];
    }
    LSHServer server = new LSHServerImpl();
    LSHServer stub = (LSHServer) UnicastRemoteObject.exportObject(server, 0);

    // bind the remote object's stub in the registry
    LocateRegistry.createRegistry(52378);
    LocateRegistry.getRegistry(52378).bind("LSHServer_" + host, stub);
    System.out.println("LSHServer started at " + host + "!");
  }
}
