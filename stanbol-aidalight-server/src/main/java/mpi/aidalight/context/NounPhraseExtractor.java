package mpi.aidalight.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mpi.util.Utils;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.nlp.WordSplitter;
import LBJ2.nlp.seg.PlainToTokenParser;
import LBJ2.parse.Parser;
import edu.illinois.cs.cogcomp.lbj.chunk.Chunker;


/**
 * 
 * @author datnb
 *
 */
public class NounPhraseExtractor{
	
  /**
   * 
   * @param sentences
   * @return list of noun-phrases extracted from string "sentences"
   */
	public synchronized List<String> getNounPhrases(String sentences) {
		String tmpFile = "./data/tmp/tmpFile";
		try{
		  Utils.writeContent(tmpFile, sentences);
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
		Chunker chunker = new Chunker();
		Parser parser =
        new PlainToTokenParser(
            new WordSplitter(new SentenceSplitter(tmpFile)));
      
      String previous = "";

      List<String> nounphrases = new ArrayList<String>();
      String np = null;
      boolean isNP = false;
      for (Word w = (Word) parser.next(); w != null; w = (Word) parser.next()) {
        String prediction = chunker.discreteValue(w);
        if (prediction.startsWith("B-")
            || prediction.startsWith("I-")
               && !previous.endsWith(prediction.substring(2))){
          if(prediction.substring(2).equalsIgnoreCase("NP"))
            isNP = true;
        }
        if(isNP) {
          if(np == null)
            np = w.form;
          else
            np += " " + w.form;
        }
        if (!prediction.equals("O")
            && (w.next == null
                || chunker.discreteValue(w.next).equals("O")
                || chunker.discreteValue(w.next).startsWith("B-")
                || !chunker.discreteValue(w.next)
                    .endsWith(prediction.substring(2)))){
          if(isNP)
            nounphrases.add(np); // emit a new np
          isNP = false;
          np = null;
        }
        previous = prediction;
      }
      
      return nounphrases;
	}

}
