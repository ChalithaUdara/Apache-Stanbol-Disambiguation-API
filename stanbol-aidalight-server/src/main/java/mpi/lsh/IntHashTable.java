package mpi.lsh;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bucketizes entities by their LSH signatures. The construction is NOT 
 * thread-safe. 
 */
public class IntHashTable {
  private static final Logger logger = 
      LoggerFactory.getLogger(IntHashTable.class);

  private int l;

  private MinHashTable[] minhashTables;

  private TIntObjectHashMap<int[]> entitySignatures_;

  public IntHashTable(TIntObjectHashMap<int[]> entitySignatures, int lshBandSize, int lshBandCount) {
    // this.k = EntityLSHRepresentation.k;
    
    logger.info(entitySignatures.size() + " sigs, " + lshBandSize + ", " + lshBandCount);

    // this param need to match the ones the signatures were created with!
    this.l = lshBandCount;
    int k = lshBandSize;
    
    
//    if (l % k != 0) {
//      logger.warn("l is not a multiple of k, this must not happend!");
//    }
    boolean checkLimitation = false;
    
        
    // pre-aggregate the signatures, k is assumed to be one, sum up all values in each band
    entitySignatures_ = new TIntObjectHashMap<int[]>();
    
    for (int eId : entitySignatures.keys()) {
      int[] sig = entitySignatures.get(eId);
      
      if(!checkLimitation){
    	  if(k *l > sig.length)
    		  logger.error("Requested signature length is not available. " +
    		                k + " x " + l + " > " + sig.length);
    	  checkLimitation = true;
      }
      
      int[] aggSig = new int[l];
      
      for (int i=0;i<aggSig.length;i++) {
        int sum = 0;
        int start = k*i;
        for (int j=start;j<start+k;j++) {
          sum += sig[j];
        }
        aggSig[i] = sum;
      }
      
      entitySignatures_.put(eId, aggSig);
    }

    // Array of l minhash tables
    this.minhashTables = new MinHashTable[l];
    for (int i = 0; i < minhashTables.length; i++) {
      this.minhashTables[i] = new MinHashTable(i);
    }
  }

  public TIntHashSet deduplicate(int entity) {
    TIntHashSet union = new TIntHashSet();

    TIntHashSet bucket;
    for (int i = 0; i < minhashTables.length; i++) {
      if ((bucket = (TIntHashSet) minhashTables[i].getBucket(entity)) != null) {
        union.addAll(bucket);
      }
    }

    return union;
  }

  public void put(int entity) {
    for (int i = 0; i < minhashTables.length; i++)
      minhashTables[i].put(entity);
  }

  public void put(Collection<Integer> entities) {
    for (int entity : entities) {
      if (entitySignatures_.contains(entity)) {
        for (int i = 0; i < minhashTables.length; i++) {
          minhashTables[i].put(entity);
        } 
      }
      else {
        logger.warn("No signatures for entity id " + entity);
      }
    }
  }

  /**
   * This will create a map from the entity id (int) to
   * all entities it shares a bucket with in the LSH table.
   * 
   * As the relation is undirected, the map will be ordered:
   * the relation between 2 and 1 will be accessible by
   * 1 -> {...,2,...}
   * 
   * @return
   */
  public TIntObjectHashMap<TIntHashSet> getAllRelatedPairs() {    
    TIntObjectHashMap<TIntHashSet> allRelatedPairs = new TIntObjectHashMap<TIntHashSet>();
    // HashSet<String> bucket;
    for (int i = 0; i < minhashTables.length; i++) {
      for (int bucketid : minhashTables[i].getBucketIds()) {
        TIntHashSet bucket = minhashTables[i].getBucketById(bucketid);
        int[] entities = bucket.toArray();
        Arrays.sort(entities);

        for (int u = 0; u < entities.length; u++) {
          for (int v = u + 1; v < entities.length; v++) {
           
            // might check Jaccard here
            TIntHashSet related = allRelatedPairs.get(entities[u]);

            if (related == null) {
              related = new TIntHashSet();
              allRelatedPairs.put(entities[u], related);
            }

            System.out.println("REL: " + entities[u] + " -> " + entities[v]);
            related.add(entities[v]);
          }
        }
      }
    }
    
    return allRelatedPairs;
  }

  private class MinHashTable {

    private int id;
    
    private TIntObjectHashMap<TIntHashSet> buckets;

    // Draw k random permutations for each table
    private MinHashTable(int id) {
      this.id = id;
      
      buckets = new TIntObjectHashMap<TIntHashSet>();
    }

    public int[] getBucketIds() {
      return buckets.keys();
    }

    private void put(int entity) {      
      int hashCode = entitySignatures_.get(entity)[id];

      TIntHashSet bucket = buckets.get(hashCode);
      if (bucket == null) {
        bucket = new TIntHashSet();
        buckets.put(hashCode, bucket);
      }
      bucket.add(entity);
      System.out.println(entity);
      System.out.println(id + " " + hashCode);
    }

    private TIntHashSet getBucket(int entity) {
//    	if(entitySignatures.contains(entity) == false){
//    		System.out.println("entitySignatures does not contain key \"" + entity + "\"");
//    		return new TIntHashSet();
//    	}
      return buckets.get(entitySignatures_.get(entity)[id]);
    }

    public TIntHashSet getBucketById(int bucketid) {
      return buckets.get(bucketid);
    }
  }
}