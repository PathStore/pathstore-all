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

package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.DeleteDeploymentRecordPayload;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This service takes a valid delete deployment payload and writes all the waiting for removal
 * records to the table
 */
public class DeleteDeploymentRecords implements IService {

  /**
   * Valid payload
   *
   * @see DeleteDeploymentRecordPayload
   */
  private final DeleteDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public DeleteDeploymentRecords(final DeleteDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /**
   * TODO: Change response
   *
   * @return blank response
   */
  @Override
  public ResponseEntity<String> response() {
    this.delete();
    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Write all records to the database */
  private void delete() {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    for (DeploymentRecord entry : payload.records) {

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
              .value(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, entry.newNodeId)
              .value(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, entry.parentId)
              .value(
                  Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                  DeploymentProcessStatus.WAITING_REMOVAL.toString())
              .value(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, this.calculateWaitFor(entry.newNodeId))
              .value(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, entry.serverUUID);

      session.execute(insert);
    }
  }

  /**
   * Simple function to gather a list of node id's that a given node needs to wait for
   *
   * @param nodeId node id to filter by
   * @return list of integers. If the filtered list is empty it returns a singleton
   */
  private List<Integer> calculateWaitFor(final int nodeId) {

    List<Integer> values =
        this.payload.records.stream()
            .filter(v -> v.parentId == nodeId)
            .map(v -> v.newNodeId)
            .collect(Collectors.toList());

    return values.size() > 0 ? values : new LinkedList<>(Collections.singleton(-1));
  }
}
