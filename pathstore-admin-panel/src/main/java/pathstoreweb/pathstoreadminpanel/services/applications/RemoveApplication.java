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

package pathstoreweb.pathstoreadminpanel.services.applications;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.RemoveApplicationPayload;

/**
 * This service will remove an application from the application list iff it is not deployed on any
 * nodes
 */
public class RemoveApplication implements IService {

  /**
   * Validated payload
   *
   * @see RemoveApplicationPayload
   */
  private final RemoveApplicationPayload payload;

  /** @param payload {@link #payload} */
  public RemoveApplication(final RemoveApplicationPayload payload) {
    this.payload = payload;
  }

  /** Remove the application from the network and return 200 OK */
  @Override
  public ResponseEntity<String> response() {
    removeApplication();
    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Delete the given application record from the table */
  private void removeApplication() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Delete removeApp = QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    removeApp.where(
        QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.payload.applicationName));

    session.execute(removeApp);

    Delete removeMasterPassword =
        QueryBuilder.delete()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_CREDENTIALS);
    removeMasterPassword.where(
        QueryBuilder.eq(
            Constants.APPLICATION_CREDENTIALS_COLUMNS.KEYSPACE_NAME, this.payload.applicationName));

    session.execute(removeMasterPassword);
  }
}
