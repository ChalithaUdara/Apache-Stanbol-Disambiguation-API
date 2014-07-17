package mpi.typerelatedness;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mpi.lingsources.WordnetDomainHierarchy;


/**
 * Should consider idf of a node to have better estimation of coherence in the type hierarchy.
 * 
 * @author datnb
 *
 */
public class TypeTypeRelatednessHierarchy extends TypeTypeRelatedness{
  
  public TypeTypeRelatednessHierarchy() {
    super();
    
  }
  
   
  @Override
  public double getRelatedness (String type1, String type2) {
    double max = 0.0;
    for(List<String> trace1: WordnetDomainHierarchy.traceRoot(type1))
      for(List<String> trace2: WordnetDomainHierarchy.traceRoot(type2)) {
        double d = calSim(trace1, trace2);
        if(d > max)
          max = d;
      }
    return max;
  }

  
  private int getIndex(String str, List<String> src) {
    for(int i = 0; i < str.length(); i ++)
      if(src.get(i).equalsIgnoreCase(str))
        return i;
    return -1; // not found
  }
  
  private double calSim(List<String> trace1, List<String> trace2) {
    Set<String> check = new HashSet<String>(trace2);
    String common = null;
    for(String str: trace1) {
      if(check.contains(str)) {
        common = str;
        break;
      }
    }
    if(common == null || common.equalsIgnoreCase("wordnetDomain_top")) {
      return 0.0;
    }
    
    int index1 = getIndex(common, trace1);
    int index2 = getIndex(common, trace2);
    double K = (double)(index1 + index2) / 2;
//    return 1.0 / Math.log(K) * Math.log(2);
    return 1.0 / Math.pow(2, K);
  }
  
  public static void main(String args[]) {
    double d;
    
    d = new TypeTypeRelatednessHierarchy().getRelatedness("wikicategory_L.D._Alajuelense_footballers", "wikicategory_Old_maps_of_the_United_States");
    System.out.println(d);
    
    d = new TypeTypeRelatednessHierarchy().getRelatedness("wikicategory_L.D._Alajuelense_footballers", "wikicategory_L.D._Alajuelense_footballers");
    System.out.println(d);
    
    d = new TypeTypeRelatednessHierarchy().getRelatedness("wikicategory_L.D._Alajuelense_footballers", "wikicategory_Baseball_leagues_in_New_York");
    System.out.println(d);
    
    d = new TypeTypeRelatednessHierarchy().getRelatedness("wikicategory_L.D._Alajuelense_footballers", "wikicategory_Football_teams_in_Egypt");
    System.out.println(d);
    
  }
}
