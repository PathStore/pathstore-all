package pathstoreweb.pathstoreadminpanel.services.topology;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.topology.formatter.GetNetworkTopologyFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.util.LinkedList;
import java.util.List;

/**
 * This service queries all topology records in the topology table
 *
 * @see GetNetworkTopologyFormatter
 * @see Constants#TOPOLOGY
 * @see Constants.TOPOLOGY_COLUMNS
 */
public class GetNetworkTopology implements IService {
  /**
   * @return json response
   * @see GetNetworkTopologyFormatter
   */
  @Override
  public String response() {
    return new GetNetworkTopologyFormatter(this.getTopologyEntries()).format();
  }

  /**
   * Queries a list of topology entries
   *
   * @return list of topology entries
   * @see TopologyEntry
   */
  public List<TopologyEntry> getTopologyEntries() {
    LinkedList<TopologyEntry> entries = new LinkedList<>();

    Session session = PathStoreCluster.getInstance().connect();

    Select queryTopology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    for (Row row : session.execute(queryTopology))
      entries.addFirst(
          new TopologyEntry(
              row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID),
              row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID)));

    return entries;
  }
}
