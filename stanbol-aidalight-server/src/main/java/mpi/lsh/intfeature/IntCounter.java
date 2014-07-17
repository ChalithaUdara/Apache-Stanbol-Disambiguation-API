package mpi.lsh.intfeature;

import java.io.Serializable;

public class IntCounter implements Comparable<IntCounter>, Serializable {
	private static final long serialVersionUID = 229L;

	int id;
	public int totalCount;

	// OpenIntIntHashMap entries;

	int keys[] = null; // have to be sorted in increasing order
	int vals[] = null;

	int hashcode = -1;

	public IntCounter(int id, int[] keys, int[] vals, int totalCount) {
		this.id = id;
		this.totalCount = totalCount;
		this.keys = keys;
		this.vals = vals;
	}

	public int[] keySet() {
		return keys;
	}
	
	public int[] valueSet(){
		return vals;
	}

	public int getCountFromIndex(int index) {
		return vals[index];
	}


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
	  return Double.compare(o.totalCount, this.totalCount);
	}

	public boolean equals(Object o) {
		if (o instanceof IntCounter) {
			// return Double.compare(((Counter) o).totalCount, this.totalCount);
			return this.id == ((IntCounter) o).id;
		}
		return false;
	}

	public int getTotalCount() {
		return totalCount;
	}


	public int getId() {
		return id;
	}

}
