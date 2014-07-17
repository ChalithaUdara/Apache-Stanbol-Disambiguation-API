package mpi.experiment.reader.util;

import basics.Normalize;

public class WikipediaLink {

  private String text = null;

  private String targetOriginal = null;

  private String target = null;

  private int offset = -1;
  
  private int originalOffset = -1;

  private int length = -1;

  public WikipediaLink(String text, int start) {
    this(text, start, -1);
  }
  
  public WikipediaLink(String text, int start, int originalStart) {
    if (text.startsWith("[[") && text.endsWith("]]")) {
      text = text.substring(2, text.length() - 2);
      int split = text.indexOf("|");
      if (split != -1) {
        this.text = text.substring(split + 1);
        this.target = text.substring(0, split);
      } else {
        this.text = text;
        this.target = text;
      }
    } else {
      this.text = text;
    }
    offset = start;
    originalOffset = originalStart;
    length = this.text.length();
    //    cleanTarget();
    if (this.target != null) {
      this.targetOriginal = target;
      this.target = Normalize.entity(this.target);
    }
  }

  public String getText() {
    return text;
  }

  public String getTarget() {
    return target;
  }

  public String getTargetOriginal(){
    return targetOriginal;
  }

  public int getOffset() {
    return offset;
  }
  
  public int getOriginalOffset() {
    return originalOffset;
  }

  public int getLength() {
    return length;
  }

  public String toString() {
    if (target == null) {
      return text + " (" + offset + ", " + length + ")";
    }
    return text + " <" + target + "> (" + offset + ", " + length + ")";
  }
  /*  // insert table data
    public static void main(String[] args) {
      DBConnection con = null;
      try {
        con = LKManager.getConnectionForDatabase(LKManager.DATABASE_YAGO_MEANS, "insert");
        DBPreparedStatementInterface stat = con.prepareStatement("insert into redirect values(?,?)");
        File f = new File("./data/experiment/CHAKRABARTI/redirect_classes.tsv");
        FileReader r = new FileReader(f);
        BufferedReader reader = new BufferedReader(r);
        String line = null;
        while ((line = reader.readLine()) != null) {
          String[] data = line.split("\t");
          stat.setString(1, data[0]);
          stat.setString(2, data[1]);
          stat.addBatch();
        }
        stat.executeBatch();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        LKManager.releaseConnection(LKManager.DATABASE_YAGO_MEANS, con);
      }
    }
    
    */
}
