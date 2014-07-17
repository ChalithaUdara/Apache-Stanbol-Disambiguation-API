package mpi.aida.data;

import mpi.aida.access.DataAccess;


/**
 * Placeholder for an out-of-knowledge-base entity. The identifier is given
 * by the surface form it is referred to (i.e. its name) plus a --OOKBE-- suffix. 
 */
public class OokbEntity extends Entity {
  
  private static final long serialVersionUID = -8291569958563336476L;
  
  public OokbEntity(String name, int id) {
    super(name + "-" + OOKBE, id);
  }

  public OokbEntity(String name) {
    super(name + "-" + OOKBE, DataAccess.getIdForName(name));
  }
}
