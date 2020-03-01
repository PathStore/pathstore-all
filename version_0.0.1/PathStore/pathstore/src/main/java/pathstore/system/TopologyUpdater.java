package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.PathStoreProperties;

/**
 * Simple process that runs on startup to update the network topology table of there node is and
 * their parent node. If there is already a record of a node with this id and the parent node is
 * different the process will be killed. We currently don't support migration of nodes
 */
public class TopologyUpdater {

  /**
   * Calls {@link #update_table()}
   *
   * <p>This class will have more uses once network reconfiguration is possible
   */
  TopologyUpdater() {
    this.update_table();
  }

  /**
   * This function checks to see if there is already a network definition for the defined nodeid. If
   * there is it checks to see if the parent node is the same. If it is then nothing happens. If
   * there is a difference then we exit the program with an error message.
   *
   * <p>If there is no definition for this nodeid we create it
   */
  private void update_table() {
    PathStoreProperties properties = PathStoreProperties.getInstance();
    int nodeid = properties.NodeID;
    int parent_nodeid = properties.ParentID;

    for (Row row :
        PathStorePriviledgedCluster.getInstance()
            .connect()
            .execute(
                QueryBuilder.select("nodeid", "parent_nodeid")
                    .from("pathstore_applications", "topology"))) {
      int row_nodeid = row.getInt("nodeid");
      int row_parent_nodeid = row.getInt("parent_nodeid");

      if (nodeid == row_nodeid && parent_nodeid == row_parent_nodeid) return;
      else {
        System.err.println(
            "Either this node is using a nodeid that is already in use or you are trying to change the network topology. Currently this operation is not supported. If you want like to do this you will need to reset the network.");
        System.exit(-1);
      }
    }

    Session session = PathStoreCluster.getInstance().connect();

    Insert insert = QueryBuilder.insertInto("pathstore_applications", "topology");
    insert.value("nodeid", nodeid);
    insert.value("parent_nodeid", parent_nodeid);

    session.execute(insert);
  }
}
