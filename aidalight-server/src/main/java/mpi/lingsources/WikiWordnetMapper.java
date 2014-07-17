package mpi.lingsources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aidalight.DataStore;
import mpi.util.Utils;

public class WikiWordnetMapper {
	private static Map<String, String> wikiCategoryToWordnetCategory = null;
	private static Map<String, String> subclass = null;
	
	private static Map<String, List<String>> wordnetDomain = null;
	
	//remove noise manully
  public static String noise[] = {"wikicategory_Living_people", "wordnet_person_100007846"};
	
	public WikiWordnetMapper(){
		
	}
	
	public static void init() throws IOException{
	  loadYagoTaxonomy();
	  loadWordnetDomains();
	}
	
	public static String standardize(String str){
		return str.substring(1, str.length()-1);
	}
	
	private static void loadYagoTaxonomy() throws IOException{
		System.out.print("Loading wiki-wordnet mapper...");
		wikiCategoryToWordnetCategory = new HashMap<String, String>();
		subclass = new HashMap<String, String>();
//		DataInputStream in = new DataInputStream(new FileInputStream(Utils.nedProperties.getProperty("yagoTaxonomy")));
//		Configuration conf = new Configuration();
//		FileSystem fs = FileSystem.get(conf);
//		Path inFile = new Path(Utils.getProperty("yagoTaxonomy"));
//		if (!fs.exists(inFile)){
//			throw new IOException("Input file not found");
//		}
//		FSDataInputStream in = fs.open(inFile);
//		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//		String line;
//		while (true) {
//			line = reader.readLine();
//			if (line == "" || line == null)
//				break;
//			String[] str = line.split("\t");
//			if(str[0].length() > 0)
//				wikiCategoryToWordnetCategory.put(standardize(str[1]), standardize(str[3]));
//			else
//				subclass.put(standardize(str[1]), standardize(str[3]));
//			if(line.indexOf("wikicategory_Bangu_") != -1)
//        System.out.println(line + "\t" + wikiCategoryToWordnetCategory.get(standardize(str[1])));
//		}
//		reader.close();
//		in.close();
		
		for(String line: Utils.getContent(Utils.getProperty("yagoTaxonomy"))) {
		  String[] str = line.split("\t");
		  if(str[0].length() > 0)
	      wikiCategoryToWordnetCategory.put(standardize(str[1]), standardize(str[3]));
	    else
	      subclass.put(standardize(str[1]), standardize(str[3]));
		}
		
		for(String str: noise) {
		  System.out.print("fix bug with key: " + str + "... ");
		  wikiCategoryToWordnetCategory.remove(str);
		}
//		System.out.print("fix bug with key: wikicategory_Association_football_Forwards due to wrong map (baseket_ball)... ");
//		wikiCategoryToWordnetCategory.remove("wikicategory_Association_football_Forwards");
//		wikiCategoryToWordnetCategory.remove("wikicategory_Association_football_forwards");
		
//		wikiCategoryToWordnetCategory.put("wikicategory_Living_people", "wordnet_person_100007846"); // still need it to classify type of an entity.
		System.out.println("Done!");
	}
	
