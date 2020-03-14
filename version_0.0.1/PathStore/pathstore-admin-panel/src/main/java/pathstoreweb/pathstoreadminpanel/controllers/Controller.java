package pathstoreweb.pathstoreadminpanel.controllers;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.apache.commons.lang3.EnumUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.recsources.InstallMethods;

import java.util.HashMap;

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
   * TODO: handle method being invalid
   *
   * <p>TODO: handle keyspace being invalid (C1: non-pathstore, C2:doesn't exist)
   *
   * <p>TODO: Actual algorithm
   *
   * @param method {@link InstallMethods}
   * @param keyspace_name Valid pathstore keyspace_name
   * @param nodes array of nodes to install on.
   * @return either successful or non-successful
   */
  @GetMapping("/install")
  public String install(
      @RequestParam("method") String method,
      @RequestParam("keyspace_name") String keyspace_name,
      @RequestParam("node") int[] nodes) {

    if (!EnumUtils.isValidEnum(InstallMethods.class, method.toUpperCase())) {
      System.out.println("invalid enum");
      return "error";
    }

    Session session = PathStoreCluster.getInstance().connect();

    Select query_available_applications =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    query_available_applications.where(
        QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, keyspace_name));

    boolean invalid_keyspace_name = true;

    for (Row row : session.execute(query_available_applications))
      if (row.getString(Constants.APPS_COLUMNS.KEYSPACE_NAME).equals(keyspace_name))
        invalid_keyspace_name = false;

    if (invalid_keyspace_name) {
      System.out.println("invalid keyspace");
      return "error";
    }

    // algorithm WIP

    Select query_topology =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.TOPOLOGY);

    HashMap<Integer, Integer> child_to_parent = new HashMap<>();
    for (Row row : session.execute(query_topology)) {
      child_to_parent.put(
          row.getInt(Constants.TOPOLOGY_COLUMNS.NODE_ID),
          row.getInt(Constants.TOPOLOGY_COLUMNS.PARENT_NODE_ID));
    }

    for (int current_node : nodes) {
      if (!child_to_parent.containsKey(current_node)) {
        System.out.println("Error nodeid " + current_node + " is not a valid node in the topology");
      }
    }

    return "";
  }
}
