package mpi.lsh.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javatools.util.FileUtils;
import mpi.lsh.Counter;
import mpi.lsh.intfeature.IntCounter;
import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

public class Common {
	// for n-grams of names
	private static HashMap<String, Integer> shingles = new HashMap<String, Integer>();

	private static int keys = 1;

	
	/*
	 * hashcode a block of k values
	 */
	private static int[] getHashCode(int[] signature) {
		int[] hashCodes = new int[Config.l];
		for (int i = 0; i < Config.l; i++) {
			hashCodes[i] = 0;
			for (int j = i * Config.k; j < (i + 1) * Config.k; j++)
				hashCodes[i] += signature[j];
		}
		return hashCodes;
	}
	
	
	public static Map<String, int[]> getEntitySignatures(String path) throws IOException {
		Map<String, int[]> entitySignatures = new HashMap<String, int[]>();

		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		String entity = "";

		while (true) {
			String line = bufReader.readLine();
			if (line == null || line == "")
				break;

			String str[] = line.split("\t");
			entity = str[0];

			int[] signatures = new int[str.length - 1];
			for (int i = 0; i < signatures.length; i++)
				signatures[i] = Integer.parseInt(str[i + 1]);

			entitySignatures.put(entity, getHashCode(signatures));
		}

		isr.close();
		fis.close();

		return entitySignatures;
	}
	
	
	/*
	 * this function is just for speeding up parameterTest
	 * instead of loading all entity-sgnatures, just load entity in entities set
	 */
	public static Map<String, int[]> getEntitySignatures(String path, List<String> entities) throws IOException {
		Map<String, int[]> entitySignatures = new HashMap<String, int[]>();
		
		Set<String> check = new HashSet<String>(entities);

		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		String entity = "";
		int counter = 0;

		while (true) {
//			if (++counter % 10000 == 0)
//				System.out.println(counter);

			String line = bufReader.readLine();
			if (line == null || line == "")
				break;

			String str[] = line.split("\t");
			entity = str[0];
			
			if(check.contains(entity)){
				int[] signatures = new int[str.length - 1];
				for (int i = 0; i < signatures.length; i++)
					signatures[i] = Integer.parseInt(str[i + 1]);

				entitySignatures.put(entity, getHashCode(signatures));
				if(++counter == entities.size())
					break;
			}
		}

		isr.close();
		fis.close();

		return entitySignatures;
	}
	
	
	
	/**
	 * just cho check hashcode function
	 * 
	 * 
	 * @param path
	 * @param entities
	 * @return
	 * @throws IOException
	 */
	public static Map<String, int[]> getEntitySignaturesbyConcatenation(String path, List<String> entities) throws IOException {
		Map<String, int[]> entitySignatures = new HashMap<String, int[]>();
		
		Set<String> check = new HashSet<String>(entities);

		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		String entity = "";
//		int counter = 0;

		while (true) {
//			if (++counter % 10000 == 0)
//				System.out.println(counter);

			String line = bufReader.readLine();
			if (line == null || line == "")
				break;

			String str[] = line.split("\t");
			entity = str[0];
			
			if(check.contains(entity)){
				int[] signatures = new int[str.length - 1];
				for (int i = 0; i < signatures.length; i++)
					signatures[i] = Integer.parseInt(str[i + 1]);

				int[] hashCodes = new int[Config.l];
				for (int i = 0; i < Config.l; i++) {
//					hashCodes[i] = 0;
					String code = "";
					for (int j = i * Config.k; j < (i + 1) * Config.k; j++)
//						hashCodes[i] += signature[j];
						code += String.valueOf(signatures[j]) + "$";
					hashCodes[i] = code.hashCode();
				}
				entitySignatures.put(entity, getHashCode(signatures));
			}
		}

		isr.close();
		fis.close();

		return entitySignatures;
	}
	
	
	public static Map<String, int[]> getEntityMIPs(String path) throws IOException {
		Map<String, int[]> entitySignatures = new HashMap<String, int[]>();

		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		String entity = "";
		int counter = 0;

		while (true) {
			counter++;
			if (counter % 10000 == 0)
				System.out.println(counter);

			String line = bufReader.readLine();
			if (line == null || line == "")
				break;

			String str[] = line.split("\t");
			entity = str[0];

			int[] signatures = new int[str.length - 1];
			for (int i = 0; i < signatures.length; i++)
				signatures[i] = Integer.parseInt(str[i + 1]);

			entitySignatures.put(entity, signatures);
		}

		isr.close();
		fis.close();

		return entitySignatures;
	}
	
	
	/**
	 * @param path
	 * @return a List of String. An element in the List responds to a line in
	 *         source file
	 * @throws IOException
	 */
	public static List<String> getContent(String path) throws IOException {
		System.out.println("Get content from " + path);
		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		List<String> str = new ArrayList<String>();
		String line;
		int counter = 0;
		while (true) {
			counter++;
			if (counter % 10000 == 0) {
//				System.out.println(counter);
				// break;
			}
			
			line = bufReader.readLine();
			if (line == "" || line == null)
				break;
			str.add(line);
		}
		System.out.println("Content from " + path + " is loaded!");

		isr.close();
		fis.close();

		return str;
	}
	
