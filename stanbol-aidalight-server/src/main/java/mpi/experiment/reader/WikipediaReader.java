package mpi.experiment.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javatools.filehandlers.UTF8Reader;
import javatools.filehandlers.UTF8Writer;
import mpi.aida.AidaManager;
import mpi.aida.data.Context;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aidalight.DataStore;
import mpi.experiment.reader.util.WikipediaLink;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basics.Normalize;

/**
 * This reader expects a folder of *.txt files containing MediaWiki Markup.
 * It will clean the markup and extract anchor links as mentions/ground truth
 * 
 */
public class WikipediaReader extends CollectionReader {
  private static final Logger logger = 
      LoggerFactory.getLogger(WikipediaReader.class);

  private List<String> docIds;

  private HashMap<String, String> text;

  private HashMap<String, Tokens> tokens;

  private HashMap<String, Mentions> mentions;  
  
  private Set<String> entities = null;
  
//  private static boolean intersectionMentionSet = true;
  
  
  public WikipediaReader(String collectionPath) {
    this(collectionPath, 0, Integer.MAX_VALUE, new CollectionReaderSettings());
  }

  public WikipediaReader(String collectionPath, CollectionPart cp) {
    this(collectionPath, cp, new CollectionReaderSettings());
  }
  
  public WikipediaReader(String collectionPath, CollectionPart cp, CollectionReaderSettings settings) {
    super(collectionPath, cp, settings);
    init();
  }

  public WikipediaReader(String collectionPath, int from, int to, CollectionReaderSettings settings) {
    super(collectionPath, from, to, settings);
    init();
  }

