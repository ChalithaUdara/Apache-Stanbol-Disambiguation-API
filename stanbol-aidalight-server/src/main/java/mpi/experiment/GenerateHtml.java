package mpi.experiment;

import java.io.IOException;

import mpi.experiment.reader.CollectionReader;
import mpi.experiment.reader.CollectionReader.DataSource;
import mpi.experiment.util.htmloutput.CompileFinalResult;

import org.apache.commons.cli.ParseException;

public class GenerateHtml {

  public static enum OverviewResultType {
    MACRO_PRECISION, NME_F1, NER_F1 
  }  
  /**
   * @param args
   * @throws ParseException 
   * @throws IOException 
   */
  public static void main(String[] args) throws ParseException, IOException {
    if (args.length < 2 || args.length > 4) {
      System.out.println("Usage:\n GenerateHtml <path-to-collection> <collection-type> [OverviewResultType - MACRO_PRECISION, NME_F1, NER_F1] [collection-part]");
      System.exit(1);
    }
    
    String collectionPath = args[0]; // collection path
    String dataSetType = args[1]; // collection type
    String collectionPart = null;
    OverviewResultType overviewResultType = OverviewResultType.MACRO_PRECISION;
    if (args.length > 2) {
      overviewResultType = OverviewResultType.valueOf(args[2]);
    }
    if (args.length > 3) {
      collectionPart = args[3];
    }
    
    DataSource ds = DataSource.NONE;
    if (dataSetType.equals(CollectionReader.CONLL)) {
      ds = DataSource.CONLL;
    } else if (dataSetType.equals(CollectionReader.WIKIPEDIA_YAGO2)) {
      ds = DataSource.WIKIPEDIA_YAGO2;
    } else if (dataSetType.equals(CollectionReader.AIDA)) {
      ds = DataSource.AIDA;
    } else if (dataSetType.equals(CollectionReader.NEWSSTREAMS)) {
      ds = DataSource.NEWSSTREAMS;
    } else if (dataSetType.equals(CollectionReader.GIGAWORD5)) {
      ds = DataSource.GIGAWORD5;
    }
    
    System.out.print("Writing HTML... ");
    
    // write html
    switch (ds) {
      case CONLL:
        new CompileFinalResult(overviewResultType, collectionPart).processCONLL(collectionPath, "100");
        break;
      case WIKIPEDIA_YAGO2:
        new CompileFinalResult(overviewResultType, collectionPart).processWIKIPEDIA_YAGO2(collectionPath, "100");
        break;
      case NEWSSTREAMS:
        new CompileFinalResult(overviewResultType, collectionPart).processNEWSSTREAMS(collectionPath, "100");
        break;
      case GIGAWORD5:
        new CompileFinalResult(overviewResultType, collectionPart).processGIGAWORD5(collectionPath, "100");
        break;
      case AIDA:
        new CompileFinalResult(overviewResultType, collectionPart).processAIDA50(collectionPath, "100");
        break;
      default:
        break;
    }
    
    System.out.println("DONE");
  }
}
