package pathstoreweb.pathstoreadminpanel.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.ValidityErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.GetApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.InstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnInstallApplication;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.ModifyApplicationStatePayload;
import pathstoreweb.pathstoreadminpanel.services.applications.AddApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.GetApplications;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload;
import pathstoreweb.pathstoreadminpanel.services.availablelogdates.GetAvailableLogDates;
import pathstoreweb.pathstoreadminpanel.services.deployment.AddDeploymentRecords;
import pathstoreweb.pathstoreadminpanel.services.deployment.DeleteDeploymentRecords;
import pathstoreweb.pathstoreadminpanel.services.deployment.GetDeploymentRecords;
import pathstoreweb.pathstoreadminpanel.services.deployment.UpdateDeploymentRecord;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.DeleteDeploymentRecordPayload;
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
   * @param modifyApplicationStatePayload {@link ModifyApplicationStatePayload}
   * @return response
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> applicationManagementInstall(
      final ModifyApplicationStatePayload modifyApplicationStatePayload) {
    return modifyApplicationStatePayload.hasErrors()
        ? new ValidityErrorFormatter(modifyApplicationStatePayload.getErrors()).format()
        : new InstallApplication(modifyApplicationStatePayload).response();
  }

  /**
   * @param modifyApplicationStatePayload {@link ModifyApplicationStatePayload}
   * @return response
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> applicationManagementRemove(
      final ModifyApplicationStatePayload modifyApplicationStatePayload) {
    return modifyApplicationStatePayload.hasErrors()
        ? new ValidityErrorFormatter(modifyApplicationStatePayload.getErrors()).format()
        : new UnInstallApplication(modifyApplicationStatePayload).response();
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
   * @return todo
   */
  @PostMapping(Endpoints.APPLICATIONS)
  public ResponseEntity<String> addApplication(final AddApplicationPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
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
   * @return either errors or success response
   */
  @PostMapping(Endpoints.SERVERS)
  public ResponseEntity<String> addServer(final AddServerPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new AddServer(payload).response();
  }

  /**
   * Endpoint used to update a servers properties
   *
   * @param payload {@link UpdateServerPayload}
   * @return either errors or success response
   */
  @PutMapping(Endpoints.SERVERS)
  public ResponseEntity<String> updateServer(final UpdateServerPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new UpdateServer(payload).response();
  }

  /**
   * Endpoint is used to delete a server object from the network. If say for example someone added
   * it by accident
   *
   * @param payload {@link DeleteServerPayload}
   * @return either errors or success response
   */
  @DeleteMapping(Endpoints.SERVERS)
  public ResponseEntity<String> deleteServer(final DeleteServerPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
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
   * @return response. Either 200 or 400
   */
  @PostMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy(@RequestBody final AddDeploymentRecordPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new AddDeploymentRecords(payload).response();
  }

  /**
   * Put request for deployment records to transfer a failed record to deploying
   *
   * @return response. Either 200 or 400
   */
  @PutMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy(@RequestBody final UpdateDeploymentRecordPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new UpdateDeploymentRecord(payload).response();
  }

  /**
   * TODO: Validation and error checking
   *
   * <p>Delete request for deployment records to remove nodes from the network
   *
   * @param payload payload for node to delete
   * @return response. Either 200 or 400
   */
  @DeleteMapping(Endpoints.DEPLOYMENT)
  public ResponseEntity<String> deploy(@RequestBody final DeleteDeploymentRecordPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new DeleteDeploymentRecords(payload).response();
  }

  /** @return list of logs for the user to see for each node */
  @GetMapping(Endpoints.LOGS)
  public ResponseEntity<String> logs(final GetLogRecordsPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new GetLogRecords(payload).response();
  }

  /** @return list of dates available for each log */
  @GetMapping(Endpoints.AVAILABLE_LOG_DATES)
  public ResponseEntity<String> getAvailableLogs() {
    return new GetAvailableLogDates().response();
  }
}
