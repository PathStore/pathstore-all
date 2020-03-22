package pathstoreweb.pathstoreadminpanel.controllers;

import javax.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.ErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnInstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.ApplicationManagementPayload;
import pathstoreweb.pathstoreadminpanel.services.applications.AddApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.AvailableApplications;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload;
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
   * TODO: Handle conflict errors
   *
   * @param applicationManagementPayload {@link ApplicationManagementPayload}
   * @param bindingResult result of validation
   * @return response
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementInstall(
      @Valid final ApplicationManagementPayload applicationManagementPayload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ErrorFormatter(bindingResult.getAllErrors()).format()
        : new InstallApplication(applicationManagementPayload).response();
  }

  /**
   * TODO: Handle conflict errors
   *
   * @param applicationManagementPayload {@link ApplicationManagementPayload}
   * @param bindingResult result of validation
   * @return response
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public String applicationManagementRemove(
      @Valid final ApplicationManagementPayload applicationManagementPayload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ErrorFormatter(bindingResult.getAllErrors()).format()
        : new UnInstallApplication(applicationManagementPayload).response();
  }

  /** @return todo */
  @GetMapping(Endpoints.APPLICATIONS)
  public String getApplications() {
    return new AvailableApplications().response();
  }

  /**
   * TODO: Handle if schema is bad.
   *
   * <p>TODO: Handle if schema name is not the same as passed application name
   *
   * @param payload user passed payload
   * @param bindingResult results from validation
   * @return todo
   */
  @PostMapping(Endpoints.APPLICATIONS)
  public String addApplication(
      @Valid final AddApplicationPayload payload, final BindingResult bindingResult) {

    return bindingResult.hasErrors()
        ? new ErrorFormatter(bindingResult.getAllErrors()).format()
        : new AddApplication(payload).response();
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