	// get names from person names
	// they might be first name or last name
	public static Set<String> getPersonName(String path) throws IOException {
		System.out.println("Get content from " + path);
		FileInputStream fis = new FileInputStream(path);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

		HashSet<String> names = new HashSet<String>();
		String line;
		int counter = 0;
		while (true) {
			counter++;
			if (counter % 10000 == 0) {
				System.out.println(counter);
				// break;
			}
			line = bufReader.readLine();
			if (line == "" || line == null)
				break;
			String[] str = line.split("_");

			// add first name
			names.add(str[0]);
			// add last name
			names.add(str[str.length - 1]);

		}
		System.out.println("Content from " + path + " is loaded!");

		isr.close();
		fis.close();

		return names;
	}

	/*
	 * write string "content" to file
	 */
	public static void write(String file, String content, boolean append)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(file, append);
		Writer out = new OutputStreamWriter(fos, "UTF-8");
		out.write(content);
		out.close();
		fos.close();
	}

	
	public static IntCounter getIntCounter(int keyphrase, int[] tokens){
		OpenIntIntHashMap entries = new OpenIntIntHashMap();
		int totalCount = 0;
		
		for(int i = 0; i < tokens.length; i++){
			if(entries.containsKey(tokens[i])){
				entries.put(tokens[i], entries.get(tokens[i]) + 1);
				totalCount++;
			}
			else{
				entries.put(tokens[i], 1);
				totalCount++;
			}
		}
		
		int[] keys = new int[entries.size()];
		IntArrayList l = entries.keys();
		for (int i = 0; i < entries.size(); i++)
			keys[i] = l.get(i);

		// sort keys arrays
		Arrays.sort(keys);
		int[] vals = new int[keys.length];
		for (int i = 0; i < keys.length; i++)
			vals[i] = entries.get(keys[i]);

		entries = null;
		return new IntCounter(keyphrase, keys, vals, totalCount);
	}
	
	public static Counter getCounterAtTokenLevel(String s) {
		// s = s.replaceAll("['-()*&^%$#@!;]","");
		OpenIntIntHashMap entries = new OpenIntIntHashMap();
		// Counter counter = new Counter(s);
		int totalCount = 0;
		StringTokenizer tokenizer = new StringTokenizer(s);
		while (tokenizer.hasMoreTokens()) {
			String tok = tokenizer.nextToken();
//			if (tok.length() < 3) {
				// if tok has less than 3 characters
				// then take tok as a shingle
				String shingle = tok;
				Integer key = shingles.get(shingle);
				if (key == null) {
					// System.out.println(shingle + " : " + keys);
					shingles.put(shingle, keys);
					// counter.incrementCount(keys);
					entries.put(keys, entries.get(keys) + 1);
					totalCount++;
					keys++;
				} else {
					// counter.incrementCount(key.intValue());
					entries.put(key.intValue(), entries.get(key.intValue()) + 1);
					totalCount++;
				}
				continue;
//			}
//			for (int i = 0; i < tok.length() - 2; i++) {
//				// String shingle = tok; //"";
//				String shingle = "";
//				for (int j = i; j < Math.min(tok.length(), i + 3); j++)
//					shingle += tok.charAt(j);
//				shingle = shingle.toLowerCase();
//				Integer key = shingles.get(shingle);
//				if (key == null) {
//					// System.out.println(shingle + " : " + keys);
//					shingles.put(shingle, keys);
//					// counter.incrementCount(keys);
//					entries.put(keys, entries.get(keys) + 1);
//					totalCount++;
//					keys++;
//				} else {
//					// counter.incrementCount(key.intValue());
//					entries.put(key.intValue(), entries.get(key.intValue()) + 1);
//					totalCount++;
//				}
//			}
		}
		// Counter counter = new Counter(s);
		// insert into counter from hashmap here...

		// return new Counter(s, entries, totalCount);

		int[] keys = new int[entries.size()];
		IntArrayList l = entries.keys();
		for (int i = 0; i < entries.size(); i++)
			keys[i] = l.get(i);

		// sort keys arrays
		Arrays.sort(keys);
		int[] vals = new int[keys.length];
		for (int i = 0; i < keys.length; i++)
			vals[i] = entries.get(keys[i]);

		entries = null;
		return new Counter(s, keys, vals, totalCount);
	}
	
	public static Counter getCounter(String s) {
		// s = s.replaceAll("['-()*&^%$#@!;]","");
	  String str = s.replaceAll("_", " ");
		OpenIntIntHashMap entries = new OpenIntIntHashMap();
		// Counter counter = new Counter(s);
		int totalCount = 0;
		StringTokenizer tokenizer = new StringTokenizer(str);
		while (tokenizer.hasMoreTokens()) {
			String tok = tokenizer.nextToken();
			if (tok.length() < 3) {
				// if tok has less than 3 characters
				// then take tok as a shingle
				String shingle = tok;
				Integer key = shingles.get(shingle);
				if (key == null) {
					// System.out.println(shingle + " : " + keys);
					shingles.put(shingle, keys);
					// counter.incrementCount(keys);
					entries.put(keys, entries.get(keys) + 1);
					totalCount++;
					keys++;
				} else {
					// counter.incrementCount(key.intValue());
					entries.put(key.intValue(), entries.get(key.intValue()) + 1);
					totalCount++;
				}
				continue;
			}
			for (int i = 0; i < tok.length() - 2; i++) {
				// String shingle = tok; //"";
				String shingle = "";
				for (int j = i; j < Math.min(tok.length(), i + 3); j++)
					shingle += tok.charAt(j);
				shingle = shingle.toLowerCase();
				Integer key = shingles.get(shingle);
				if (key == null) {
					// System.out.println(shingle + " : " + keys);
					shingles.put(shingle, keys);
					// counter.incrementCount(keys);
					entries.put(keys, entries.get(keys) + 1);
					totalCount++;
					keys++;
				} else {
					// counter.incrementCount(key.intValue());
					entries.put(key.intValue(), entries.get(key.intValue()) + 1);
					totalCount++;
				}
			}
		}
		// Counter counter = new Counter(s);
		// insert into counter from hashmap here...

		// return new Counter(s, entries, totalCount);

		int[] keys = new int[entries.size()];
		IntArrayList l = entries.keys();
		for (int i = 0; i < entries.size(); i++)
			keys[i] = l.get(i);

		// sort keys arrays
		Arrays.sort(keys);
		int[] vals = new int[keys.length];
		for (int i = 0; i < keys.length; i++)
			vals[i] = entries.get(keys[i]);

		entries = null;
		return new Counter(s, keys, vals, totalCount);
	}
	
	
	/*
	 * fix bug of creating signature file with unsorted file
	 */
	public static void fixBug(String src, String dst, int len) throws IOException {
//		FileInputStream fis = new FileInputStream(
//				"../data/keyphrases/entityMIPs20byKeyphrasesClustering_6_2");
		FileInputStream fis = new FileInputStream(src);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader bufReader = new BufferedReader(isr);

//		int len = 300;

		Map<String, int[]> check = new HashMap<String, int[]>();

		// String entity = "";
		int counter = 0;

		// String content = "";

		while (true) {
			// if (++counter % 1000 == 0)
			// System.out.println(counter);

			// if (counter == 200)
			// break;

			if (++counter % 1000 == 0)
				System.out.println(counter);

			String line = bufReader.readLine();
			if (line == null || line == "")
				break;

			String[] str = line.split("\t");
			// content += str[0] + "\t";

			int[] tmp = new int[len];
			for (int i = 0; i < len; i++)
				tmp[i] = Integer.parseInt(str[i + 1]);

			if (check.containsKey(str[0])) {
				int[] currentResult = check.get(str[0]);
				int[] update = new int[len];
				for (int i = 0; i < len; i++)
					update[i] = Math.min(tmp[i], currentResult[i]);
				check.put(str[0], update);
			} else
				check.put(str[0], tmp);
		}

		// Common.write("../data/keyphrases/entityMIPs20AtToken", content,
		// true);

		isr.close();
		fis.close();

		BufferedWriter writer = FileUtils.getBufferedUTF8Writer(new File(dst));
		
		counter = 0;
		StringBuilder content = new StringBuilder();
		for (String str : check.keySet()) {
			if (++counter % 1000 == 0) {
				System.out.println(counter);
//				Common.write(
//						"../data/keyphrases/newentityMIPs20byKeyphrasesClustering_6_2",
//						content, true);
//				Common.write(dst, content, true);
				writer.write(content.toString());
				content = new StringBuilder();
			}
			content.append(str).append("\t");
			int[] tmp = check.get(str);
			for (int i = 0; i < len; i++)
				content.append(tmp[i]).append("\t");
			content.append("\n");
		}
		
		writer.flush();
		writer.close();
	}
	

	/*
	 * map from a name to a list of entites that might relate to it.
	 */
