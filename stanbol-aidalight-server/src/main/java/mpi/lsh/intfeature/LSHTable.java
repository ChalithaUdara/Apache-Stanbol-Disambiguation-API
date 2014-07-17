package mpi.lsh.intfeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mpi.lsh.Counter;
import cern.colt.map.OpenIntObjectHashMap;

public class LSHTable implements Serializable {
	private static final long serialVersionUID = 228L;

	// public static double CONFIDENCE_THRESHOLD = 0.5;
	public static double CONFIDENCE_THRESHOLD = 0.8;

	private MinhashTable[] minhashTables;

	private int k, P;

	@SuppressWarnings("unused")
	private int l, m, d;

	private int[] a, b;
	
	private Random random = new Random(1337);

	@SuppressWarnings("unchecked")
	private class MinhashTable extends OpenIntObjectHashMap {
	  
	  private static final long serialVersionUID = 2342L;

//		private int idx; // table id

		private int[] indices; // permutation indices

		// Draw k random permutations for each table
		private MinhashTable(int k, int m) {
			super(10000);
//			this.idx = idx;
			this.indices = new int[k];
			for (int i = 0; i < k; i++)
				indices[i] = (int) Math.floor(random.nextDouble() * m);
		}

		// private int[] getSignature(Counter counter) {
		public int[] getSignature(Counter counter) {
			int[] signature = new int[k];
			int q;

			int[] keys = counter.keySet();
			for (int i = 0; i < k; i++) {
				signature[i] = Integer.MAX_VALUE;
				for (int j = 0; j < keys.length; j++) {
					q = ((a[indices[i]] * keys[j]) + b[indices[i]]) % P;
					if (q < signature[i])
						signature[i] = q;
				}
			}
			return signature;
		}

		// private int hashCode(int[] signature) {
		public int hashCode(int[] signature) {
			int h = 0;
			// String s = "";
			for (int i = 0; i < signature.length && i < k; i++) {
				// h += Math.pow(signature[i], i + 1); // bad for long
				// signatures
				// s += String.valueOf(signature[i]) + "$"; // approximate!
				// may create
				// additional
				// collisions
				h += signature[i];
				// System.out.print("  " + signature[i]);
			}
			// System.out.println(" \t" + h + " \t" + s.hashCode());
			return h;// s.hashCode();
		}

		private void put(Counter counter) {
			int hashCode = hashCode(getSignature(counter));
			HashSet<Counter> bucket = null;
			synchronized (this) {
				bucket = (HashSet<Counter>) super.get(hashCode);
			}
			if (bucket == null) {
				bucket = new HashSet<Counter>();
				synchronized (this) {
					super.put(hashCode, bucket);
				}
			}
			synchronized (bucket) {
				bucket.add(counter);
			}
			// if (counter.minhashes != null)
			// counter.minhashes[idx] = hashCode;
		}

		private Set<Counter> getBucket(Counter counter) {
			HashSet<Counter> bucket = null;
			// Optimization to avoid redundant minhash computations!
			// if (counter.minhashes == null) {
			synchronized (this) { // TODO why synchronized?
				bucket = (HashSet<Counter>) super
						.get(hashCode(getSignature(counter)));
			}
			// } else {
			// synchronized (this) {
			// bucket = (HashSet<Counter>)
			// super.get(counter.minhashes[idx]);
			// }
			// }
			return bucket;
		}
	}

	public LSHTable(int k, int l, int m, int d) {

		// k: number of minhashes (random permutations) to be concatenated
		// (higher value increases precision but loses recall)
		// l: number of minhash functions to be used for finding near matches
		// (higher value increases recall but loses precision)
		// m: number of random permutations that are created and from which l
		// permutations are drawn as hash functions (m should be >> l)
		// d: number of output dimensions into which the signatures are
		// projected (less dimensions mean more collisions)

		this.k = k;
		this.l = l;
		this.m = m;
		this.d = d;

		// Initialize array of m random linear projections
		this.P = getPrime(d);
		this.a = new int[m];
		this.b = new int[m];
				
		for (int i = 0; i < m; i++) {
			// this.a[i] = 1 + (int) Math.floor(Math.random() * (d - 1));
			// this.b[i] = (int) Math.floor(Math.random() * d);
			this.a[i] = 1 + (int) Math.floor(random.nextDouble() * (P - 1));
			this.b[i] = (int) Math.floor(random.nextDouble() * P);
		}

		// Array of l minhash tables
		this.minhashTables = new MinhashTable[l];
		for (int i = 0; i < l; i++)
			this.minhashTables[i] = new MinhashTable(k, m);
	}

