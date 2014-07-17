package mpi.aidalight.entitycoherence;


public class GraphSettingsExperiment extends GraphSettings{
  /*
   * use StandordNED to annotate mentions.
   */
  private boolean useStanfordNER = false;
  
  @Override
  public boolean isUsingStanfordNER() {
    return useStanfordNER;
  }
  
  
  /*
   * if labelMentionTypebyStanfordNER, mention types are labelled by StanfordNER. Otherwise,
   * they are predicted from target entities.
   * 
   */
  private boolean labelMentionTypebyStanfordNER = true;
  
  public boolean isLabelingMentionTypeByNER() {
    return labelMentionTypebyStanfordNER;
  }
  
  public void setLabelMentionTypeByStanfordNER(boolean labelMentionTypebyStanfordNER) {
    this.labelMentionTypebyStanfordNER = labelMentionTypebyStanfordNER;
  }
  
}
