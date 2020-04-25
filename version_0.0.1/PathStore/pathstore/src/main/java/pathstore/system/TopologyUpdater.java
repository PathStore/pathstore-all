package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;

/**
 * Simple process that runs on startup to update the network topology table of there node is and
 * their parent node. If there is already a record of a node with this id and the parent node is
 * different the process will be killed. We currently don't support migration of nodes
 */
public class TopologyUpdater {

  /**
   * This function checks to see if there is already a network definition for the defined nodeid. If
   * there is it checks to see if the parent node is the same. If it is then nothing happens. If
   * there is a difference then we exit the program with an error message.
   *
   * <p>If there is no definition for this nodeid we create it
   */
  public void updateTable() {
    PathStoreProperties properties = PathStoreProperties.getInstance();
    int nodeid = properties.NodeID;
    int parent_nodeid = properties.ParentID;

    for (Row row :
        PathStorePriviledgedCluster.getInstance()
            .connect()
            .execute(
                QueryBuilder.select(
                        Constants.TOPOLOGY_COLUMNS.NODE_ID,
                        Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID)
                    .from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY))) {
      int row_nodeid = row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID);
      int row_parent_nodeid = row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID);

      if (nodeid == row_nodeid && parent_nodeid == row_parent_nodeid) return;
      else {
        System.err.println(
            "Either this node is using a nodeid that is already in use or you are trying to change the network topology. Currently this operation is not supported. If you want like to do this you will need to reset the network.");
        System.exit(-1);
      }
    }

    Session session = PathStoreCluster.getInstance().connect();

    Insert insert = QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);
    insert.value(Constants.TOPOLOGY_COLUMNS.NODE_ID, nodeid);
    insert.value(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID, parent_nodeid);

    session.execute(insert);
  }
}
