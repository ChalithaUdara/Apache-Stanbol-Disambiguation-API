package mpi.lsh.intfeature;


import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import cern.colt.map.OpenIntObjectHashMap;


@SuppressWarnings ("unchecked")
public class IntLSHTable {

//	private static final long serialVersionUID = 228L;

//	public static double CONFIDENCE_THRESHOLD = 0.8;

	private MinhashTable[] minhashTables;

	private int k, P;

	private int l;//, m, d;

	private int[] a, b;
	
	private Random random = new Random(1337);

	private class MinhashTable extends OpenIntObjectHashMap {
		private static final long serialVersionUID = 228L;

//		private int idx; // table id

		private int[] indices; // permutation indices
		
		// Draw k random permutations for each table
		private MinhashTable(int k, int m) {
			super(10000);
//			this.idx = idx;
			this.indices = new int[k];
			for (int i = 0; i < k; i++){
				indices[i] = (int) Math.floor(random.nextDouble() * m);
//				System.out.println(a[indices[i]] + "\t" + b[indices[i]]);
			}
		}

		// private int[] getSignature(Counter counter) {
		public int[] getSignature(IntCounter counter) {
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

		public int hashCode(int[] signature) {
			int h = 0;
			for (int i = 0; i < signature.length && i < k; i++) {
				h += signature[i];
			}
			return h;// s.hashCode();
		}

		private void put(IntCounter counter) {
			int hashCode = hashCode(getSignature(counter));
			HashSet<IntCounter> bucket = null;
			synchronized (this) {
				bucket = (HashSet<IntCounter>) super.get(hashCode);
			}
			if (bucket == null) {
				bucket = new HashSet<IntCounter>();
				synchronized (this) {
					super.put(hashCode, bucket);
				}
			}
			synchronized (bucket) {
				bucket.add(counter);
			}
		}

		private Set<IntCounter> getBucket(IntCounter counter) {
			HashSet<IntCounter> bucket = null;
			// Optimization to avoid redundant minhash computations!
			synchronized (this) { // TODO why synchronized?
				bucket = (HashSet<IntCounter>) super
						.get(hashCode(getSignature(counter)));
			}
			return bucket;
		}
	}

	public IntLSHTable(int k, int l, int m, int d) {

		// k: number of min-hashes (random permutations) to be concatenated
		// (higher value increases precision but loses recall)
		// l: number of min-hash functions to be used for finding near matches
		// (higher value increases recall but loses precision)
		// m: number of random permutations that are created and from which l
		// permutations are drawn as hash functions (m should be >> l)
		// d: number of output dimensions into which the signatures are
		// projected (less dimensions mean more collisions)

		this.k = k;
		this.l = l;
//		this.m = m;
//		this.d = d;

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

	public TIntHashSet deduplicate(IntCounter counter) {

	  // TODO Ask: no banding?
	  
		// Remove repeated entries from buckets
		HashSet<IntCounter> union = new HashSet<IntCounter>();
		// OpenIntObjectHashMap union = new OpenIntObjectHashMap();

		HashSet<IntCounter> bucket;
		for (int i = 0; i < l; i++)
			if ((bucket = (HashSet<IntCounter>) minhashTables[i]
					.getBucket(counter)) != null)
				union.addAll(bucket);

		TIntHashSet keyphrases = new TIntHashSet();
		// Check for near duplicates
		for (IntCounter counter2 : union) {
			if(counter.equals(counter2) == false)
//			double sim = 1.0;
//			if (counter != counter2
//					&& (getJaccard(counter, counter2,
//							CONFIDENCE_THRESHOLD)) >= CONFIDENCE_THRESHOLD) {
				// System.out.println(counter.id + "\t" + counter2.id + "\t"
				// + String.valueOf(sim));
				keyphrases.add(counter2.getId());
//			}
		}

		return keyphrases;
	}

	public void put(IntCounter counter) {
		for (int i = 0; i < l; i++)
			minhashTables[i].put(counter);
	}

	// Jaccard similarity generalized for multi-sets (weighted dimensions)
	public static double getJaccard(IntCounter index1, IntCounter index2,
			double threshold) {
		double min, max, s_min = 0, bound = 0;
		double upper_max = Math.max(index1.totalCount, index2.totalCount);
		double upper_union = index1.totalCount + index2.totalCount;
		int c1, c2;//, s_c1 = 0, s_c2 = 0;


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
//				s_max += max;
//				s_c1 += c1;
//				s_c2 += c2;

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
	
	
	

	private TIntObjectHashMap<TIntHashSet> keyphraseToBucketID = null;
	
	public TIntObjectHashMap<TIntHashSet> getKeyphraseToBucketID(){
		if(keyphraseToBucketID == null)
			buildKeyphraseToBucketID();
		return keyphraseToBucketID;
	}
	
	private void buildKeyphraseToBucketID(){
		keyphraseToBucketID = new TIntObjectHashMap<TIntHashSet>();
		int A = k * P;
		//for bucket j of minhashtable i, map each keyphrase to A * i + j
		//therefore a keyphrase will map to a list of ids
		
		for(int i = 0; i < l; i ++){
			Object[] list = minhashTables[i].values().elements();
			for (int j = 0; j < list.length; j ++) {
				HashSet<IntCounter> bucket = (HashSet<IntCounter>) list[j];
				
				for(IntCounter counter: bucket){
					int tmpID = A * i + j;
					TIntHashSet ids = keyphraseToBucketID.get(counter.getId());
					if(ids == null)
						ids = new TIntHashSet();
					ids.add(tmpID);
					
					keyphraseToBucketID.put(counter.getId(), ids);
				}
			}
		}
	}
	

}
