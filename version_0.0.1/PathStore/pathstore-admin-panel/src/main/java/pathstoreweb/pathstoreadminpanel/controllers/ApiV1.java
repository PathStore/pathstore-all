package pathstoreweb.pathstoreadminpanel.controllers;

import org.springframework.web.bind.annotation.*;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplication;
import pathstoreweb.pathstoreadminpanel.services.topology.NetworkTopology;

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
   * @param keyspace Valid pathstore keyspace
   * @param nodes array of nodes to install on.
   * @return either successful or non-successful
   * @see DeployApplication
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementInstall(
      @RequestParam("keyspace") String keyspace, @RequestParam("node") int[] nodes) {
    return new DeployApplication(keyspace, nodes).response();
  }
}