//	public static Map<String, List<String>> getNameToEntities()
//			throws IOException {
//		FileInputStream fis = new FileInputStream(Config.MEANS);
//		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//		BufferedReader bufReader = new BufferedReader(isr);
//
//		HashMap<String, List<String>> nameToEntities = new HashMap<String, List<String>>();
//		String line;
//		int counter = 0;
//		while (true) {
//			line = bufReader.readLine();
//			if (line == null || line == "")
//				break;
//
//			// System.out.println(line);
//			if (++counter % 100 == 0)
//				System.out.println(counter);
//			String key, value;
//
//			String str[] = line.split("\t");
//			if (str[1].startsWith("\""))
//				key = str[1].substring(1, str[1].length() - 1);
//			else
//				key = str[1];
//			value = str[2];
//
//			// int begin = line.indexOf("\"") + 1;
//			// if (begin != 0) {
//			// int end = line.indexOf("\"", begin);
//			// if (end != -1 && end + 2 < line.length()) {
//			// key = line.substring(begin, end);
//			// value = line.substring(end + 2);
//			// }
//			//
//			// else {
//			// String str[] = line.split("\t");
//			// key = str[1];
//			// value = str[2];
//			// }
//			// } else {
//			// String str[] = line.split("\t");
//			// key = str[1];
//			// value = str[2];
//			// }
//
//			List<String> vals = nameToEntities.get(key);
//			if (vals == null) {
//				vals = new ArrayList<String>();
//				vals.add(value);
//				nameToEntities.put(key, vals);
//			} else
//				vals.add(value);
//
//		}
//
//		isr.close();
//		fis.close();
//
//		System.out.println("loaded from means file!");
//		System.out.println("updating for person names...");
//
//		List<String> personEntities = getContent(Config.PERSON_ENTITIES);
//		for (String entity : personEntities) {
//			String[] str = entity.split("_");
//
//			// first name
//			List<String> updateFirstName = nameToEntities.get(str[0]);
//			if (updateFirstName == null) {
//				updateFirstName = new ArrayList<String>();
//				updateFirstName.add(entity);
//				nameToEntities.put(str[0], updateFirstName);
//			} else {
//				updateFirstName.add(entity);
//			}
//
//			// last name
//			List<String> updateLastName = nameToEntities
//					.get(str[str.length - 1]);
//			if (updateLastName == null) {
//				updateLastName = new ArrayList<String>();
//				updateLastName.add(entity);
//				nameToEntities.put(str[str.length - 1], updateLastName);
//			} else {
//				updateLastName.add(entity);
//			}
//		}
//
//		System.out.println("Map from name to entities built!");
//
//		return nameToEntities;
//	}

	// map from an entity to its key phrases
