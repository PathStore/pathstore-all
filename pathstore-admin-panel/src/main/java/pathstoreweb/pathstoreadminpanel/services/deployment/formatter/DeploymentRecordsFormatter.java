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

package pathstoreweb.pathstoreadminpanel.services.deployment.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.tables.DeploymentEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;

import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;

/** This class is used to format a set of records either given by add requests or get requests */
public class DeploymentRecordsFormatter implements IFormatter {

  /** List of entries */
  private final List<DeploymentEntry> entries;

  /** @param entries {@link #entries} */
  public DeploymentRecordsFormatter(final List<DeploymentEntry> entries) {
    this.entries = entries;
  }

  /** @return converts list to a json array of data */
  @Override
  public ResponseEntity<String> format() {

    JSONArray array = new JSONArray();

    for (DeploymentEntry entry : this.entries)
      array.put(
          new JSONObject()
              .put(NEW_NODE_ID, entry.newNodeId)
              .put(PARENT_NODE_ID, entry.parentNodeId)
              .put(PROCESS_STATUS, entry.deploymentProcessStatus.toString())
              .put(WAIT_FOR, entry.waitFor)
              .put(SERVER_UUID, entry.serverUUID));

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}
