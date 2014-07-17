package mpi.aidalight.context;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mpi.aida.data.Mention;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;
import LBJ2.nlp.Sentence;

public class MentionExtractor {

  private HashMap<String, String> tags = null;
  private Sentence[] sentences;
  private Map<Sentence, TIntHashSet> sentence2Nps;
  
  private int windowSize = 1;

  public MentionExtractor(Sentence[] sentences, Map<Sentence, TIntHashSet> sentence2Nps, int windowSize) {
    this.sentences = sentences;
    this.sentence2Nps = sentence2Nps;
	  this.windowSize = windowSize;
    
    tags = new HashMap<String, String>();
    tags.put("LOCATION", "LOCATION");
    tags.put("I-LOC", "I-LOC");
    tags.put("B-LOC", "I-LOC");
    tags.put("PERSON", "PERSON");
    tags.put("I-PER", "I-PER");
    tags.put("B-PER", "I-PER");
    tags.put("ORGANIZATION", "ORGANIZATION");
    tags.put("I-ORG", "I-ORG");
    tags.put("B-ORG", "I-ORG");
    tags.put("MISC", "MISC");
    tags.put("I-MISC", "I-MISC");
    tags.put("B-MISC", "I-MISC");
  }

  public List<Mention> filter(Tokens tokens) {
    List<Mention> mentions = new ArrayList<Mention>();
    HashMap<Integer, Integer> subStrings = new HashMap<Integer, Integer>();
    List<String> content = new LinkedList<String>();
    for (int p = 0; p < tokens.size(); p++) {
      Token token = tokens.getToken(p);
      content.add(token.getOriginal());
    }
    String previous = null;
    int start = -1;
    int end = -1;
    for (int p = 0; p < tokens.size(); p++) {
      Token token = tokens.getToken(p);
      if (previous == null) {
        if (tags.containsKey(token.getNE())) {
          previous = tags.get(token.getNE());
          start = token.getId();
          end = token.getId();
        }
      } else if (previous.equals(token.getNE())) {
        end = token.getId();
      } else {
        Mention newMentions = getPossibleMentions(start, end, tokens, previous);
        
        int vt = binarySearch(sentences, token);
        // extract sentences nearby.
        TIntHashSet toks = sentence2Nps.get(sentences[vt]);
        for(int i = 0; i < windowSize; i++){
          if(vt-i-1 >= 0){
            toks.addAll(sentence2Nps.get(sentences[vt-i-1]));
          }
          if(vt+i+1 < sentences.length){
            toks.addAll(sentence2Nps.get(sentences[vt+i+1]));
          }
        }
        MentionContext context = new MentionContext(toks.toArray(), null);
        newMentions.setMentionContext(context);
        
        mentions.add(newMentions);
        subStrings.put(start, end);
        previous = null;
        if (tags.containsKey(token.getNE())) {
          previous = tags.get(token.getNE());
          start = token.getId();
          end = token.getId();
        }
      }
    }
    if (previous != null) {
      Mention newMentions = getPossibleMentions(start, end, tokens, previous);
      
      int vt = binarySearch(sentences, tokens.getToken(tokens.size()-1));
      // extract sentences nearby.
      TIntHashSet toks = sentence2Nps.get(sentences[vt]);
      for(int i = 0; i < windowSize; i++){
        if(vt-i-1 >= 0){
          toks.addAll(sentence2Nps.get(sentences[vt-i-1]));
        }
        if(vt+i+1 < sentences.length){
          toks.addAll(sentence2Nps.get(sentences[vt+i+1]));
        }
      }
      MentionContext context = new MentionContext(toks.toArray(), null);
      newMentions.setMentionContext(context);
      
      mentions.add(newMentions);
      subStrings.put(start, end);
      previous = null;
    }
    return mentions;
  }

  private Mention getPossibleMentions(int start, int end, Tokens advTokens, String type) {
    String meansArg = advTokens.toText(start, end);
    int startStanford = advTokens.getToken(start).getStandfordId();
    int sentenceId = advTokens.getToken(start).getSentence();
    int endStanford = advTokens.getToken(end).getStandfordId();
    Mention mention = new Mention(meansArg, start, end, startStanford, endStanford, sentenceId, type);
    int firstChar = advTokens.getToken(mention.getStartToken()).getBeginIndex();
    int lastChar = advTokens.getToken(mention.getEndToken()).getEndIndex();
    int charLength = lastChar - firstChar;
    mention.setCharOffset(firstChar);
    mention.setCharLength(charLength);
    return mention;
  }
  
  private int binarySearch(Sentence[] sentences, Token token){
		int begin = 0;
		int end = sentences.length-1;
		while(begin <= end){
			int mid = (begin + end)/2;
			if(sentences[mid].start == token.getBeginIndex())
				return mid;
			else if(sentences[mid].start > token.getBeginIndex())
				end = mid - 1;
			else 
				begin = mid + 1;
		}
		return end;
	}
}
