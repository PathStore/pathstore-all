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
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.UpdateDeploymentRecordPayload;

/**
 * This service is used to take a failed deployment record and update it to deploying after the user
 * has fixed the cause of failure
 */
public class UpdateDeploymentRecord implements IService {

  /** Valid payload */
  private final UpdateDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public UpdateDeploymentRecord(final UpdateDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /**
   * TODO create formatter
   *
   * @return updated
   */
  @Override
  public ResponseEntity<String> response() {
    this.update();

    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Updates the given record to the status of deploying */
  private void update() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    update
        .where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, payload.record.newNodeId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, payload.record.parentId))
        .and(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID, payload.record.serverUUID))
        .with(
            QueryBuilder.set(
                Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                DeploymentProcessStatus.DEPLOYING.toString()));

    session.execute(update);
  }
}