	private static void loadWordnetDomains() throws IOException{
		System.out.print("Loading wordnet domains...");
		wordnetDomain = new HashMap<String, List<String>>();
//		DataInputStream in = new DataInputStream(new FileInputStream("D:/Study/PhD Work/workspace/lkfinder3/data/link_resources/yagoWordnetDomains.tsv"));
//		Configuration conf = new Configuration();
//		FileSystem fs = FileSystem.get(conf);
//		Path inFile = new Path(Utils.getProperty("bnedWordnetDomains"));
//		if (!fs.exists(inFile)){
//			throw new IOException("Input file not found");
//		}
//		FSDataInputStream in = fs.open(inFile);
//		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//		String line;
//		while (true) {
//			line = reader.readLine();
//			if (line == "" || line == null)
//				break;
//			
//			String[] str = line.split("\t");
//			List<String> tmp = wordnetDomain.get(standardize(str[1]));
//			if(tmp == null){
//				tmp = new ArrayList<String>();
//				tmp.add(standardize(str[3]));
//				wordnetDomain.put(standardize(str[1]), tmp);
//			}
//			else
//				tmp.add(standardize(str[3]));
//		}
//		reader.close();
//		in.close();
		for(String line: Utils.getContent(Utils.getProperty("bnedWordnetDomains"))) {
		  String[] str = line.split("\t");
      List<String> tmp = wordnetDomain.get(standardize(str[1]));
      if(tmp == null){
        tmp = new ArrayList<String>();
        tmp.add(standardize(str[3]));
        wordnetDomain.put(standardize(str[1]), tmp);
      }
      else
        tmp.add(standardize(str[3]));
		}
		
	// remove noise manully
    String noise[] = {"wordnet_person_100007846", "wordnet_forward_110105733"};
    for(String str: noise) {
      System.out.print("fix bug with key: " + str + "... ");
      wordnetDomain.remove(str);
    }
    List<String> tmp = new ArrayList<String>();
    tmp.add("wordnetDomain_person");
    wordnetDomain.put("wordnet_person_100007846", tmp);
		System.out.println("Done!");
	}  
	
	
	/**
	 * This suffers the lack of mapping in yagoTaxonomy
	 * E.g. wikicategory_Auburn_Tigers_basketball_broadcasters not map to wornet_person, 
	 * but only to wordnet_broadcasting_company_108002015
	 * @param subType
	 * @return Root type (e.g. Person, Location, Organization or MISC).
	 */
	public static String getType(String subType){
	  if(subType.equalsIgnoreCase("yagoGeoEntity"))
	    return "LOCATION";
	  for(String str: noise)
	    if(subType.equalsIgnoreCase(str))
	      return "PERSON";
	  
		String type = getWordnetTypeFromWikiCategory(subType);
		if(type == null) {
		  // TODO: might find nearest wikicategory with this to map
		  String tmp = DataStore.getNearestString(subType);
		  if(tmp == null)
		    return null;
		  
		  type = getWordnetTypeFromWikiCategory(tmp);
		  System.out.println("Update wiki-wordnet map with key: " + subType + "\t value: " + type);
		  wikiCategoryToWordnetCategory.put(subType, type);
		}
		while(true){
			if(type == null)
				break;
			
			type = type.toLowerCase();
			if(type.indexOf("person") != -1)
			  return "PERSON";
			if(type.indexOf("location") != -1 || type.indexOf("yagoGeoEntity") != -1)
				return "LOCATION";
			if(type.indexOf("organization") != -1)
				return "ORGANIZATION";
			// trace back to father type.
			type = subclass.get(type);
		}
		return "MISC";
	}
	
	
	/**
	 * Map from wikicategory to root types (person, organization, etc.).
	 * @param types
	 * @return
	 */
	public static Set<String> getTypes(Set<String> types){
		Set<String> res = new HashSet<String>();
		for(String type: types){
		  String tmp = WikiWordnetMapper.getType(type);
		  if(tmp != null)
		    res.add(tmp);
		}
		return res;
	}
	
	
	public static List<String> getWordnetDomainFromWordnetCategory(String wordnetCategory){
	  try {
      if(wordnetDomain == null)
        loadWordnetDomains();
    } catch (IOException e){
      e.printStackTrace();
    }
    
	  List<String> result = wordnetDomain.get(wordnetCategory);
	  if(result == null)
	    return new ArrayList<String>();
	  return new ArrayList<String>(result);
	}
	
	public static List<String> getWordnetDomainFromWikiCategory(String wikiCategory){
		try {
			if(wordnetDomain == null)
				loadWordnetDomains();
			if(wikiCategoryToWordnetCategory == null)
				loadYagoTaxonomy();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		List<String> result = wordnetDomain.get(wikiCategoryToWordnetCategory.get(wikiCategory));
    if(result == null)
      return new ArrayList<String>();
    return new ArrayList<String>(result);
	}
	
	public static String getWordnetTypeFromWikiCategory(String wikiCategory){
		try {
			if(wikiCategoryToWordnetCategory == null)
				loadYagoTaxonomy();
		} catch (IOException e){
			e.printStackTrace();
		}
		return wikiCategoryToWordnetCategory.get(wikiCategory);
	}
	
	
	public static boolean wikiCategoryIsSubOfDomain(String wikiCategory, String superDomain) {
    // e.g. wikicategory_Olympic_badminton_players_of_South_Korea to badminton
    String specific_domain[] = superDomain.split("_");
    
    if(wikiCategory.toLowerCase().indexOf(specific_domain[1].toLowerCase()) != -1) 
      return true;
    
    List<String> domains = getWordnetDomainFromWikiCategory(wikiCategory);
    if(domains == null)
      return false;
    boolean isSubDomain = false;
    for(String domain: domains) {
      if(WordnetDomainHierarchy.getRelation(domain, superDomain) == 1) {
        isSubDomain = true;
        break;
      }
    }
    
    if (isSubDomain) 
      return true;
    return false;
  }
	
	
	public static void main(String args[]) throws Exception{
		new WikiWordnetMapper().printDomains();
//		Utils.loadProperties();
//		for (String str: Utils.getRootTypes("wikicategory_Townships_in_Macoupin_County\u002c_Illinois"))
//			System.out.println(str);
	}
	
	private void printDomains() throws Exception{
//    for (String str: WikiWordnetMapper.getWordnetDomainFromWikiCategory("wikicategory_Townships_in_Macoupin_County\u002c_Illinois"))
//      System.out.println(str);
//    Set<String> domains = new HashSet<String>();
//    for(List<String> tmp : wordnetDomain.values())
//      domains.addAll(tmp);
//    for(String str: domains)
//      System.out.println(str);
  }
}
