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

package pathstoreweb.pathstoreadminpanel.services.deployment.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.deployment.DeploymentRecord;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.DELETE_DEPLOYMENT_RECORD_PAYLOAD.EMPTY;
import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.DELETE_DEPLOYMENT_RECORD_PAYLOAD.INVALID_RECORD;

/**
 * Delete deployment record payload. This payload is used to request the deletion of a given node
 * and all of its children
 */
public class DeleteDeploymentRecordPayload extends ValidatedPayload {
  /** Deployment record */
  public List<DeploymentRecord> records;

  /**
   * Validity Check
   *
   * <p>(1): Deployment record empty check
   *
   * <p>(2): All entries represent a valid DEPLOYED Node (i.e. the newNodeId, parentId, serverUUID
   * match the record in the table and the status is DEPLOYED)
   *
   * @return all null iff all validity checks pass
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.records == null || this.records.size() == 0) return new String[] {EMPTY};

    Map<Integer, DeploymentRecord> mapFromIdToRecord =
        this.records.stream().collect(Collectors.toMap(k -> k.newNodeId, Function.identity()));

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select deploymentQuery =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    // (2)
    for (Row row : session.execute(deploymentQuery)) {
      int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);

      if (mapFromIdToRecord.containsKey(newNodeId)) {
        int parentId = row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID);
        String serverUUID = row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID);
        DeploymentProcessStatus status =
            DeploymentProcessStatus.valueOf(
                row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));
        DeploymentRecord entry = mapFromIdToRecord.get(newNodeId);

        if (parentId != entry.parentId
            || !serverUUID.equals(entry.serverUUID)
            || status != DeploymentProcessStatus.DEPLOYED) return new String[] {INVALID_RECORD};
      }
    }

    return new String[0];
  }
}
