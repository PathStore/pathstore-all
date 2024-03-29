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

package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.ServerAuthType;
import pathstore.common.tables.ServerIdentity;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.AddServerFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload;

import java.io.IOException;

/**
 * This service is used to insert a record in the database to denote the creation of a new server
 */
public class AddServer implements IService {

  /**
   * Valid payload
   *
   * @see AddServerPayload
   */
  private final AddServerPayload payload;

  /** @param payload {@link #payload} */
  public AddServer(final AddServerPayload payload) {
    this.payload = payload;
  }

  /**
   * Insert the server into the database and create a success response
   *
   * @return response of the uuid and ip {@link AddServerFormatter}
   */
  @Override
  public ResponseEntity<String> response() {
    try {
      this.writeServerRecord();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new AddServerFormatter(this.payload.server.serverUUID, this.payload.server.ip).format();
  }

  /** Write insert up to the table */
  private void writeServerRecord() throws IOException {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(Constants.SERVERS_COLUMNS.SERVER_UUID, this.payload.server.serverUUID.toString())
            .value(Constants.SERVERS_COLUMNS.IP, this.payload.server.ip)
            .value(Constants.SERVERS_COLUMNS.USERNAME, this.payload.server.username)
            .value(Constants.SERVERS_COLUMNS.SSH_PORT, this.payload.server.sshPort)
            .value(Constants.SERVERS_COLUMNS.GRPC_PORT, this.payload.server.grpcPort)
            .value(Constants.SERVERS_COLUMNS.NAME, this.payload.server.name);

    // set proper rows for password auth type
    if (this.payload.server.authType.equals(ServerAuthType.PASSWORD.toString()))
      insert
          .value(Constants.SERVERS_COLUMNS.AUTH_TYPE, ServerAuthType.PASSWORD.toString())
          .value(Constants.SERVERS_COLUMNS.PASSWORD, this.payload.server.password);
    // set proper rows for keys type
    else if (this.payload.server.authType.equals(ServerAuthType.IDENTITY.toString()))
      insert
          .value(Constants.SERVERS_COLUMNS.AUTH_TYPE, ServerAuthType.IDENTITY.toString())
          .value(
              Constants.SERVERS_COLUMNS.SERVER_IDENTITY,
              new ServerIdentity(
                      this.payload.getPrivateKey().getBytes(), this.payload.server.passphrase)
                  .serialize());
    else
      throw new RuntimeException(
          String.format("%s  is not a valid auth type", this.payload.server.authType));

    session.execute(insert);
  }
}