	@SuppressWarnings("unused")
	public List<String> deduplicate(Counter counter) {
		// Remove repeated entries from buckets
		HashSet<Counter> union = new HashSet<Counter>();
		// OpenIntObjectHashMap union = new OpenIntObjectHashMap();

		HashSet<Counter> bucket;
		for (int i = 0; i < minhashTables.length; i++)
			if ((bucket = (HashSet<Counter>) minhashTables[i]
					.getBucket(counter)) != null)
				union.addAll(bucket);
		// for (Counter c : bucket)
		// union.put(c.hashCode(), c);

		// System.out.println(counter.getId() + " " + union.size());

		List<String> names = new ArrayList<String>();
		// Check for near duplicates
		for (Counter counter2 : union) {
			// for (Object o : union.values().elements()) {
			// Counter counter2 = (Counter) o;
			double sim = 1.0;
			if (counter != counter2
					&& (sim = getJaccard(counter, counter2,
							CONFIDENCE_THRESHOLD)) >= CONFIDENCE_THRESHOLD) {
				// System.out.println(counter.id + "\t" + counter2.id + "\t"
				// + String.valueOf(sim));
				names.add(counter2.getId());
			}
		}

		return names;
	}
	
	public String getNearest(Counter counter) {
	  HashSet<Counter> union = new HashSet<Counter>();
    // OpenIntObjectHashMap union = new OpenIntObjectHashMap();

    HashSet<Counter> bucket;
    for (int i = 0; i < minhashTables.length; i++)
      if ((bucket = (HashSet<Counter>) minhashTables[i]
          .getBucket(counter)) != null)
        union.addAll(bucket);
    // for (Counter c : bucket)
    // union.put(c.hashCode(), c);

    // System.out.println(counter.getId() + " " + union.size());

    String bestCandidate = null;
    double max = 0.0;
    // Check for near duplicates
    for (Counter counter2 : union) {
      // for (Object o : union.values().elements()) {
      // Counter counter2 = (Counter) o;
      double sim = getJaccard(counter, counter2, CONFIDENCE_THRESHOLD);
      if (counter != counter2 && sim > max) {
        // System.out.println(counter.id + "\t" + counter2.id + "\t"
        // + String.valueOf(sim));
        max = sim;
        bestCandidate = counter2.getId();
      }
    }
    
    if(max < CONFIDENCE_THRESHOLD)
      return null;
    return bestCandidate;
	}

	public void put(Counter counter) {
		for (int i = 0; i < minhashTables.length; i++)
			minhashTables[i].put(counter);
	}

