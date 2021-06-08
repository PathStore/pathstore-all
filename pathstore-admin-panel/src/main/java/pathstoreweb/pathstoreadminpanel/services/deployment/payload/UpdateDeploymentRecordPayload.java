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

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.UPDATE_DEPLOYMENT_RECORD_PAYLOAD.INVALID_FAILED_ENTRY;

/** This payload is used to pass a record that has valid and update it to deploying */
public final class UpdateDeploymentRecordPayload extends ValidatedPayload {

  /**
   * deployment record based by user. This record must be a failed record in order to pass
   * validation
   */
  public DeploymentRecord record;

  /**
   * Validity check
   *
   * <p>(1): Enter a valid failed record
   *
   * @return true iff all validity checks pass
   */
  @Override
  protected String[] calculateErrors() {

    String[] errors = {INVALID_FAILED_ENTRY};

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    // (1)
    Select select =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    select.where(
        QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID, this.record.parentId));

    for (Row row : session.execute(select)) {
      int newNodeId = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);
      String serverUUID = row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID);
      DeploymentProcessStatus status =
          DeploymentProcessStatus.valueOf(
              row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS));

      if (newNodeId == this.record.newNodeId
          && serverUUID.equals(this.record.serverUUID)
          && status == DeploymentProcessStatus.FAILED) {
        errors[0] = null;
        break;
      }
    }
    return errors;
  }
}
