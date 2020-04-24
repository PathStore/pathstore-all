package pathstoreweb.pathstoreadminpanel.controllers;

import javax.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.ValidityErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.GetApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnInstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.UpdateApplicationStatePayload;
import pathstoreweb.pathstoreadminpanel.services.applications.AddApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.GetApplications;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload;
import pathstoreweb.pathstoreadminpanel.services.servers.AddServer;
import pathstoreweb.pathstoreadminpanel.services.servers.GetServers;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload;
import pathstoreweb.pathstoreadminpanel.services.topology.GetNetworkTopology;

/** Main controller for api. TODO: split up to sub functions */
@RestController
@RequestMapping(Endpoints.API)
public class ApiV1 {

  /**
   * @return json array, of topology diagram
   * @see GetNetworkTopology
   */
  @GetMapping(Endpoints.TOPOLOGY)
  public String topology() {
    return new GetNetworkTopology().response();
  }

  /**
   * @return json array, of current states on each node.
   * @see GetApplicationState
   */
  @GetMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String getApplicationState() {
    return new GetApplicationState().response();
  }

  /**
   * @param updateApplicationStatePayload {@link UpdateApplicationStatePayload}
   * @param bindingResult result of validation
   * @return response
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementInstall(
      @Valid final UpdateApplicationStatePayload updateApplicationStatePayload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new InstallApplication(updateApplicationStatePayload).response();
  }

  /**
   * @param updateApplicationStatePayload {@link UpdateApplicationStatePayload}
   * @param bindingResult result of validation
   * @return response
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementRemove(
      @Valid final UpdateApplicationStatePayload updateApplicationStatePayload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new UnInstallApplication(updateApplicationStatePayload).response();
  }

  /** @return List of applications on the system */
  @GetMapping(Endpoints.APPLICATIONS)
  public String getApplications() {
    return new GetApplications().response();
  }

  /**
   * TODO: Build a schema builder. Properties: 1 keyspace. n tables, with some number of attributes
   * each
   *
   * @param payload user passed payload
   * @param bindingResult results from validation
   * @return todo
   */
  @PostMapping(Endpoints.APPLICATIONS)
  public String addApplication(
      @Valid final AddApplicationPayload payload, final BindingResult bindingResult) {

    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new AddApplication(payload).response();
  }

  /** @return todo */
  @DeleteMapping(Endpoints.APPLICATIONS)
  public String removeApplication() {
    return "Not Supported";
  }

  /** @return JSON Array of all servers created */
  @GetMapping(Endpoints.SERVERS)
  public String getServers() {
    return new GetServers().response();
  }

  @PostMapping(Endpoints.SERVERS)
  public String addServer(@Valid final AddServerPayload payload, final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new AddServer(payload).response();
  }
}