	// Jaccard similarity generalized for multi-sets (weighted dimensions)
	@SuppressWarnings("unused")
	public static double getJaccard(Counter index1, Counter index2,
			double threshold) {
		double min, max, s_min = 0, s_max = 0, bound = 0;
		double upper_max = Math.max(index1.totalCount, index2.totalCount);
		double upper_union = index1.totalCount + index2.totalCount;
		int c1, c2, s_c1 = 0, s_c2 = 0;

		// check upper bound of Jaccard similarity
		int numberOfSignatures = index1.getTotalCount() < index2
				.getTotalCount() ? index1.getTotalCount() : index2
				.getTotalCount();
		if ((double) numberOfSignatures / upper_max < threshold)
			return 0;

		// merge join
		int pos = 0;
		int[] keys1 = index1.keySet();
		int[] keys2 = index2.keySet();
		for (int i = 0; i < keys1.length; i++) {
			int key = keys1[i];
			while (pos < keys2.length && keys2[pos] < key)
				pos++;

			if (pos == keys2.length) {
				// nothing to do more
				break;
			}
			if (keys2[pos] == key) {
				// matching
				c1 = index1.getCountFromIndex(i);
				c2 = index2.getCountFromIndex(pos);
				min = Math.min(c1, c2);
				max = Math.max(c1, c2);
				s_min += min;
				s_max += max;
				s_c1 += c1;
				s_c2 += c2;

				// Early threshold break for pairwise counter comparison
				bound += max - min;

				// Enable this for better efficiency. Exact Jaccard similarities
				// will not always be printed when enabled!
				if ((upper_max - bound) / upper_max < threshold)
					return 0;
				else if (s_min / upper_union >= threshold)
					return 1;
			}

			else {
				// nothing to do
			}
		}

		return s_min / (index1.totalCount + index2.totalCount - s_min);
	}
	
	
	@SuppressWarnings("unused")
	public static double getExactJaccard(Counter index1, Counter index2) {
    double min, max, s_min = 0, s_max = 0, bound = 0;
//    double upper_max = Math.max(index1.totalCount, index2.totalCount);
//    double upper_union = index1.totalCount + index2.totalCount;
    int c1, c2, s_c1 = 0, s_c2 = 0;

    // check upper bound of Jaccard similarity
//    int numberOfSignatures = index1.getTotalCount() < index2
//        .getTotalCount() ? index1.getTotalCount() : index2
//        .getTotalCount();
//    if ((double) numberOfSignatures / upper_max < threshold)
//      return 0;

    // merge join
    int pos = 0;
    int[] keys1 = index1.keySet();
    int[] keys2 = index2.keySet();
    for (int i = 0; i < keys1.length; i++) {
      int key = keys1[i];
      while (pos < keys2.length && keys2[pos] < key)
        pos++;

      if (pos == keys2.length) {
        // nothing to do more
        break;
      }
      if (keys2[pos] == key) {
        // matching
        c1 = index1.getCountFromIndex(i);
        c2 = index2.getCountFromIndex(pos);
        min = Math.min(c1, c2);
        max = Math.max(c1, c2);
        s_min += min;
        s_max += max;
        s_c1 += c1;
        s_c2 += c2;

        // Early threshold break for pairwise counter comparison
        bound += max - min;

      }

      else {
        // nothing to do
      }
    }

    return s_min / (index1.totalCount + index2.totalCount - s_min);
  }

	private static int getPrime(int n) {
		while (!isPrime(n))
			n++;
		return n;
	}

	private static boolean isPrime(int n) {
		if (n <= 2)
			return n == 2;
		else if (n % 2 == 0)
			return false;
		for (int i = 3, end = (int) Math.sqrt(n); i <= end; i += 2)
			if (n % i == 0)
				return false;
		return true;
	}
	
	
	
	/*
	 * =======================================================================
	 * lsh for clustering
	 * ========================================================================
	 */

	// private class Pair implements Comparable {
	// String firstElement;
	// String secondElement;
	//
	// public Pair(String firstElement, String secondElement) {
	// this.firstElement = firstElement;
	// this.secondElement = secondElement;
	// }
	//
	// @Override
	// public int compareTo(Object o) {
	// if (o instanceof Pair) {
	// Pair tmp = (Pair) o;
	// if (tmp.firstElement.compareTo(firstElement) == 0)
	// return tmp.secondElement.compareTo(secondElement);
	// else
	// return tmp.firstElement.compareTo(firstElement);
	// }
	// return 0;
	// }
	//
	// public boolean equals(Object o) {
	// if (o instanceof Pair) {
	// Pair tmp = (Pair) o;
	// return tmp.firstElement.equalsIgnoreCase(firstElement)
	// && tmp.secondElement.equalsIgnoreCase(secondElement);
	// }
	// return false;
	// }
	//
	// public String toString() {
	// return firstElement + "==" + secondElement;
	// }
	// }

	private Map<String, Set<Integer>> keyphraseToBucketID = null;
	