  private HashMap<String, String> init() {
    text = new HashMap<String, String>();
    docIds = new LinkedList<String>();
    mentions = new HashMap<String, Mentions>();
    tokens = new HashMap<String, Tokens>();
    String path = collectionPath + "/text";
    HashMap<String, String> plainDocs = new HashMap<String, String>();
    try {
      File dir = new File(path);
      if (!dir.exists() || !dir.isDirectory()) {
        logger.error("Directory does not exist " + path);
        return plainDocs;
      }

      int counter = 0;

      for (File file : dir.listFiles()) {
        if (file.isDirectory() || file.getName().startsWith(".")) {
          continue;
        }
        counter++;
        if (counter >= from && counter <= to) {
          String id = file.getName().replace(' ', '_');

          docIds.add(id);

          //          logger.info("Reading '" + file.getAbsolutePath() + "'");

          UTF8Reader reader = null;

          // workaround for crappy NFS @ d5blades ....
          int retries = 5;

          while (retries > 0) {
            try {
              reader = new UTF8Reader(file);
              retries = 0;
            } catch (FileNotFoundException e) {
              logger.warn("Retrying '" + file.getAbsolutePath() + "' for " + retries + " times");
              Thread.sleep(500);
              retries--;
            }
          }

          String line = null;
          StringBuffer doc = new StringBuffer();
          while ((line = reader.readLine()) != null) {
            doc.append(line);
            doc.append("\n");
          }
          reader.close();

          getLinks(id, cleanText(doc.toString()));
        }
      }
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage());
    }
    return plainDocs;
  }

  private String cleanText(String text) {
    int size = 0;
    // remove external links
    while (size != text.length()) {
      size = text.length();
      text = text.replaceAll("(\\[https?:.+)\\[\\[[^\\[\\]]+\\]\\]", "$1");
      text = text.replaceAll("\\[https?:[^\\[\\]]+\\]", " ");
    }
    // remove xml tags
    text = text.replaceAll("<[^/t!][^>]+>", " ");
    text = text.replaceAll("</[^t][^>]+>", " ");
    size = 0;
    // remove xml comments
    while (size != text.length()) {
      size = text.length();
      text = text.replaceAll("<!--.+-->", "");
      text = text.replaceAll("(<!--.*)\\n", "$1");
    }
    // remove all templates/macros
    text = text.replaceAll("'{2,}", "");
    text = text.replaceAll("\\[\\[[^\\[\\]]+:[^\\[\\]]+\\]\\]", "");
    text = text.replaceAll("\\n\\n+", "\n");
    size = 0;
    // remove {{ ... }}
    text = text.replaceAll("\\{\\{[sS]tart [bB]ox\\}\\}", "{|");
    text = text.replaceAll("\\{\\{[eE]nd [bB]ox\\}\\}", "|}");
    while (size != text.length()) {
      size = text.length();
      text = text.replaceAll("\\{\\{[[^\\{\\}]\\n]+\\}\\}", "");
      text = text.replaceAll("(\\{\\{[^\\{\\}]*)\\n", "$1");
      text = text.replaceAll("\\{\\}", "");
    }
    text = text.replaceAll("\\n\\n+", "\n");
    //    size = 0;
    //    // remove {| ... |}
    //    while (size != text.length()) {
    //      size = text.length();
    //      text = text.replaceAll("\\{\\|[^\\{\\}]+\\|\\}", "");
    //      text = text.replaceAll("(\\{\\|[^\\{\\}]*)\\n", "$1");
    //    }
    //    text = text.replaceAll("\\n\\n+", "\n");
    size = 0;
    // remove [[ ... :  ... ]]
    while (size != text.length()) {
      size = text.length();
      text = text.replaceAll("(\\[\\[[^\\[\\]]+:[^\\[\\]]+)\\[\\[[^\\[\\]]+\\]\\]", "$1");
      text = text.replaceAll("\\[\\[[^\\[\\]]+:[^\\[\\]]+\\]\\]", "");
      text = text.replaceAll("(\\[\\[[^\\[\\]]+:[^\\[\\]]+)\\n", "$1");
      text = text.replaceAll("\\[\\]", "");
    }
    text = text.replaceAll("\\( *\\)", " ");
    // remove * \n and *\n
    text = text.replaceAll("\\* *\\n", "");
    // remove extra newlines
    text = text.replaceAll("\\n\\n+", "\n");
    // remove == to prepare into titles
    text = text.replaceAll("==+", "\n");
    // clean multiple new lines
    text = text.replaceAll(" +\\n", "\n");
    text = text.replaceAll("\\n\\n\\n+", "\n\n");
    text = text.replaceAll("  +", " ");
    size = 0;
    return text.trim();
  }

  private void getLinks(String id, String doc) {
    String annotation = doc;
    List<WikipediaLink> links = new LinkedList<WikipediaLink>();
    int position = 0;
    int textPosition = 0;
    while (position < annotation.length()) {
      int start = annotation.indexOf("[[", position);
      int end = -1;
      if (start == position) {
        end = annotation.indexOf("]]", start) + 2;
        if (end > start) {
          WikipediaLink link = new WikipediaLink(annotation.substring(start, end), textPosition);
          links.add(link);
          position = end;
          textPosition = textPosition + link.getLength();
        } else {
          // in case there is no ]] to finish the mention this last mention is ignored.
          position=position+2;
        }
      } else if (start == -1) {
        WikipediaLink link = new WikipediaLink(annotation.substring(position), textPosition);
        links.add(link);
        position = annotation.length();
        textPosition = textPosition + link.getLength();
      } else if (start > position) {
        WikipediaLink link = new WikipediaLink(annotation.substring(position, start), textPosition);
        links.add(link);
        position = start;
        textPosition = textPosition + link.getLength();
      }
    }
    StringBuffer sb = new StringBuffer();
    Iterator<WikipediaLink> build = links.iterator();
    Mentions mentions = new Mentions();
    while (build.hasNext()) {
      WikipediaLink link = build.next();
      sb.append(link.getText());
      if (link.getTarget() != null) {
//        String groundTruthEntity = link.getTargetOriginal();
//        groundTruthEntity = Standardize.unicodeConverter(groundTruthEntity).replaceAll(" ", "_");
//        if(intersectionMentionSet) {
//          if (IntersectionMentionSetAmongKBsExtraction.getIdForEntity(groundTruthEntity) != 0) {
//            Mention mention = new Mention();
//            mention.setCharOffset(link.getOffset());
//            mention.setCharLength(link.getLength());
//            mention.setMention(link.getText());
//            mention.setGroundTruthResult(link.getTargetOriginal());
//            mentions.addMention(mention);
//          }
//          else {
////            System.out.println(id + "\t" + groundTruthEntity + " not in YAGO");
//          }
//        }
//        else {
//          Mention mention = new Mention();
//          mention.setCharOffset(link.getOffset());
//          mention.setCharLength(link.getLength());
//          mention.setMention(link.getText());
//          mention.setGroundTruthResult(link.getTargetOriginal());
//          mentions.addMention(mention);
//        }
//        
////        else {
////          System.out.println(id + "\t" + groundTruthEntity + " not in YAGO");
////        }
        
        Mention mention = new Mention();
        mention.setCharOffset(link.getOffset());
        mention.setCharLength(link.getLength());
        mention.setMention(link.getText());
        mention.setGroundTruthResult(link.getTargetOriginal());
        mentions.addMention(mention);
      }
    }
    text.put(id, sb.toString());
    tokenize(id, sb.toString(), mentions);
  }

  private void tokenize(String id, String text, Mentions mentions) {
    Tokens tokens = AidaManager.tokenize(id, text);
    this.tokens.put(id, tokens);
    List<String> textContent = new LinkedList<String>();
    for (int p = 0; p < tokens.size(); p++) {
      Token token = tokens.getToken(p);
      textContent.add(token.getOriginal());
    }
    int startToken = -1;
    int endToken = -1;
    int t = 0;
    int i = 0;
    Mention mention = null;
    Token token = null;
    while (t < tokens.size() && i < mentions.getMentions().size()) {
      mention = mentions.getMentions().get(i);
      token = tokens.getToken(t);
      if (startToken >= 0) {
        if (token.getEndIndex() > mention.getCharOffset() + mention.getCharLength()) {
          mention.setStartToken(startToken);
          mention.setId(startToken);
          mention.setEndToken(endToken);
          if (mention.getMention() == null) {
            mention.setMention(tokens.toText(startToken, endToken));
          }
          startToken = -1;
          endToken = -1;
          i++;
        } else {
          endToken = token.getId();
          t++;
        }
      } else {
        if (token.getBeginIndex() >= mention.getCharOffset() && mention.getCharOffset() <= token.getEndIndex()) {
          startToken = token.getId();
          endToken = token.getId();
        } else {
          t++;
        }
      }
    }
    if (startToken >= 0) {
      if (token.getEndIndex() >= mention.getCharOffset() + mention.getCharLength()) {
        mention.setStartToken(startToken);
        mention.setId(startToken);
        mention.setEndToken(endToken);
      }
    }
    List<Mention> tobeRemoved = new LinkedList<Mention>();
    for (int m = 0; m < mentions.getMentions().size(); m++) {
      Mention check = mentions.getMentions().get(m);
      Token start = tokens.getToken(check.getStartToken());
      Token end = tokens.getToken(check.getEndToken());
      if (start.getBeginIndex() != check.getCharOffset() || end.getEndIndex() != check.getCharOffset() + check.getCharLength()) {
        tobeRemoved.add(check);
      } else {
        if (!isEntity(check)) {
          // TODO re-add redirect check
          // if (!isRedirect(check)) {
            tobeRemoved.add(check);
          // }
        }
      }
    }
    Iterator<Mention> iter = tobeRemoved.iterator();
    while (iter.hasNext()) {
      mentions.remove(iter.next());
    }
    this.mentions.put(id, mentions);
  }

  private boolean isEntity(Mention mention) {
    String target = Normalize.entity(mention.getGroundTruthResult());
    if (settings.isIncludeNMEMentions() &&
        mention.getGroundTruthResult().equals(Entity.OOKBE)) {
      return true;
    } else if (isEntity(target)) {
      mention.setGroundTruthResult(target);
      return true;
    } else {
      return false;
    }
  }
  
  
  private boolean isEntity(String entity) {
    if(entities == null) {
      entities = new HashSet<String>();
      for(String e: DataStore.getAllEntitiesIds().keySet()) {
        entities.add(Normalize.entity(e));
      }
    }
    return entities.contains(entity);
  }
  
  

  @Override
  public Iterator<PreparedInput> iterator() {
    if (preparedInputs == null) {
      preparedInputs = new ArrayList<PreparedInput>(docIds.size());

      // Hack for Wikipedia Nullmentions dataset.
      long documentTimestamp = 
        new DateTime(2013, 7, 9,
            0, 0, DateTimeZone.UTC).getMillis();

      for (String docId : docIds) {
        PreparedInput p = new PreparedInput(docId, tokens.get(docId), mentions.get(docId));
        p.setTimestamp(documentTimestamp);
        preparedInputs.add(p);
      }
    }

    return preparedInputs.iterator();
  }

  @Override
  public Mentions getDocumentMentions(String docId) {
    return mentions.get(docId);
  }

  @Override
  public int collectionSize() {
    return docIds.size();
  }

  @Override
  public String getText(String docId) {
    return text.get(docId);
  }

  @Override
  protected int[] getCollectionPartFromTo(CollectionPart cp) {
    int[] ft = new int[] { 1, 150 };
    switch (cp) {
      case TRAIN:
        ft = new int[] { 1, 100 };
        break;
      case DEV:
        ft = new int[] { 101, 120 };
        break;
      case DEV_SMALL:
        ft = new int[] { 101, 110 };
        break;
      case TEST:
        ft = new int[] { 121, 150 };
        break;
      default:
        ft = new int[] { 1, docIds.size() };
        break;
    }
    return ft;
  }

  public static void main(String[] args) {
//    String path = "./data/experiment/WIKIPEDIA_YAGO2";
    String path = args[0];
    WikipediaReader wr = new WikipediaReader(path);
    File dir = new File(path + "/clean");
    if (!dir.exists()) {
      dir.mkdir();
    }
    dir = new File(path + "/info");
    if (!dir.exists()) {
      dir.mkdir();
    }
    try {
      Iterator<String> ids = wr.docIds.iterator();
      while (ids.hasNext()) {
        String id = ids.next();
        File f = new File(path + "/clean/c-" + id);
        UTF8Writer writer = new UTF8Writer(f);
        writer.write(wr.getText(id));
        writer.close();
        f = new File(path + "/info/i-" + id);
        writer = new UTF8Writer(f);
        writer.write(wr.getDocumentMentions(id).toString());
        writer.write("\n--------------------\n");
        writer.write(wr.tokens.get(id).toString());
        writer.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("done");
  }

  @Override
  public Context getDocumentContext(String docId) {
    return new Context(tokens.get(docId));
  }
}
