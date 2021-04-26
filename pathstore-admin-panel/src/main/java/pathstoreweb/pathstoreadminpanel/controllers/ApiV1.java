/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package pathstoreweb.pathstoreadminpanel.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import pathstoreweb.pathstoreadminpanel.Endpoints;
import pathstoreweb.pathstoreadminpanel.services.ValidityErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplications;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.GetApplicationState;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnDeployApplications;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.AddApplicationDeploymentRecordPayload;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.DeleteApplicationDeploymentRecordPayload;
import pathstoreweb.pathstoreadminpanel.services.applications.AddApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.GetApplications;
import pathstoreweb.pathstoreadminpanel.services.applications.RemoveApplication;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.RemoveApplicationPayload;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/** Main controller for api. */
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
   * deploy applications on the network
   *
   * @return {}
   */
  @PostMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> deployApplication(
      @RequestBody final AddApplicationDeploymentRecordPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new DeployApplications(payload).response();
  }

  /**
   * un-deploy applications on the network
   *
   * @return {}
   */
  @DeleteMapping(Endpoints.APPLICATION_MANAGEMENT)
  public ResponseEntity<String> unDeployApplication(
      @RequestBody final DeleteApplicationDeploymentRecordPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new UnDeployApplications(payload).response();
  }

  /** @return List of applications on the system */
  @GetMapping(Endpoints.APPLICATIONS)
  public ResponseEntity<String> getApplications() {
    return new GetApplications().response();
  }

  /**
   * Take in a schema and an application name
   *
   * @param payload user passed payload
   * @return {}
   */
  @PostMapping(Endpoints.APPLICATIONS)
  public ResponseEntity<String> addApplication(final AddApplicationPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new AddApplication(payload).response();
  }

  /**
   * This endpoint removes an application from the apps table
   *
   * @param payload payload from user {@link RemoveApplicationPayload}
   * @return {} 200 or 400 with errors
   */
  @DeleteMapping(Endpoints.APPLICATIONS)
  public ResponseEntity<String> removeApplication(
      @RequestBody final RemoveApplicationPayload payload) {
    return payload.hasErrors()
        ? new ValidityErrorFormatter(payload.getErrors()).format()
        : new RemoveApplication(payload).response();
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

  /**
   * This is used to allow put requests to send
   *
   * @return multipart resolver
   */
  @Bean
  public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver() {
      @Override
      public boolean isMultipart(HttpServletRequest request) {
        String method = request.getMethod().toLowerCase();
        // By default, only POST is allowed. Since this is an 'update' we should accept PUT.
        if (!Arrays.asList("put", "post").contains(method)) {
          return false;
        }
        String contentType = request.getContentType();
        return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
      }
    };
  }
}
