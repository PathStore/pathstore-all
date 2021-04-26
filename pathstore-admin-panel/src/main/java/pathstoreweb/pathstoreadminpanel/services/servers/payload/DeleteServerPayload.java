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

package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import java.util.UUID;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.DELETE_SERVER_PAYLOAD.*;

/** Simple delete server payload for the delete operation */
public final class DeleteServerPayload extends ValidatedPayload {

  /** Server UUID passed by user to remove */
  public final UUID serverUUID;

  /** @param server_uuid {@link #serverUUID} */
  public DeleteServerPayload(final String server_uuid) {
    this.serverUUID = UUID.fromString(server_uuid);
  }

  /**
   * Validity check
   *
   * <p>(1): Wrong submission format
   *
   * <p>(2): server UUID exists
   *
   * <p>(3): server uuid is not in the deployment table as anything but nothing or (TODO:
   * un-deployed)
   *
   * @return all null iff validity test has passed
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.bulkNullCheck(serverUUID)) return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {SERVER_UUID_DOESNT_EXIST, null};

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    // (2)
    Select serverSelect =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
    serverSelect.where(
        QueryBuilder.eq(Constants.SERVERS_COLUMNS.SERVER_UUID, this.serverUUID.toString()));

    for (Row row : session.execute(serverSelect)) errors[0] = null;

    // (3)
    Select deploymentSelect =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    for (Row row : session.execute(deploymentSelect))
      if (row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID)
          .equals(this.serverUUID.toString())) errors[1] = SERVER_UUID_IS_NOT_FREE;

    return errors;
  }
}
