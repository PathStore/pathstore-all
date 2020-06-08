package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import java.util.List;

/** Denotes what a single application record looks like */
public class ApplicationRecord {
  /** Node id to perform some operation on */
  public int nodeId;

  /** What keyspace to perform that opertion on */
  public String keyspaceName;

  /** List of nodes to wait for */
  public List<Integer> waitFor;
}