	public Map<String, Set<Integer>> getKeyphraseToBucketID(){
		if(keyphraseToBucketID == null)
			buildKeyphraseToBucketID();
		return keyphraseToBucketID;
	}
	
	@SuppressWarnings("unchecked")
	private void buildKeyphraseToBucketID(){
		keyphraseToBucketID = new HashMap<String, Set<Integer>>();
		int A = k * P;
		//for bucket j of minhashtable i, map each keyphrase to A * i + j
		//therefore a keyphrase will map to a list of ids
		
		for(int i = 0; i < minhashTables.length; i ++){
			Object[] list = minhashTables[i].values().elements();
			for (int j = 0; j < list.length; j ++) {
				HashSet<Counter> bucket = (HashSet<Counter>) list[j];
				
				for(Counter counter: bucket){
					int tmpID = A * i + j;
					Set<Integer> ids = keyphraseToBucketID.get(counter.getId());
					if(ids == null)
						ids = new HashSet<Integer>();
					ids.add(tmpID);
					
					keyphraseToBucketID.put(counter.getId(), ids);
				}
			}
		}
	}
	
//	public List<List<String>> cluster(List<String> keyphrases) {
//		// compute pairwise similarities
//		HashSet<Counter> bucket;
//
//		Set<Pair> similarPairs = new HashSet<Pair>();
//
//		for (int i = 0; i < minhashTables.length; i++) {
//			Object[] list = minhashTables[i].values().elements();
//			for (Object o : list) {
//				bucket = (HashSet<Counter>) o;
//				Object[] counters = bucket.toArray();
//				Arrays.sort(counters);
//				for (int u = 0; u < counters.length; u++)
//					for (int v = u + 1; v < counters.length; v++) {
//						Counter c1 = (Counter) counters[u];
//						Counter c2 = (Counter) counters[v];
//						if (getJaccard(c1, c2, CONFIDENCE_THRESHOLD) >= CONFIDENCE_THRESHOLD) {
//							similarPairs.add(new Pair(c1.getId(), c2.getId()));
//						}
//					}
//			}
//		}
//
//		Object[] pairs = similarPairs.toArray();
//		// for(int i = 0; i < pairs.length; i ++)
//		// for(int j = i + 1; j < pairs.length; j ++)
//		// if(((Pair)pairs[i]).compareTo((Pair)pairs[j]) < 0){
//		// Object tmp = pairs[i];
//		// pairs[i] = pairs[j];
//		// pairs[j] = tmp;
//		// }
//		Arrays.sort(pairs);
//		// for(Object o: pairs)
//		// System.out.println(o.toString());
//
//		Set<String> isMarked = new HashSet<String>();
//
//		List<List<String>> result = new ArrayList<List<String>>();
//		String currentCenter = ((Pair) pairs[0]).firstElement;
//		isMarked.add(currentCenter);
//		List<String> currentCluster = new ArrayList<String>();
//		currentCluster.add(currentCenter);
//
//		for (Object o : pairs) {
//			Pair pair = (Pair) o;
//			if (pair.firstElement.equalsIgnoreCase(currentCenter)) {
//				if (isMarked.contains(pair.secondElement) == false) {
//					// then put in current cluster
//					currentCluster.add(pair.secondElement);
//					isMarked.add(pair.secondElement);
//				}
//			} else {
//				if (currentCluster != null) {
//					result.add(currentCluster);// done with one cluster
//					currentCluster = null;
//					currentCenter = null;
//				}
//
//				if (isMarked.contains(pair.firstElement) == false) {
//					// then new center
//					currentCenter = pair.firstElement;
//					isMarked.add(currentCenter);
//					currentCluster = new ArrayList<String>();
//					currentCluster.add(currentCenter);
//
//					// check second element
//					if (isMarked.contains(pair.secondElement) == false) {
//						// then put in current cluster
//						currentCluster.add(pair.secondElement);
//						isMarked.add(pair.secondElement);
//					}
//				}
//			}
//		}
//
//		return result;
//	}

}
