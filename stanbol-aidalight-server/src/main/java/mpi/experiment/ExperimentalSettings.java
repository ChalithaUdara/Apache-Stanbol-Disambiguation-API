package mpi.experiment;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Mention;
import mpi.experiment.reader.AIDAReader;
import mpi.experiment.reader.CoNLLReader;
import mpi.experiment.reader.CollectionReader;
import mpi.experiment.reader.CollectionReader.CollectionPart;
import mpi.experiment.reader.CollectionReader.DataSource;
import mpi.experiment.reader.CollectionReaderSettings;
import mpi.experiment.reader.Gigaword5Reader;
import mpi.experiment.reader.NewsStreamReader;
import mpi.experiment.reader.WikipediaReader;

/**
 * Contains all experimental settings, including default values for each setting
 * where it makes sense
 * 
 *
 */
public class ExperimentalSettings {

  private DataSource ds = DataSource.NONE;
  
  private CollectionReader cr = null;
  
  private boolean shouldOnlyDoEvaluation = false;

  private boolean shouldTrace = false;
  
  private boolean shouldGraphTrace;

  private boolean restrictMentions = false;
   
  private Map<String, Set<Mention>> restrictedMentions = new HashMap<String, Set<Mention>>();
  
  private boolean restrictMentionNames = false;
  
  private Set<String> restrictedMentionNames = new HashSet<String>();

  private boolean forceGraphCreation;
    
  private int numParallelThreads = 1;

  private boolean shouldGenerateLinkCounts = false;
  
  private String previousResults;
  
  public ExperimentalSettings(String collectionPath, DataSource ds, CollectionPart cp, int from, int to, CollectionReaderSettings crSettings) throws IOException {       
    cr = getCollectionReaderForSource(collectionPath, ds, cp, from, to, crSettings);
  }
  
  public static CollectionReader getCollectionReaderForSource(
      String collectionPath, DataSource ds, CollectionPart cp, 
      int from, int to, CollectionReaderSettings crSettings) throws IOException {
    CollectionReader cr = null;
    switch (ds) {
      case CONLL:
        if (cp != null) {
          cr = new CoNLLReader(collectionPath, cp, crSettings);
        } else {
          cr = new CoNLLReader(collectionPath, from, to, crSettings);
        }
        break;
      case WIKIPEDIA_YAGO2:
        if (cp != null) {
          cr = new WikipediaReader(collectionPath, cp, crSettings);
        } else {
          cr = new WikipediaReader(collectionPath, from, to, crSettings);
        }
        break;
      case AIDA:
        if (cp != null) {
          cr = new AIDAReader(collectionPath, cp, crSettings);
        } else {
          cr = new AIDAReader(collectionPath, from, to, crSettings);
        }
        break;        
      case NEWSSTREAMS:
        if (cp != null) {
          cr = new NewsStreamReader(collectionPath, cp, crSettings);
        } else {
          cr = new NewsStreamReader(collectionPath, from, to, crSettings);
        }
        break;
      case GIGAWORD5:
        if (cp != null) {
          cr = new Gigaword5Reader(collectionPath, cp, crSettings);
        } else {
          cr = new Gigaword5Reader(collectionPath, from, to, crSettings);
        }
        break;  
      default:
        break;
    }
    return cr;
  }
    
  public void setShouldOnlyDoEvaluation(boolean shouldOnlyDoEvaluation) {
    this.shouldOnlyDoEvaluation = shouldOnlyDoEvaluation;
  }

  public CollectionReader getCollectionReader() {
    return cr;
  }

  public boolean shouldOnlyDoEvaluation() {
    return shouldOnlyDoEvaluation;
  }

  public void setShouldTrace(boolean flag) {
    shouldTrace = flag;
  }
  
  public boolean shouldTrace() {
    return shouldTrace;
  }

  public boolean isRestrictMentions() {
    return restrictMentions;
  }
  
  public void setRestrictMentions(boolean restrictMentions) {
    this.restrictMentions = restrictMentions;
  }
  
  public Map<String, Set<Mention>> getRestrictedMentions() {
    return restrictedMentions;
  }
  
  public void setRestrictedMentions(Map<String, Set<Mention>> restrictedMentions) {
    this.restrictedMentions = restrictedMentions;
  }
  
  public boolean isRestrictMentionNames() {
    return restrictMentionNames;
  }
  
  public void setRestrictMentionNames(boolean restrictMentionNames) {
    this.restrictMentionNames = restrictMentionNames;
  }
  
  public Set<String> getRestrictedMentionNames() {
    return restrictedMentionNames;
  }

  public void setRestrictedMentionNames(Set<String> restrictedMentionNames) {
    this.restrictedMentionNames = restrictedMentionNames;
  }

  public void setForceGraphCreation(boolean flag) {
    this.forceGraphCreation = flag;
  }
  
  public boolean isForceGraphCreation() {
    return forceGraphCreation;
  }

  public DataSource getDataSource() {
    return ds;
  }

  public boolean shouldGraphTrace() {
    return shouldGraphTrace;
  }
  
  public void setShouldGraphTrace(boolean flag) {
    shouldGraphTrace = flag;
  }

  
  public int getNumParallelThreads() {
    return numParallelThreads;
  }

  
  public void setNumParallelThreads(int numParallelThreads) {
    this.numParallelThreads = numParallelThreads;
  }

  public boolean shouldIncludeNMEMentions() {
    return cr.getSettings().isIncludeNMEMentions();
  }

  public void setShouldGenerateLinkCounts(boolean flag) {
    shouldGenerateLinkCounts = flag;
  }
  
  public boolean shouldGenerateLinkCounts() {
    return shouldGenerateLinkCounts ;
  }

  public String getPreviousResults() {
    return previousResults;
  }

  public void setPreviousResults(String previousResults) {
    this.previousResults = previousResults;
  }
}
