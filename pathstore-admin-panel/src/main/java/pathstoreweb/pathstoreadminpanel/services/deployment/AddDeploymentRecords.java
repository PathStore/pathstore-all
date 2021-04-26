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
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;
import pathstoreweb.pathstoreadminpanel.services.deployment.payload.AddDeploymentRecordPayload;

import java.util.Collections;
import java.util.LinkedList;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;

/** This class is used when the user passes a set of records to add */
public class AddDeploymentRecords implements IService {

  /**
   * Valid payload given by user
   *
   * @see AddDeploymentRecordPayload
   */
  private final AddDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public AddDeploymentRecords(final AddDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /** @return {@link DeploymentRecordsFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    this.writeEntries();

    return new DeploymentRecordsFormatter(new LinkedList<>()).format();
  }

  /**
   * Writes all entries that the user has provided with the {@link
   * DeploymentProcessStatus#WAITING_DEPLOYMENT} status. The DeploymentFSM will handle the
   * transitions of states and the slave deployment server will install when their state hits {@link
   * DeploymentProcessStatus#DEPLOYING}
   *
   * @return list of entries written. This is so the user can see the output
   */
  private void writeEntries() {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    for (DeploymentRecord record : payload.records) {

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
              .value(NEW_NODE_ID, record.newNodeId)
              .value(PARENT_NODE_ID, record.parentId)
              .value(PROCESS_STATUS, DeploymentProcessStatus.WAITING_DEPLOYMENT.toString())
              .value(WAIT_FOR, new LinkedList<>(Collections.singleton(record.parentId)))
              .value(SERVER_UUID, record.serverUUID);

      session.execute(insert);
    }
  }
}
