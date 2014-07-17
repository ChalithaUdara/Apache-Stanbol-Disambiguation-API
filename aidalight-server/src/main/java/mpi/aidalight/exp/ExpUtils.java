package mpi.aidalight.exp;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.Settings;
import mpi.aidalight.context.MentionContext;
import mpi.aidalight.context.NounPhraseExtractor;
import mpi.aidalight.entitycoherence.GraphSettingsExperiment;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokenizer;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.util.Utils;
import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;


/**
 * This is used to run experiment only, where we might have mentions annotated already.
 * In CoNLL corpus, we can used the mention's types from StanfordNER or we can predict
 * them from target entities.
 * 
 * @author datnb
 *
 */

public class ExpUtils {
  
  private static int binarySearch(Sentence[] sentences, int offset){
    int begin = 0;
    int end = sentences.length-1;
    while(begin <= end){
      int mid = (begin + end)/2;
      if(sentences[mid].start == offset)
        return mid;
      else if(sentences[mid].start > offset)
        end = mid - 1;
      else 
        begin = mid + 1;
    }
    return end;
  }
  
  private static int binarySearch(int[] sentences, int offset){
    int begin = 0;
    int end = sentences.length-1;
    while(begin <= end){
      int mid = (begin + end)/2;
      if(sentences[mid] == offset)
        return mid;
      else if(sentences[mid] > offset)
        end = mid - 1;
      else 
        begin = mid + 1;
    }
    return end;
  }
  
  
  public static List<Mention> getMentions(String text, GraphSettingsExperiment settings, List<Mention> mentions) throws Exception {
    // first extract the context
//    int windowSize = 5; // extract the context in 11 sentences nearby
    int windowSize = Settings.windowSizeToExtractMentionContext; 
    
//    String file = "./data/tmp_doc";
//    Utils.writeContent(file, text);
    
    Tokens tokens = TokenizerManager.parse("tmpFile", text, Tokenizer.type.pos, false);
    TIntObjectHashMap<TIntHashSet> sentence2Nps = new TIntObjectHashMap<TIntHashSet>();
    int currentSentence = -1;
    TIntHashSet ids = null;
    int offset = 0;
    int len = 0;
    for(Token token: tokens) {
      if(token.getSentence() != currentSentence) {
        if(currentSentence != -1) {
          sentence2Nps.put(offset, ids);
        }
        currentSentence = token.getSentence();
        ids = new TIntHashSet();
        offset += len;
        len = 0;
      }
      len += token.getOriginal().length() + 1;
      
      if(token.getPOS().startsWith("V"))
        continue;
      int id = DataStore.getIdForWord(token.getOriginal());
      if(DataStore.isStopword(id) == false)
        ids.add(id);
    }
    sentence2Nps.put(offset, ids);
    
//    Map<Sentence, TIntHashSet> sentence2Nps = new HashMap<Sentence, TIntHashSet>();
//    NounPhraseExtractor extractor = new NounPhraseExtractor();
//    SentenceSplitter ss = new SentenceSplitter(file);
//    Sentence[] sentences = ss.splitAll();
//    for(Sentence sentence: sentences){
//      List<String> nps = extractor.getContext(sentence.toString());
//      Set<String> tokens = new HashSet<String>();
//      for(String np: nps){
//        String toks[] = np.split(" ");
//        for(String tok: toks)
//          if(tokens.contains(tok) == false)
//            tokens.add(tok);
//      }
//      TObjectIntHashMap<String> token2Id = DataStore.getIdsForWords(tokens);
//      TIntHashSet ids = new TIntHashSet();
//      
//      for(int id: token2Id.values())
//        if(DataStore.isStopword(id) == false)
//          ids.add(id);
//      
//      sentence2Nps.put(sentence, ids);
//    }
    
//    String[] str = file.split("/");
//    String target = str[0];
//    for(int i = 1; i < str.length-3; i++)
//      target += "/" + str[i];
//    target += "/annotated_mentions/" + str[str.length-2] + "/" + str[str.length-1];
    
    
//    List<Mention> mentions = new ArrayList<Mention>();
    int[] sentences = sentence2Nps.keys();
    Arrays.sort(sentences);
    
    for(Mention mention: mentions) {
      int vt = binarySearch(sentences, mention.getCharOffset());
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
      mention.setMentionContext(context);
      mention.setSentenceId(vt);
//      mentions.add(mention);
    }
    
//    for(String line: Utils.getContent(target)) {
//      String s[] = line.split("\t");
//      Mention mention = new Mention();
//      int offset = Integer.parseInt(s[0]);
//      mention.setCharOffset(offset);
//      mention.setMention(s[1]);
//      if(settings.isLabelingMentionTypeByNER() && s[3].equalsIgnoreCase("UNKNOWN") == false)
//        mention.setType(s[3]);
//      else {
//        if(s[2].equalsIgnoreCase(Entity.NO_MATCHING_ENTITY))
//          mention.setType(s[3]);
//        else if(DataStore.isNation(s[2].replaceAll("_", " ")))
//          mention.setType("Location");
//        else
//          mention.setType(DataStore.getType(DataStore.getIdForEntity(s[2])));
//      }
//
//      int vt = binarySearch(sentences, offset);
//      // extract sentences nearby.
//      TIntHashSet toks = sentence2Nps.get(sentences[vt]);
//      for(int i = 0; i < windowSize; i++){
//        if(vt-i-1 >= 0){
//          toks.addAll(sentence2Nps.get(sentences[vt-i-1]));
//        }
//        if(vt+i+1 < sentences.length){
//          toks.addAll(sentence2Nps.get(sentences[vt+i+1]));
//        }
//      }
//      MentionContext context = new MentionContext(toks.toArray(), null);
//      mention.setMentionContext(context);
//      mention.setSentenceId(vt);
//      mentions.add(mention);
//    }
    
    return mentions;
  }
  
