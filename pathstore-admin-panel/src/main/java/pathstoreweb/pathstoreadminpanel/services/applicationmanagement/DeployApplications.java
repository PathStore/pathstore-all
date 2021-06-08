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

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.NodeSchemaProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload.AddApplicationDeploymentRecordPayload;

import java.util.LinkedList;

/**
 * This services writes all validated records to the table which will cause pathstore to install
 * application on a subtree of the network
 */
public class DeployApplications implements IService {

  /** Validated payload */
  private final AddApplicationDeploymentRecordPayload payload;

  /** @param payload {@link #payload} */
  public DeployApplications(final AddApplicationDeploymentRecordPayload payload) {
    this.payload = payload;
  }

  /** @return TODO: Valid returns */
  @Override
  public ResponseEntity<String> response() {
    writeRecords();
    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Writes all records to the table with WAITING_INSTALL status */
  private void writeRecords() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    for (ApplicationRecord record : this.payload.records) {

      Insert insert =
          QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS)
              .value(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, record.nodeId)
              .value(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME, record.keyspaceName)
              .value(
                  Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS,
                  NodeSchemaProcessStatus.WAITING_INSTALL.toString())
              .value(Constants.NODE_SCHEMAS_COLUMNS.WAIT_FOR, new LinkedList<>(record.waitFor));

      session.execute(insert);
    }
  }
}
