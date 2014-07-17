package mpi.lsh.intfeature;

import java.util.HashMap;

import mpi.lsh.Counter;
import mpi.lsh.utils.Common;

public class LSH_main {

	static int keys = 1;
	static HashMap<String, Integer> shingles = new HashMap<String, Integer>();

	public static void main(String[] args) {

		String[] s = { "Frank_Sinatra", "Frank_M._Sinatra", "Franky_Sinatra",
				"Frankcis Sinatra" };

		LSHTable.CONFIDENCE_THRESHOLD = 0.8;
		LSHTable lsh = new LSHTable(4, 8, 128, 128);

		for (int i = 0; i < s.length; i++)
			lsh.put(Common.getCounter(s[i]));
		
		Counter counter1 = Common.getCounter(s[0]);
		Counter counter2 = Common.getCounter(s[2]);
		
		System.out.println(LSHTable.getJaccard(counter1, counter2, 0));
		
		for(String name: lsh.deduplicate(Common.getCounter("Frank_Sinatra")))
		  System.out.println(name);
	}

}
