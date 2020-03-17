package pathstoreweb.pathstoreadminpanel.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnInstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.AddApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.AvailableApplications;
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
   * TODO: User input validation / error handling
   *
   * @param keyspace keyspace to deploy
   * @param nodes array of nodes to install on.
   * @return todo
   * @see InstallApplication
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementInstall(
      @RequestParam("keyspace") final String keyspace, @RequestParam("node") final int[] nodes) {
    return new InstallApplication(keyspace, nodes).response();
  }

  /**
   * TODO: User input validation / error handling
   *
   * @param keyspace keyspace to remove
   * @param nodes array of nodes to remove from.
   * @return todo
   * @see UnInstallApplication
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementRemove(
      @RequestParam("keyspace") final String keyspace, @RequestParam("node") final int[] nodes) {
    return new UnInstallApplication(keyspace, nodes).response();
  }

  /** @return todo */
  @GetMapping(Endpoints.APPLICATIONS)
  public String getApplications() {
    return new AvailableApplications().response();
  }

  /**
   * TODO: User input validation / error handling
   *
   * @return todo
   */
  @PostMapping(value = Endpoints.APPLICATIONS)
  public String addApplication(
      @RequestParam("application_name") final String applicationName,
      @RequestParam("application") final MultipartFile multipart) {

    return new AddApplication(applicationName, multipart).response();
  }

  /**
   * TODO: User input validation / error handling
   *
   * <p>TODO: call {@link UnInstallApplication} on root
   *
   * @return todo
   */
  @DeleteMapping(Endpoints.APPLICATIONS)
  public String removeApplication() {
    return "Not Supported";
  }
}
