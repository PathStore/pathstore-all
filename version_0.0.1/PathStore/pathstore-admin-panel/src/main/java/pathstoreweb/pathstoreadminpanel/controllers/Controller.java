package pathstoreweb.pathstoreadminpanel.controllers;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplication;

@RestController
@RequestMapping("/api")
public class Controller {

  @GetMapping("/topology")
  public String topology() {

    Session session = PathStoreCluster.getInstance().connect();

    Select query_topology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    JSONArray array = new JSONArray();
    for (Row row : session.execute(query_topology)) {
      JSONObject topology_entry = new JSONObject();
      topology_entry.put(
          Constants.TOPOLOGY_COLUMNS.NODE_ID, row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID));
      topology_entry.put(
          Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID,
          row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID));
      array.put(topology_entry);
    }
    return array.toString();
  }

  /**
   * @param keyspace Valid pathstore keyspace
   * @param nodes array of nodes to install on.
   * @return either successful or non-successful
   */
  @PostMapping("/install")
  public String install(
      @RequestParam("keyspace") String keyspace, @RequestParam("node") int[] nodes) {
    return new DeployApplication(keyspace, nodes).response();
  }
}
