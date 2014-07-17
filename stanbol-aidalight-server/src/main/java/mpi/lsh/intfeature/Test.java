package mpi.lsh.intfeature;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import mpi.lsh.utils.Common;

public class Test extends TestCase{
	String str[] = {"the president of the US", "the US president", "US president", "the president of A", "the president of B"};
	TIntObjectHashMap<int[]> keyphraseTokens = null;
	static Map<String, Integer> tokens = null;
	
	private void init(){
		keyphraseTokens = new TIntObjectHashMap<int[]>();
		
		tokens = new HashMap<String, Integer>();
		for(int i = 0; i < str.length; i++){
			TIntHashSet bag = new TIntHashSet();
			String[] toks = str[i].split(" ");
			
			for(String tok: toks){
				Integer tmp = tokens.get(tok); 
				if(tmp != null)
					bag.add(tmp.intValue());
				else{
					bag.add(tokens.size());
					tokens.put(tok, tokens.size());
				}
			}
			
			keyphraseTokens.put(i, bag.toArray());
		}
	}
	
//	private void monitor(){
//		init();
//		for(int i = 0; i < str.length; i++){
//			System.out.print(i + ":");
//			int[] toks = keyphraseTokens.get(i);
//			for(int tok: toks)
//				System.out.print("\t" + tok);
//			System.out.println();
//		}
//	}
	
	int[] a = {87, 14, 191, 31};
	int[] b = {134, 134, 37, 169};
	int[][] toks = {{3, 2, 1, 0}, {3, 1, 0}, {3, 1}, {4, 2, 1, 0}, {5, 2, 1, 0}};
	
	
	// calculate bucketIds, assume k = 2, l = 2
	private int[] calBucketIds(int[] toks){
		int k = 2, l = 2;
		int D = 199;
		int[] bids = new int[l];
		for(int i = 0; i < l; i++){
			int tmpbid = 0;
			for(int j = 0; j < k; j ++){
				int index = i * l + j;
				int min = Integer.MAX_VALUE;
				for(int tok: toks){
					int tmp = (a[index] * tok + b[index]) % D;
					if(min > tmp)
						min = tmp;
					
				}
				// simple hash code function
				tmpbid += min;
			}
			bids[i] = tmpbid;
		}
		return bids;
	}
	
	public void test(){
		int l = 2;
		init();
		
		int bids[][] = new int[str.length][2];
		for(int i = 0; i < str.length; i ++){
			bids[i] = calBucketIds(keyphraseTokens.get(i)); 
		}
		
		// do hashing
		IntLSHTable lsh = new IntLSHTable(2, 2, 100, 199);
		for(int i = 0; i < str.length; i++)
			lsh.put(Common.getIntCounter(i, keyphraseTokens.get(i)));

		
		for(int i = 0; i < str.length; i ++){
			// get similar kp by lsh
			TIntHashSet src = lsh.deduplicate(Common.getIntCounter(i, keyphraseTokens.get(i)));
			
			TIntHashSet dst = new TIntHashSet();
			for(int j = 0; j < str.length; j ++)
				if(i != j){
					for(int t = 0; t < l; t++){
						if(bids[i][t] == bids[j][t]){
							dst.add(j);
							break;
						}
					}
				}
			
			assertTrue(src.equals(dst));
		}
	}
	
		
//	public static void main(String args[]){
//		/**
//		 * print out token ids
//		 */
////		new Test().monitor();
////		0:	3	2	1	0
////		1:	3	1	0
////		2:	3	1
////		3:	4	2	1	0
////		4:	5	2	1	0
//
//		
//		/**
//		 * print out a[], b[] in the lsh table
//		 */
////		IntLSHTable lsh = new IntLSHTable(2, 2, 100, 199);
////		87	134
////		14	134
////		191	37
////		31	169
//		
//	}
}
