package mpi.lsh.intfeature;

import java.io.Serializable;

import mpi.lsh.utils.WeighedLSHUtils;

public class IntWeightedCounter implements Comparable<IntCounter>, Serializable {
  private static final long serialVersionUID = 229L;

  int id;
//  public int totalCount;
  
  // OpenIntIntHashMap entries;

  int keys[] = null; // have to be sorted in increasing order
//  double vals[] = null;

  int hashcode = -1;
  
  private int[] index; // store which keys are chosen to create signatures
  
  private double totalWeight = 0.0;

  /**
   * 
   * @param id is the unique id of the mention/entity
   * @param keys
   */
  public IntWeightedCounter(int id, int[] keys) {
    this.id = id;
//    this.totalCount = totalCount;
    this.keys = keys;
    for(int key: keys)
      totalWeight += WeighedLSHUtils.getTokenWeight(key);
//    this.vals = vals;
  }

  public int[] keySet() {
    return keys;
  }
  
//  public double[] valueSet(){
//    return vals;
//  }
//
//  public double getCountFromIndex(int index) {
//    return vals[index];
//  }


  public int hashCode() {
    if (hashcode != -1)
      return hashcode;
    hashcode = 0;
    for (int i = 0; i < keys.length; i++)
      hashcode += keys[i]; // approximate! may create collisions
    return hashcode;
  }


  // Sort by signature length
  public int compareTo(IntCounter o) {
//    return Double.compare(o.totalCount, this.totalCount);
    if(this.keys.hashCode() > o.keys.hashCode())
      return 1;
    else if(this.keys.hashCode() == o.keys.hashCode())
      return 0;
    else 
      return -1;
  }

  public boolean equals(Object o) {
    if (o instanceof IntCounter) {
      // return Double.compare(((Counter) o).totalCount, this.totalCount);
      return this.id == ((IntCounter) o).id;
    }
    return false;
  }

//  public int getTotalCount() {
//    return totalCount;
//  }


  public int getId() {
    return id;
  }
  
  public int[] getIndex() {
    return index;
  }
  
  public void setIndex(int[] index) {
    this.index = index;
  }
  
  public double getTotalWeight() {
    return totalWeight;
  }

}
