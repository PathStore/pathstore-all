package pathstoreweb.pathstoreadminpanel.controllers;

import org.springframework.web.bind.annotation.*;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.RemoveApplication;
import pathstoreweb.pathstoreadminpanel.services.topology.NetworkTopology;

/** Main controller for api. TODO: split up to sub functions */
@RestController
@RequestMapping(Endpoints.API)
public class ApiV1 {

  /**
   * @return json array, of topology diagram
   * @see NetworkTopology
   */
  @GetMapping(Endpoints.TOPOLOGY)
  public String topology() {
    return new NetworkTopology().response();
  }

  /**
   * @return json array, of current states on each node.
   * @see ApplicationState
   */
  @GetMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String getApplicationState() {
    return new ApplicationState().response();
  }

  /**
   * @param keyspace keyspace to deploy
   * @param nodes array of nodes to install on.
   * @return todo
   * @see DeployApplication
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementInstall(
      @RequestParam("keyspace") final String keyspace, @RequestParam("node") final int[] nodes) {
    return new DeployApplication(keyspace, nodes).response();
  }

  /**
   * @param keyspace keyspace to remove
   * @param nodes array of nodes to remove from.
   * @return todo
   * @see RemoveApplication
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementRemove(
      @RequestParam("keyspace") final String keyspace, @RequestParam("node") final int[] nodes) {
    return new RemoveApplication(keyspace, nodes).response();
  }
}
