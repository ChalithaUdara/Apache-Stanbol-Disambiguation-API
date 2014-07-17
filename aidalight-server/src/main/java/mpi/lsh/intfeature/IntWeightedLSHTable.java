package mpi.lsh.intfeature;


import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import mpi.lsh.utils.WeighedLSHUtils;
import cern.colt.map.OpenIntObjectHashMap;


@SuppressWarnings ("unchecked")
public class IntWeightedLSHTable {

//  private static final long serialVersionUID = 228L;
  
  public static double SIGNATURE_THRESHOLD = 0.2;

  public static double CONFIDENCE_THRESHOLD = 0.3;

  private MinhashTable[] minhashTables;

  private int k, P;

  private int l;//, m, d;

  private int[] a, b;
  
  private Random random = new Random(1337);
  
  
  private class MinhashTable extends OpenIntObjectHashMap {
    private static final long serialVersionUID = 228L;

    private int[] indices; // permutation indices
    
    // Draw k random permutations for each table
    private MinhashTable(int k, int m) {
      super(10000);
      this.indices = new int[k];
      for (int i = 0; i < k; i++){
        indices[i] = (int) Math.floor(random.nextDouble() * m);
      }
    }

    private int[] getSignature(IntWeightedCounter counter) {
//    public int[] getSignature(IntWeightedCounter counter) {
      int[] signature = new int[k];
      int q;

      int[] keys = counter.keySet();
      int[] index = new int[k];
      for (int i = 0; i < k; i++) {
        signature[i] = Integer.MAX_VALUE;
        for (int j = 0; j < keys.length; j++) {
          q = ((a[indices[i]] * keys[j]) + b[indices[i]]) % P;
          if (q < signature[i]) {
            signature[i] = q;
            index[i] = j;
          }
        }
      }
      counter.setIndex(index);
      return signature;
    }

    private int hashCode(int[] signature) {
      int h = 0;
      for (int i = 0; i < signature.length && i < k; i++) {
        h += signature[i];
      }
      return h;// s.hashCode();
    }
    

    private void put(IntWeightedCounter counter) {
      int hashCode = hashCode(getSignature(counter));
      HashSet<IntWeightedCounter> bucket = null;
      synchronized (this) {
        bucket = (HashSet<IntWeightedCounter>) super.get(hashCode);
      }
      if (bucket == null) {
        bucket = new HashSet<IntWeightedCounter>();
        synchronized (this) {
          super.put(hashCode, bucket);
        }
      }
      synchronized (bucket) {
        bucket.add(counter);
      }
    }
    
    private Set<IntWeightedCounter> getBucket(IntWeightedCounter counter) {
      HashSet<IntWeightedCounter> bucket = null;
      // Optimization to avoid redundant minhash computations!
      synchronized (this) { // due to modifying counter.
        int[] signature = getSignature(counter);
        
        // compute the weight
        int[] index = counter.getIndex();
        int[] keys = counter.keys;
        double totalWeight = 0.0;
        TIntHashSet set = new TIntHashSet(index);
        for(int i: set.toArray()) 
          totalWeight += WeighedLSHUtils.getTokenWeight(keys[i]);
        if(totalWeight < SIGNATURE_THRESHOLD)
          return bucket;
        
        bucket = (HashSet<IntWeightedCounter>) super
            .get(hashCode(signature));
      }
      return bucket;
    }
  }

  public IntWeightedLSHTable(int k, int l, int m, int d) {

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
//    this.m = m;
//    this.d = d;

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

  public Set<IntWeightedCounter> deduplicate(IntWeightedCounter counter) {
    // Remove repeated entries from buckets
    HashSet<IntWeightedCounter> union = new HashSet<IntWeightedCounter>();
    // OpenIntObjectHashMap union = new OpenIntObjectHashMap();

    HashSet<IntWeightedCounter> bucket;
    for (int i = 0; i < l; i++)
      if ((bucket = (HashSet<IntWeightedCounter>) minhashTables[i]
          .getBucket(counter)) != null)
        union.addAll(bucket);

    Set<IntWeightedCounter> res = new HashSet<IntWeightedCounter>();
    // Check for near duplicates
    for (IntWeightedCounter counter2 : union) {
      if (counter != counter2
          && (getJaccard(counter, counter2,
              CONFIDENCE_THRESHOLD)) >= CONFIDENCE_THRESHOLD) {
        res.add(counter2);
      }
    }

    return res;
  }

  public void put(IntWeightedCounter counter) {
    for (int i = 0; i < l; i++)
      minhashTables[i].put(counter);
  }

  // Jaccard similarity generalized for multi-sets (weighted dimensions)
  public static double getJaccard(IntWeightedCounter counter1, IntWeightedCounter counter2,
      double threshold) {
    double w = 0.0;

    // merge join
    int pos = 0;
    int[] keys1 = counter1.keySet();
    int[] keys2 = counter2.keySet();
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
        w += WeighedLSHUtils.getTokenWeight(key);
      }

      else {
        // nothing to do
      }
    }

    return w / (counter1.getTotalWeight() + counter2.getTotalWeight() - w);
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
        HashSet<IntWeightedCounter> bucket = (HashSet<IntWeightedCounter>) list[j];
        
        for(IntWeightedCounter counter: bucket){
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