//	public static HashMap<String, int[]> getEntityKeyPhrasesMap()
//			throws IOException {
//		FileInputStream fis = new FileInputStream(Config.KEYPHRASES_VECTOR);
//		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//		BufferedReader bufReader = new BufferedReader(isr);
//
//		HashMap<String, int[]> entityToKeyPhrases = new HashMap<String, int[]>();
//		String line;
//		int counter = 0;
//		while (true) {
//			if (++counter % 10000 == 0)
//				System.out.println(counter);
//			line = bufReader.readLine();
//			if (line == "" || line == null)
//				break;
//
//			String str[] = line.split("\t");
//			str[0] = str[0].substring(0, str[0].length() - 1);
//			int[] keySet = new int[str.length - 1];
//			for (int i = 1; i < str.length; i++)
//				keySet[i - 1] = Integer.parseInt(str[i]);
//
//			entityToKeyPhrases.put(str[0], keySet);
//
//		}
//		isr.close();
//		fis.close();
//
//		System.out.println("Loaded!");
//
//		return entityToKeyPhrases;
//	}
//
//	// public static void loadConfig() throws IOException {
//	// Properties myProps = new Properties();
//	// FileInputStream MyInputStream = new FileInputStream(
//	// "./config.properties");
//	// myProps.load(MyInputStream);
//	// String myPropValue = myProps.getProperty("NAMES_FROM_YAGO");
//	// System.out.println(myPropValue);
//	// // String key = "";
//	// // String value = "";
//	// // for (Map.Entry<Object, Object> propItem : myProps.entrySet()) {
//	// // key = (String) propItem.getKey();
//	// // value = (String) propItem.getValue();
//	// // }
//	//
//	// }
//
	public static void main(String args[]) throws IOException {
		// loadConfig();
//		getNameToEntities();
		Common.fixBug(args[0] + "_tmp", args[0], 1000);
	}
}
