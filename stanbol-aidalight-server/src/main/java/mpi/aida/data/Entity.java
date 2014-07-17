package mpi.aida.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javatools.parsers.Char;

public class Entity implements Serializable, Comparable<Entity>, Cloneable {

  private static final long serialVersionUID = 131444964369556633L;

  private String name;
  
  private Set<String> wikiTypes;
  
  private List<String> surroundingMentionNames;
  
  private int[] kpTokens = null;
  
  private double localsim = -1.0;

  private int id = -1;
  
  private Mention mention;
  
  private boolean waitforRemoved = false;
  
  public boolean isDisambiguated = false;
  
  public static final String NO_MATCHING_ENTITY = "--NME--";
  
  public static final String OOKBE = "--NME--";
  
  private double firstLocalSim = -1.0;
  
  public double getFirstLocalSim() {
    return firstLocalSim;
  }
  

  /**
   * Use this field to represent the mention-entity similarity computed with 
   * some method (not the score stored in the DB). This field will not be set 
   * in the constructor. We set it later on, when we compute the similarity
   */
  private double mentionEntitySimilarity;
  
  private Collection<Entity> confusableEntities = new HashSet<Entity>();

  public Entity(String name, int id) {
    this.name = name;
    this.mentionEntitySimilarity = -1.0;
    this.id = id;
  }

  public String getName() {
    return name;
  }
  
  public String toString() {
    return name + " (" + id + ")";
  }

  public String tohtmlString() {
    return "<td></td><td></td><td>" + Char.toHTML(name) + "</td><td></td><td></td><td></td>";
  }

  public int getId() {
    return id;
  }
  
  public void setConfusableEntities(Collection<Entity> entities) {
    confusableEntities = entities;
  }
  
  public Collection<Entity> getConfusableEntities() {
    return confusableEntities;
  }
  
  
  public Mention getMention() {
    return mention;
  }
  
  public void setMention(Mention mention) {
    this.mention = mention;
  }
  
  public boolean getWaitforRemoved() {
    return waitforRemoved;
  }
  
  public void setWaitforRemoved(boolean waitforRemoved) {
    this.waitforRemoved = waitforRemoved;
  }

  public double getMentionEntitySimilarity() {
    return this.mentionEntitySimilarity;
  }

  public void setMentionEntitySimilarity(double mes) {
    this.mentionEntitySimilarity = mes;
  }
  
  public int[] getKpTokens() {
    return kpTokens;
  }
  
  public void setKpTokens(int[] kpTokens) {
    this.kpTokens = kpTokens;
  }
  
  public double getLocalSimilarity() {
    return localsim;
  }
  
  public void setLocalSimilarity(double localsim) {
    if(firstLocalSim == -1.0)
      firstLocalSim = localsim;
    this.localsim = localsim;
  }

  public int compareTo(Entity e) {
    String str1 = name;
    if(mention!= null)
      str1 += mention.getMention() + mention.getType();
    String str2 = e.getName();
    if(e.getMention() != null)
      str2 += e.getMention().getMention() + e.getMention().getType();
    return str1.compareTo(str2);
//    return name.compareTo(e.getName());
  }
  
  public void setWikiTypes(Set<String> wikiTypes) {
    this.wikiTypes = wikiTypes;
  }
  
  public Set<String> getWikiTypes() {
    return wikiTypes;
  }
  
  public boolean equals(Object o) {
    if (o instanceof Entity) {
      Entity e = (Entity) o;
      String str1 = name;
      if(mention!= null)
        str1 += mention.getMention() + mention.getType();
      String str2 = e.getName();
      if(e.getMention() != null)
        str2 += e.getMention().getMention() + e.getMention().getType();
      return str1.equalsIgnoreCase(str2);
//      return name.equals(e.getName());
    } else {
      return false;
    }
  }
  
  public int hashCode() {
    return name.hashCode();
  }

//  public boolean isNMEentity() {
//    return Entities.isNMEName(name);
//  }

  public String getNMEnormalizedName() {
    String normName = name.replace("-"+NO_MATCHING_ENTITY, "").replace(' ', '_');
    return normName;
  }

  public List<String> getSurroundingMentionNames() {
    return surroundingMentionNames;
  }

  public void setSurroundingMentionNames(List<String> surroundingMentionNames) {
    this.surroundingMentionNames = surroundingMentionNames;
  }
}
