package mpi.experiment;

import java.net.URLDecoder;
import java.util.Locale;


public class Standardize {
  public static String standardize(String mention) {
    String s = mention.trim().toLowerCase();
    String str = "" + Character.toUpperCase(s.charAt(0));
    for(int i = 1; i < s.length(); i++) {
      if(s.charAt(i-1) == ' ') {
        str += Character.toUpperCase(s.charAt(i));
      }
      else {
        str += s.charAt(i);
      }
    }
    return str;
  }
  
  public static String unicodeConverter(String str){
    String text = "";
    int i = 0; 
    while (i < str.length() - 1){
      if(str.charAt(i) == '\\' && str.charAt(i+1) == 'u'){
        String tmp = "";
        for(int j = i +2; j < i + 6; j++)
          tmp += str.charAt(j);
        int hexVal = Integer.parseInt(tmp, 16);
          text += (char)hexVal;
          i += 6;
      }
      else
        text += str.charAt(i++);
      
    }
    if(i < str.length())
    text += str.charAt(i);
    return text;
  }
  
  public static String conflateMention(String mention) {
    // conflate cases for mentions of length >= 4
    if (mention.length() >= 4) {
      mention = mention.toUpperCase(Locale.ENGLISH);
    }
    
    return mention;
  }
  
  public static String getPostgresEscapedString(String input) {
    return input.replace("'", "''").replace("\\", "\\\\");
  }
  
  public static void main(String args[]) throws Exception {
//    ..........Euler-Lagrange_equation
    System.out.println(unicodeConverter("Euler\u2013Lagrange_equation"));
    System.out.println(URLDecoder.decode("Lincoln%E2%80%93Douglas_debate", "UTF-8"));
  }
}
