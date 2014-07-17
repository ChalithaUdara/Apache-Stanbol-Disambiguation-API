package mpi.lsh.intfeature;


public class Utils {
//	private static final Logger logger = 
//      LoggerFactory.getLogger(Utils.class);
//  
////	private static String keyphraseDictionary;// = "./data/new_data/keyphrase_dictionary";
////	
////	private static String entitySignatures;// = "./data/entity_keyphrases/new_format_entitySignature_Token_2_2"; // entitySignaturebyKeyphrasesClustering;
////	
////	private static String entityMIPs;// = "./data/entity_keyphrases/new_format_entityMIPs_1000"; // entitySignaturebyKeyphrasesClustering;
//	
//	private static void buildKeyphraseDictionary(String keyphraseDictionary) throws IOException{
//		if(keyphraseDictionary == null || keyphraseDictionary.equalsIgnoreCase("")){
//			System.out.println("Keyphrase Dictionary path is not set!");
//			return;
//		}
//		AidaManager.init();
//		  
//		DBConnection con = null;
//		
//		Set<String> dic = new HashSet<String>();
//		
//		int counter = 0;
//		try {
//			String ENTITY_KEYPHRASES= "entity_keyphrases";
//			con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA, "Getting All Yago Entities with Keyphrases");
//			DBStatementInterface statement = con.getStatement();
//			con.setAutoCommit(false);
//			statement = con.getStatement();
//			statement.setFetchSize(100000);
//
////			String sql = "SELECT entity,keyphrase FROM " + ENTITY_KEYPHRASES;
//			String sql = "SELECT keyphrase FROM " + ENTITY_KEYPHRASES;
//			ResultSet rs = statement.executeQuery(sql);
//
//			while (rs.next()) {
//				if(++counter % 1000 == 0)
//					System.out.println(counter);
////				String e = rs.getString("entity");
//			    String kp = rs.getString("keyphrase");
//			    // YOUR CODE GOES HERE
////			    System.out.println(e + ": " + kp);
//			    dic.add(kp);
//			}
//
//			rs.close();
//			statement.commit();
//			} catch (Exception e) {
//				logger.error(e.getLocalizedMessage());
//			} finally {
//				AidaManager.releaseConnection(AidaManager.DB_AIDA, con);
//			}
//		
//		System.out.println("Start writing dictionary...");
//		
//		StringBuilder content = new StringBuilder();
//	    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(new File(keyphraseDictionary));
//	    
//	    counter = 0;
//	    for(String str: dic){
//	    	if(++counter % 10000 == 0){
//	    		System.out.println(counter);
//	    		writer.write(content.toString());
//	            content = new StringBuilder();
//	    	}
//	    	content.append(str).append("\n");
//	    }
//	    
//	    writer.write(content.toString());
//	    writer.flush();
//	    writer.close();
//
//	}
//	
//	private static void createEntitySignaturesFromKeyphrasesClustering(String keyphraseDictionary, String entitySignatures) throws IOException {
////	    List<String> keyphrases = getKeyphrases();
//	    //		List<String> keyphrases = Common
//	    //				.getContent("./data/entity_keyphrases/smallSet");
//		
//		List<String> keyphrases = Common
//			    				.getContent(keyphraseDictionary);
//
////	    String entitySignatures = "./data/entity_keyphrases/entitySignaturebyKeyphrasesClustering_Token_2_2"; // entitySignaturebyKeyphrasesClustering
////	    String keyphrasesPath = "./data/entity_keyphrases/keyphrases.csv"; //keyphrases.csv file
//
//	    //		System.out.println(8 + "\t" + 2);
//	    LSHTable lsh = new LSHTable(2, 2, 4096, 99999999);
//	    // do hashing
//
//	    int counter = 0;
//	    for (String keyphrase : keyphrases) {
//	      if (++counter % 10000 == 0) System.out.println(counter);
//
//	      //			if(counter == 2000)
//	      //				break;
//
//	      lsh.put(Common.getCounterAtTokenLevel(keyphrase));
//	    }
//
//	    Map<String, Set<Integer>> keyphraseToBucketID = lsh.getKeyphraseToBucketID();
//	    
//	    //		for(String str: keyphraseToBucketID.keySet()){
//	    //			System.out.println(str + "--------" );
//	    //			Set<Integer> set = keyphraseToBucketID.get(str);
//	    //			for(Integer val: set)
//	    //				System.out.print(val + "\t");
//	    //			System.out.println();
//	    //		}
//
//	    
//	    
//	    AidaManager.init();
//		  
//		DBConnection con = null;
//		
//		Map<String, List<String>> entityToKeyphrases = new HashMap<String, List<String>>();
//		
//		counter = 0;
//		try {
//			String ENTITY_KEYPHRASES= "entity_keyphrases";
//			con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA, "Getting All Yago Entities with Keyphrases");
//			DBStatementInterface statement = con.getStatement();
//			con.setAutoCommit(false);
//			statement = con.getStatement();
//			statement.setFetchSize(100000);
//
//			String sql = "SELECT entity,keyphrase FROM " + ENTITY_KEYPHRASES;
////			String sql = "SELECT keyphrase FROM " + ENTITY_KEYPHRASES;
//			ResultSet rs = statement.executeQuery(sql);
//
//			while (rs.next()) {
//				if(++counter % 1000 == 0)
//					System.out.println(counter);
//				String e = rs.getString("entity");
//			    String kp = rs.getString("keyphrase");
//			    // YOUR CODE GOES HERE
////			    System.out.println(e + ": " + kp);
//			    
//			    List<String> tmp = entityToKeyphrases.get(e);
//			    if(tmp == null) {
//			    	tmp = new ArrayList<String>();
//	          entityToKeyphrases.put(e, tmp);
//			    }
//			    tmp.add(kp);
//			}
//
//			rs.close();
//			statement.commit();
//			} catch (Exception e) {
//				logger.error(e.getLocalizedMessage());
//			} finally {
//				AidaManager.releaseConnection(AidaManager.DB_AIDA, con);
//			}
//	    
//	    
//	    
////	    FileInputStream fis = new FileInputStream(keyphrasesPath);
////	    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
////	    BufferedReader bufReader = new BufferedReader(isr);
//
////	    String entity = "";
//	    // String vector = "";
//	    //		String keyphrase = "";
//	    counter = 0;
//
//	    /**
//	     * write in format of "entity \t number_of_keys"
//	     * 
//	     * (weights) \t key
//	     */
//	    StringBuilder content = new StringBuilder();
//	    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(new File(entitySignatures));
//
////	    Map<Integer, Integer> mp = new HashMap<Integer, Integer>(); //mp.keys is signature vector of an entity
//	    boolean flag = true;
//
//	    List<String> entityList = new ArrayList<String>();
//	    
//	    for(String entity: entityToKeyphrases.keySet()) {
//	    	if(++counter % 50000 == 0 || counter == entityToKeyphrases.keySet().size()){
//	    		WeightedKeyphrasesContext wkc = null;
//			    
//			    Entities entitySet = new Entities();
//			    
//			    for(String en: entityList)
//			    	entitySet.add(new Entity(en, DataAccess.getIdForYagoEntityId(en)));
//				
//			    try{
//			    	wkc = new WeightedKeyphrasesContext(entitySet);
//			    	
//			    	//delete entities that can not calculate weight
////			    	List<String> tmp = new ArrayList<String>(entities);
////			    	for(String entity: tmp)
////			    		if(wkc.getEntityKeyphraseIds(entity).length == 0)
////			    			entities.remove(entity);
//			    }catch(Exception e){
//			    	e.printStackTrace();
//			    }
//		    	
//		    	
////		        if (++counter % 10 == 0) {
////		          System.out.println(counter);
////		          writer.write(content.toString());
////		          content = new StringBuilder();
////		        }
//			    
//        for (Entity en : entitySet) {
//          Map<Integer, Integer> mp = new HashMap<Integer, Integer>();
//          Map<Integer, Double> keyphrase_weight = new HashMap<Integer, Double>();
//
//          int[] tmp = wkc.getContext(en);
//
//          for (int phrase : tmp) {
//            Set<Integer> ids = keyphraseToBucketID.get(phrase);
//            if (ids == null) {
//              flag = false;
//              continue;
//            }
//            for (int id : ids) {
//              Integer i = mp.get(id);
//              if (i == null) {
//                mp.put(id, 1);
//                keyphrase_weight.put(id, wkc.getCombinedKeyphraseMiIdfWeight(en, phrase));
//              } else {
//                mp.put(id, i + 1);
//                keyphrase_weight.put(id, keyphrase_weight.get(id) + wkc.getCombinedKeyphraseMiIdfWeight(en, phrase));
//              }
//
//              //					            System.out.println("entity: " + en + "\t phrase: " + phrase + keyphrase_weight.get(id));
//            }
//
//          }
//			        				        
//			        content.append(en).append("\t").append(tmp.length).append("\n");
//			        
//			        for(Integer id: mp.keySet()){
//			        	content.append(keyphrase_weight.get(id) / mp.get(id)).append("\t").append(id).append("\n");
//			        }
//			    }
//			    
//			    System.out.println(counter);
//		        writer.write(content.toString());
//		        content = new StringBuilder();
//		        
//		        entityList = new ArrayList<String>();
//	    	}
//	    	else
//	    		entityList.add(entity);
//	        
//	    }
//
////	    writer.write(content.toString());
////	    writer.flush();
////	    writer.close();
//	    
//	    if (!flag) System.out.println("NULL ids");
//	}
//	
//	private static int getPrime(int n) {
//	    while (!isPrime(n))
//	      n++;
//	    return n;
//	}
//
//	private static boolean isPrime(int n) {
//	    if (n <= 2) return n == 2;
//	    else if (n % 2 == 0) return false;
//	    for (int i = 3, end = (int) Math.sqrt(n); i <= end; i += 2)
//	      if (n % i == 0) return false;
//	    return true;
//	}
//	
//	private static void createEntityMIPs(String src, String dst, int dimentions, int numberOfHashFucntions) throws IOException, InterruptedException {
//	    // generate min-hash functions
//	    int a[] = new int[numberOfHashFucntions];
//	    int b[] = new int[numberOfHashFucntions];
//
//	    int D = getPrime(dimentions);
//	    
//	    Random r = new Random(42);
//
//	    for (int i = 0; i < numberOfHashFucntions; i++) {
//	      a[i] = 1 + (int) Math.floor(r.nextDouble() * (D - 1));
//	      b[i] = (int) Math.floor(r.nextDouble() * D);
//	    }
//
//	    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "UTF-8"));
//
//	    // set up buffers and processors
////	    BlockingQueue<Pair<String, int[]>> queue = new LinkedBlockingQueue<Pair<String, int[]>>(10000);
//	    BlockingQueue<Pair<String, List<Pair<Integer, Double>>>> queue = new LinkedBlockingQueue<Pair<String, List<Pair<Integer, Double>>>>(10000);
//	    List<SigComputer> scs = new LinkedList<SigComputer>();
//
//	    // start 40 SigComputers
//	    for (int i = 0; i < 40; i++) {
//	      SigComputer sc = new SigComputer(i, numberOfHashFucntions, a, b, D, queue, writer);
//	      scs.add(sc);
//	      sc.start();
//	    }
//
//	    FileInputStream fis = new FileInputStream(src);
//	    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//	    BufferedReader bufReader = new BufferedReader(isr);
//	    
//	    int counter = 0;
//	    
//	    while (true) {
//	      if (++counter % 1000 == 0) {
//	        System.out.println(counter);
//	      }
//	      
//	      String line = bufReader.readLine();
//	      if (line == null || line == "")
//	        break;
//	      String str[] = line.split("\t");
//	      String entity = str[0];
//	      int numberKeys = Integer.parseInt(str[1]);
////	      int[] keys = new int[numberKeys];
//	      List<Pair<Integer, Double>> keys = new ArrayList<Pair<Integer,Double>>();
//
//	      for (int i = 0; i < numberKeys; i++) {
//	        line = bufReader.readLine();
//	        String key[] = line.split("\t");
//
////	        if (key.length > 1) keys[i] = Integer.parseInt(key[1]);
////	        else keys[i] = Integer.parseInt(key[0]);
//	        Pair<Integer, Double> tmp = new Pair<Integer, Double>();
//	        tmp.setFirst(Integer.parseInt(key[1]));
//	        tmp.setSecond(Double.parseDouble(key[0]));
//	        keys.add(tmp);
//	      }
//	      
//	      queue.put(new Pair<String, List<Pair<Integer, Double>>>(entity, keys));      
//	    }
//
//	    // Send stop packets for all scs
//	    for (SigComputer sc : scs) {
//	      queue.put(new Pair<String, List<Pair<Integer, Double>>>(null, null));
//	    }    
//	    
//	    // wait for all SigComputers to finish
//	    for (SigComputer sc : scs) {
//	      sc.join();
//	    }    
//	    
////	    Common.write(dst, content, true);
//	    writer.flush();
//	    writer.close();
//
//	    isr.close();
//	    fis.close();
//	}  
//	
//	
//	
//	public static void main(String args[]) throws IOException, InterruptedException{
////		System.out.println("build weighted files...");
//		if(args.length > 0){
//			//step 1: create a file consists of keyphrases
//			//args[1] is the path to the file where we wanna store keyphrases
//			if(args[0].equalsIgnoreCase("dic"))
//				buildKeyphraseDictionary(args[1]);
//			
//			//step 2: create entity signature file
//			//args[1] is the path to the file containing keyphrases
//			//args[2] is the path to entitySignatures file
//			if(args[0].equalsIgnoreCase("sig"))
//				createEntitySignaturesFromKeyphrasesClustering(args[1], args[2]);
//			
//			//step 3: create MIPs file
//			//args[1] is the path to entitySignatures file
//			//args[2] is the path to entityMIPs file
//			else if(args[0].equalsIgnoreCase("mips"))
//				createEntityMIPs(args[1], args[2], 1999999, 1000);
//			
//			else if(args[0].equalsIgnoreCase("all")){
//				buildKeyphraseDictionary(args[1]);
//				createEntitySignaturesFromKeyphrasesClustering(args[1], args[2]);
//				createEntityMIPs(args[2], args[3], 1999999, 1000);
//			}
//		}
//	}
}
