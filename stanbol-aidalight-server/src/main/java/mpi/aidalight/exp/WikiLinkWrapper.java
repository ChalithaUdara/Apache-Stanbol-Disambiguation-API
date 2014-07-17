package mpi.aidalight.exp;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mpi.aidalight.DataStore;
import mpi.experiment.Standardize;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokenizer;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.util.Utils;
import de.l3s.boilerpipe.extractors.ArticleExtractor;


public class WikiLinkWrapper {
  private String wikiLinkDumpPath;
  private String urlFilePath; // path to the file containing urls
  private String outputPath; // path to the output (in tsv format) file
  
  public WikiLinkWrapper(String wikiLinkDumpPath, String urlFilePath, String outputPath) {
    this.wikiLinkDumpPath = wikiLinkDumpPath;
    this.urlFilePath = urlFilePath;
    this.outputPath = outputPath;
  }
  
  private String getText(String url) throws IOException{
    String text = "";
    BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
    String strTemp = "";
    while(null != (strTemp = br.readLine())){
      text += strTemp + "\n";
    }
    
    return text;
  }
  
  
  private boolean checkCharacter(char ch) {
    if(ch >= '0' && ch <= '9')
      return true;
    if(ch >= 'a' && ch <= 'z')
      return true;
    if(ch >= 'A' && ch <= 'Z')
      return true;
    if(ch == ' ')
      return true;
    return false;
  }
  
  
  private int getIndex(String text, String mention, int start) {
    String str[] = mention.split("\t");
    String query = "<a href=\"" + str[3];
    int index = text.indexOf(query, start);
    if(index == -1)
      return -1;
    // found, then compare mention string
    int i = index + query.length();
    while(i < text.length() && text.charAt(i) != '>')
      i++;
    if(i == text.length())
      return -1;
    i++;
    for(int j = 0; j < str[1].length(); j ++) {
      if(i + j >= text.length())
        return -1;
      if(str[1].charAt(j) != text.charAt(i+j))
        return getIndex(text, mention, i);
    }
    return index;
  }
  
  private String process(String htmlText, List<String> mentions) throws Exception {
    String text = ArticleExtractor.INSTANCE.getText(htmlText);
    
    TIntObjectHashMap<String> mentionMap = new TIntObjectHashMap<String>();
    for(String mention: mentions) {
      int index = getIndex(htmlText, mention, 0);
      if(index == -1) {
        return null;
      }
      
      String suffix = "";
      int postIndex = htmlText.indexOf("</a>", index) + 4;
      for(int i = 0; i < 5 && postIndex + i < htmlText.length(); i ++) {
        if(checkCharacter(htmlText.charAt(postIndex + i)))
          suffix += htmlText.charAt(postIndex + i);
        else
          break;
      }
      
      index--;
      String prefix = "";
      for(int i = 0; i < 5 && i < index; i ++) {
        if(checkCharacter(htmlText.charAt(index - i)))
          prefix = htmlText.charAt(index - i) + prefix;
        else
          break;
      }
      
      String str[] = mention.split("\t");
      String query = prefix + str[1] + suffix;
      int offset = text.indexOf(query);
      if(offset == -1)
        return null;
      
      String entity = URLDecoder.decode(str[3].substring("http://en.wikipedia.org/wiki/".length(), str[3].length()), "UTF-8");
      // check if entity is in YAGO
      if(DataStore.getIdForEntity(Standardize.unicodeConverter(entity)) != 0)
        mentionMap.put(offset + prefix.length(), str[1] + "\t" + entity + "\tUNKNOWN");
    }
    
    String res = "";
    Tokens tokens = TokenizerManager.parse("tmpFile", text, Tokenizer.type.tokens, false);
    int check = -1;
    String str = "";
    for(Token token: tokens) {
      int offset = token.getBeginIndex();
      if(mentionMap.contains(offset)) {
        check = offset;
        str = mentionMap.get(offset);
        res += token.getOriginal() + "\tB\t" + str + "\n"; 
      } 
      else {
        if(check != -1) {
          String s[] = str.split("\t");
          if(offset - check > s[0].length()) {
            // out of mention
            res += token.getOriginal() + "\n";
            check = -1;
          }
          else {
            res += token.getOriginal() + "\tI\t" + str + "\n";
          }
        }
        else {
          res += token.getOriginal() + "\n";
        }
      }
    }
    
    return res;
  }
  
  public void gatherData() throws Exception {
    // load urls
    Set<String> urls = new HashSet<String>();
    for(String line: Utils.getContent(urlFilePath)) {
      String str[] = line.split(" ");
      if(str.length > 1)
        urls.add(str[1]);
    }
    String res = "";
    // go through wiki-links file, gather interesting information...
    int urlFound = 0, counter = 0;
    FileInputStream fis = new FileInputStream(wikiLinkDumpPath);
    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
    BufferedReader bufReader = new BufferedReader(isr);

    String line;
    while (true) {
      line = bufReader.readLine();
      if (line == "" || line == null)
        break;
      if(++counter % 10000 == 0)
        System.out.println(counter);
      if(line.startsWith("URL")) {
        // extract url
        String str[] = line.split("\t");
        String url = str[1];
        if(urls.contains(url)) {
          // this is interesting information
          // get html text
          int attempt = 5;
          String htmlText = null;
          while(htmlText == null && attempt > 0) {
            try {
              htmlText = getText(url);
            } catch (IOException e) {
              System.out.println("try to connect to " + url + "...");
              attempt--;
            }
          }
          if(htmlText == null) {
            System.out.println("fail to connect to " + url + "!");
            continue;
          }
          // get mentions
          List<String> mentions = new ArrayList<String>();
          while(true) {
            line = bufReader.readLine();
            if(line == "" || line == null || line.startsWith("MENTION") == false)
              break;
            // this is a mention
            mentions.add(line);
          }
          String tmp = process(htmlText, mentions);
          if(tmp != null)
            res += "-DOCSTART- (" + urlFound + " " + url + ")" + "\n" + tmp + "\n\n";
          urlFound++;
          if(urlFound == urls.size())
            break;
        }
      }
    }
    System.out.println("Already extracted " + urlFound + " out of " + urls.size() + " urls!");
    
    isr.close();
    fis.close();
    
    Utils.writeContent(outputPath, res);
  }
  
  public static void main(String args[]) throws Exception {
    new WikiLinkWrapper(args[0], args[1], args[2]).gatherData();
  }
}
