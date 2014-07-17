package mpi.lsh.intfeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javatools.datatypes.Pair;


public class SigComputer extends Thread {
  int id;
  int numberOfHashFunctions;
  int[] a;
  int[] b;
  int D;
//  BlockingQueue<Pair<String, int[]>> queue;
  BlockingQueue<Pair<Integer, List<Integer>>> queue;
  BufferedWriter writer;
  
  public SigComputer(int id, int numberOfHashFunctions, int[] a, int[] b, int D, BlockingQueue<Pair<Integer, List<Integer>>> queue, BufferedWriter writer) {
    this.id = id;
    this.numberOfHashFunctions = numberOfHashFunctions;
    this.a = a;
    this.b = b;
    this.D = D;
    this.queue = queue;
    this.writer = writer;
  }
  
  @Override
  public void run() {
    while (true) {
      Pair<Integer, List<Integer>> entityKeyphrases = null;
      try {
        entityKeyphrases = queue.take();
      } catch (InterruptedException e1) {
        System.err.println("Error getting element from queue ("+id+")");
        e1.printStackTrace();
      }
      
      if (entityKeyphrases == null || entityKeyphrases.first == null) {
        break;
      }
      
      // compute signature
      StringBuilder content = new StringBuilder();
      
      Integer entity = entityKeyphrases.first;
      List<Integer> keys = entityKeyphrases.second;
      
      content.append(entity).append("\t");
      for (int i = 0; i < numberOfHashFunctions; i++) {
        long min = Long.MAX_VALUE;
        for (int j = 0; j < keys.size(); j++) {
          long tmp = (a[i] * keys.get(j) + b[i]) % D;
          if (tmp < min){
        	  min = tmp;
          }
        }
        content.append(min).append("\t");
      }
      content.append("\n");
      
      synchronized (writer) {
        try {
          writer.write(content.toString());
        } catch (IOException e) {
          System.err.println("Could not write signatures for '"+entity+"'");
          e.printStackTrace();
        }
      }
    }
  }
}