  public static List<Mention> getMentionsFromWikiPages(String file) throws Exception {
    // first extract the context
//    int windowSize = 5; // extract the context in 11 sentences nearby
    int windowSize = Settings.windowSizeToExtractMentionContext; 
    
    Map<Sentence, TIntHashSet> sentence2Nps = new HashMap<Sentence, TIntHashSet>();
    NounPhraseExtractor extractor = new NounPhraseExtractor();
    SentenceSplitter ss = new SentenceSplitter(file);
    Sentence[] sentences = ss.splitAll();
    for(Sentence sentence: sentences){
      List<String> nps = extractor.getNounPhrases(sentence.toString());
      Set<String> tokens = new HashSet<String>();
      for(String np: nps){
        String toks[] = np.split(" ");
        for(String tok: toks)
          if(tokens.contains(tok) == false)
            tokens.add(tok);
      }
      TObjectIntHashMap<String> token2Id = DataStore.getIdsForWords(tokens);
      TIntHashSet ids = new TIntHashSet();
      
      for(int id: token2Id.values())
        if(DataStore.isStopword(id) == false)
          ids.add(id);
      
      sentence2Nps.put(sentence, ids);
    }
    
    String[] str = file.split("/");
    String target = str[0];
    for(int i = 1; i < str.length-2; i++)
      target += "/" + str[i];
    target += "/annotated_mentions/" + str[str.length-1];
    List<Mention> mentions = new ArrayList<Mention>();
    for(String line: Utils.getContent(target)) {
      String s[] = line.split("\t");
      
      Mention mention = new Mention();
      if(DataStore.isNation(s[2].replaceAll("_", " ")))
        mention.setType("Location");
      else
        mention.setType(DataStore.getType(DataStore.getIdForEntity(s[2])));
//      Set<String> types = DataStore.getTypes(DataStore.getIdForEntity(entity));
//      if(types == null)
//        mention.setType("MISC"); // just choose one
//      else
//        for (String type: types) {
//          mention.setType(type);
//          break;
//        }
      
      int offset = Integer.parseInt(s[0]);
      mention.setCharOffset(offset);
      mention.setMention(s[1]);

      int vt = binarySearch(sentences, offset);
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
      mention.setMentionContext(context);
      mention.setSentenceId(vt);
      mentions.add(mention);
    }
    
    return mentions;
  }
}
