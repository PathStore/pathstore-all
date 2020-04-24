package pathstoreweb.pathstoreadminpanel.services.topology.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.topology.GetNetworkTopology;
import pathstoreweb.pathstoreadminpanel.services.topology.TopologyEntry;

import java.util.List;

/**
 * Network Topology Formatter
 *
 * @see GetNetworkTopology
 */
public class GetNetworkTopologyFormatter implements IFormatter {

  /** List of topology entries */
  private final List<TopologyEntry> entryList;

  /** @param entryList {@link #entryList} */
  public GetNetworkTopologyFormatter(final List<TopologyEntry> entryList) {
    this.entryList = entryList;
  }

  /**
   * Json array of topology entries. See read me for example.
   *
   * @return json array
   */
  @Override
  public String format() {
    JSONArray array = new JSONArray();

    for (TopologyEntry entry : this.entryList) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.TOPOLOGY_COLUMNS.NODE_ID, entry.nodeId)
          .put(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID, entry.parentNodeId);

      array.put(object);
    }

    return array.toString();
  }
}
