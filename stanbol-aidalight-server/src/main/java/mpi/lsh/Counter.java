package mpi.lsh;

import java.io.Serializable;

//This is a simple hash aggregator for counting signatures
public class Counter implements Comparable<Counter>, Serializable {
	private static final long serialVersionUID = 229L;

	String id;
	public int totalCount;

	// OpenIntIntHashMap entries;

	int keys[] = null; // have to be sorted in increasing order
	int vals[] = null;

	int hashcode = -1;

	// public Counter(String id) {
	// this.id = id;
	// // this.entries = new OpenIntIntHashMap();
	// this.totalCount = 0;
	// }

	// public Counter(String id, OpenIntIntHashMap entries, int totalCount) {
	// this.id = id;
	// this.totalCount = totalCount;
	// this.keys = new int[entries.size()];
	// IntArrayList l = entries.keys();
	// for (int i = 0; i < entries.size(); i++)
	// keys[i] = l.get(i);
	//
	// // sort keys arrays
	// Arrays.sort(this.keys);
	// this.vals = new int[keys.length];
	// for (int i = 0; i < keys.length; i++)
	// vals[i] = entries.get(keys[i]);
	//
	// entries.clear();
	// entries = null;
	// }

	public Counter(String id, int[] keys, int[] vals, int totalCount) {
		this.id = id;
		this.totalCount = totalCount;
		this.keys = keys;
		this.vals = vals;
	}

	public int[] keySet() {
		return keys;
		// // if we use array structure
		// if (keys != null)
		// return keys;
		//
		// // if we use OpenIntIntHashMap structure
		// keys = new int[entries.size()];
		// IntArrayList l = entries.keys();
		// for (int i = 0; i < entries.size(); i++)
		// keys[i] = l.get(i);
		// return keys;
	}
	
	public int[] valueSet(){
		return vals;
	}

	public int getCountFromIndex(int index) {
		return vals[index];
	}

	// free memory for entries
	// use array structure to save memory
	// public void deleteEntries() {
	// // initialize keys array
	// this.keys = new int[entries.size()];
	// IntArrayList l = entries.keys();
	// for (int i = 0; i < entries.size(); i++)
	// keys[i] = l.get(i);
	//
	// // sort keys arrays
	// Arrays.sort(this.keys);
	// this.vals = new int[keys.length];
	// for (int i = 0; i < keys.length; i++)
	// vals[i] = entries.get(keys[i]);
	//
	// entries.clear();
	// entries = null;
	// }

	public int hashCode() {
		if (hashcode != -1)
			return hashcode;
		hashcode = 0;
		for (int i = 0; i < keys.length; i++)
			hashcode += keys[i]; // approximate! may create collisions
		return hashcode;
	}

	// return # of keys
	// public int size() {
	// return entries == null ? keys.length : entries.size();
	// }
	//
	// public boolean containsKey(int key) {
	// return entries.containsKey(key);
	// }
	//
	// public int getCount(int key) {
	// return entries.get(key);
	// }
	//
	// public void incrementCount(int key) {
	// entries.put(key, getCount(key) + 1);
	// totalCount++;
	// }

	// Sort by signature length
	public int compareTo(Counter o) {
	  return Double.compare(o.totalCount, this.totalCount);
	}

	public boolean equals(Object o) {
		if (o instanceof Counter) {
			// return Double.compare(((Counter) o).totalCount, this.totalCount);
			return this.id.equals(((Counter) o).id);
		}
		return false;
	}

	public int getTotalCount() {
		return totalCount;
	}

	// public String toString() {
	// String s = id + "=[";
	// int[] keys = keySet();
	// for (int i = 0; i < keys.length; i++) {
	// s += String.valueOf(keys[i]) + ":"
	// + String.valueOf(getCount(keys[i]))
	// + (i < keys.length - 1 ? ", " : "");
	// }
	// s += "] @ " + String.valueOf(totalCount);
	// return s;
	// }

	public String getId() {
		return id;
	}

}
