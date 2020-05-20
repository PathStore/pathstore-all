package pathstoreweb.pathstoreadminpanel.controllers;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
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
import pathstoreweb.pathstoreadminpanel.services.availablelogdates.GetAvailableLogDates;
import pathstoreweb.pathstoreadminpanel.services.deployment.AddDeploymentRecords;
import pathstoreweb.pathstoreadminpanel.services.deployment.GetDeploymentRecords;
import pathstoreweb.pathstoreadminpanel.services.deployment.UpdateDeploymentRecord;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.UpdateDeploymentRecordPayload;
import pathstoreweb.pathstoreadminpanel.services.logs.GetLogRecords;
import pathstoreweb.pathstoreadminpanel.services.logs.payload.GetLogRecordsPayload;
import pathstoreweb.pathstoreadminpanel.services.servers.AddServer;
import pathstoreweb.pathstoreadminpanel.services.servers.DeleteServer;
import pathstoreweb.pathstoreadminpanel.services.servers.GetServers;
import pathstoreweb.pathstoreadminpanel.services.servers.UpdateServer;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.DeleteServerPayload;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.UpdateServerPayload;

/** Main controller for api. TODO: split up to sub functions */
@RestController
@RequestMapping(Endpoints.API)
public class ApiV1 {

  /**
   * @return json array, of current states on each node.
   * @see GetApplicationState
   */
  @GetMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> getApplicationState() {
    return new GetApplicationState().response();
  }

  /**
   * @param updateApplicationStatePayload {@link UpdateApplicationStatePayload}
   * @param bindingResult result of validation
   * @return response
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> applicationManagementInstall(
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
  public ResponseEntity<String> applicationManagementRemove(
      @Valid final UpdateApplicationStatePayload updateApplicationStatePayload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new UnInstallApplication(updateApplicationStatePayload).response();
  }

  /** @return List of applications on the system */
  @GetMapping(Endpoints.APPLICATIONS)
  public ResponseEntity<String> getApplications() {
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
  public ResponseEntity<String> addApplication(
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
  public ResponseEntity<String> getServers() {
    return new GetServers().response();
  }

  /**
   * Endpoint used to add a server record to the servers table. This is used during deployment of a
   * new node
   *
   * @param payload {@link AddServerPayload}
   * @param bindingResult containers errors generated by the validators
   * @return either errors or success response
   */
  @PostMapping(Endpoints.SERVERS)
  public ResponseEntity<String> addServer(
          @Valid final AddServerPayload payload, final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new AddServer(payload).response();
  }

  /**
   * Endpoint used to update a servers properties
   *
   * @param payload {@link UpdateServerPayload}
   * @param bindingResult containers errors generated by the validators
   * @return either errors or success response
   */
  @PutMapping(Endpoints.SERVERS)
  public ResponseEntity<String> upadteServer(
          @Valid final UpdateServerPayload payload, final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new UpdateServer(payload).response();
  }

  /**
   * Endpoint is used to delete a server object from the network. If say for example someone added
   * it by accident
   *
   * @param payload {@link DeleteServerPayload}
   * @param bindingResult containers errors generated by the validators
   * @return either errors or success response
   */
  @DeleteMapping(Endpoints.SERVERS)
  public ResponseEntity<String> deleteServer(
      @Valid final DeleteServerPayload payload, final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new DeleteServer(payload).response();
  }

  /**
   * Get request for deployment records.
   *
   * @return returns all deployment records in the deployment table. No errors possible
   */
  @GetMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy() {
    return new GetDeploymentRecords().response();
  }

  /**
   * Post request for deployment records.
   *
   * @param payload set of records to add
   * @param bindingResult whether or not the payload is valid or not
   * @return response. Either 200 or 400
   */
  @PostMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy(
      @RequestBody @Valid final AddDeploymentRecordPayload payload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new AddDeploymentRecords(payload).response();
  }

  /**
   * Put request for deployment records to transfer a failed record to deploying
   *
   * @param payload valid payload
   * @param bindingResult whether or not payload is valid
   * @return response. Either 200 or 400
   */
  @PutMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy(
      @RequestBody @Valid final UpdateDeploymentRecordPayload payload,
      final BindingResult bindingResult) {
    return bindingResult.hasErrors()
        ? new ValidityErrorFormatter(bindingResult.getAllErrors()).format()
        : new UpdateDeploymentRecord(payload).response();
  }

  /** @return list of logs for the user to see for each node */
  @GetMapping(Endpoints.LOGS)
  public ResponseEntity<String> logs(final GetLogRecordsPayload payload) {
    return new GetLogRecords(payload).response();
  }

  /** @return list of dates available for each log */
  @GetMapping(Endpoints.AVAILABLE_LOG_DATES)
  public ResponseEntity<String> getAvailableLogs() {
    return new GetAvailableLogDates().response();
  }
}
