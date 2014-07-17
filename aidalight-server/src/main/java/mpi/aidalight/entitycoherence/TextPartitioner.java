package mpi.aidalight.entitycoherence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Mention;
import mpi.aidalight.context.NounPhraseExtractor;



public class TextPartitioner {
  private String[] lines = null;
  
  private List<Mention> mentions = null;
  
  private double threshold = 0.01;
  
  public TextPartitioner(String[] lines) {
    this.lines = lines;
  }
  
  public TextPartitioner(String[] lines, List<Mention> mentions, double threshold) {
    this.lines = lines;
    this.mentions = mentions;
    this.threshold = threshold;
  }
  
  public List<Integer> partition() {
    if(mentions == null)
      return partition(lines);
    return partition(lines, mentions);
  }
  
  private List<Integer> partition(String[] lines) {
    List<Integer> pos = new ArrayList<Integer>();
    int vt = 0;
    boolean newParagraph = false;
    for(String line: lines) {
      String tmp = line.trim();
      if(tmp.equalsIgnoreCase("")) {
        newParagraph = true;
      }
      else {
        if(newParagraph) {
          pos.add(vt);
          newParagraph = false;
        }
      }
      vt += line.length() + 1; // 1 for \n
    }
    pos.add(vt);
    
    return pos;
  }
  
  public List<Integer> partitionByLine(List<String> lines) {
    List<Integer> pos = new ArrayList<Integer>();
    int vt = 0;
    for(String line: lines) {
      pos.add(vt);
      vt += line.length() + 1; // 1 for \n
    }
    pos.add(vt);
    
    return pos;
  }
  
  
  private List<Integer> partition(String[] lines, List<Mention> mentions) {
    List<Integer> pos = new ArrayList<Integer>();
    List<Integer> tmp = partition(lines);//partitionByLine(lines);
    List<List<Mention>> mentionListByParagraph = new ArrayList<List<Mention>>();
    
    String text = "";
    for(String line: lines)
      text += line + "\n";
    List<String> context = new ArrayList<String>();
    
    int start = 0;
    int vt = 0;
    for(int i = 0; i < tmp.size(); i ++) {
      int end = tmp.get(i);
      List<Mention> ms = new ArrayList<Mention>();
      while(vt < mentions.size() && mentions.get(vt).getCharOffset() >= start && mentions.get(vt).getCharOffset() < end)
        ms.add(mentions.get(vt++));
      mentionListByParagraph.add(ms);
      context.add(text.substring(start, end));
      start = end;
    }
    
    int lastUnemptyParagraph = -1;
    
    for(int i = 0; i < mentionListByParagraph.size(); i ++) {
      if(mentionListByParagraph.get(i).size() == 0) {
        // combine. Do nothing
        if(i == mentionListByParagraph.size() - 1)
          pos.add(tmp.get(i)); // end text
      }
      else {
        if(lastUnemptyParagraph == -1) {
          // first unempty paragraph
          // do nothing.
        }
        else {
          double mention_relatedness = getRelatedness(mentionListByParagraph.get(lastUnemptyParagraph), mentionListByParagraph.get(i));
          NounPhraseExtractor npe = new NounPhraseExtractor();
          double context_relatedness = getContextRelatedness(npe.getNounPhrases(context.get(lastUnemptyParagraph)), npe.getNounPhrases(context.get(i)));
          double context_relatedness_at_token_level = getContextRelatednessAtTokenLevel(npe.getNounPhrases(context.get(lastUnemptyParagraph)), npe.getNounPhrases(context.get(i)));
          
          System.out.println(mention_relatedness + "\t" + context_relatedness + "\t" + context_relatedness_at_token_level);
          
          
          if(mention_relatedness > threshold ) {
            // combine. Do nothing
          }
          else {
            pos.add(tmp.get(i));
          }
        }
        lastUnemptyParagraph = i;
      }
    }
    return pos;
  }
  
  
  private double getContextRelatedness(List<String> src, List<String> dst) {
    if(src.size() == 0 || dst.size() == 0)
      return 0.0;
    Set<String> tmp = new HashSet<String>(src);
    int counter = 0;
    for(String np: dst)
      if(tmp.contains(np))
        counter ++;
    return (double) counter / (src.size() + dst.size() - counter);
  }
  
  private double getContextRelatednessAtTokenLevel(List<String> src, List<String> dst) {
    if(src.size() == 0 || dst.size() == 0)
      return 0.0;
    
    Set<String> tokens = new HashSet<String>();
    for(String np: src){
      String toks[] = np.split(" ");
      for(String tok: toks)
        if(tokens.contains(tok) == false)
          tokens.add(tok);
    }
    
    Set<String> dst_tokens = new HashSet<String>();
    for(String np: dst){
      String toks[] = np.split(" ");
      for(String tok: toks)
        if(dst_tokens.contains(tok) == false)
          dst_tokens.add(tok);
    }
    
    int counter = 0;
    for(String np: tokens) {
      if(dst_tokens.contains(np))
        counter ++;
    }
    return (double) counter / (tokens.size() + dst_tokens.size() - counter);
  }
  
  private double getRelatedness(List<Mention> src, List<Mention> dst) {
    if(src.size() == 0 || dst.size() == 0)
      return 0;
    List<Mention> allMentions = new ArrayList<Mention>();
    allMentions.addAll(src);
    allMentions.addAll(dst);
    MentionCombiner mc = new MentionCombiner(allMentions);
    
    Map<Mention, Mention> mp = mc.deduplicate();
    int counter = 0;
    for(Mention mention: src) {
      for(Mention m: dst) {
        if(mp.get(mention).equals(mp.get(m))) {
          counter ++;
          break; // count just 1 for each mention
        }
      }
    }
    return (double) counter / (src.size() + dst.size() - counter);
  }
  
}
