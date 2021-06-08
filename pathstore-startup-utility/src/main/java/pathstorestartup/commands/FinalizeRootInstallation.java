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

package pathstorestartup.commands;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstore.common.tables.ServerAuthType;
import pathstore.common.tables.ServerIdentity;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.commands.ICommand;
import pathstorestartup.constants.BootstrapDeploymentConstants;

import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

import static pathstore.common.Constants.APPLICATION_CREDENTIALS_COLUMNS.PASSWORD;
import static pathstore.common.Constants.APPLICATION_CREDENTIALS_COLUMNS.*;
import static pathstore.common.Constants.DEPLOYMENT_COLUMNS.*;
import static pathstore.common.Constants.SERVERS_COLUMNS.SERVER_UUID;
import static pathstore.common.Constants.SERVERS_COLUMNS.*;

/**
 * This command will write the server record for the root node and the deployment record for the
 * root node
 */
public class FinalizeRootInstallation implements ICommand {

  /** Cassandra credential to connect to server with */
  private final DeploymentCredential cassandraCredential;

  /** Username to server */
  private final String username;

  /** Method of authentication used to install the root node */
  private final String authType;

  /** Null if password auth was used, else */
  private final ServerIdentity serverIdentity;

  /** Password to server */
  private final String password;

  /** Master password for the pathstore_applications row in the application credentials */
  private final String masterPassword;

  /** Ssh port to server */
  private final int sshPort;

  /** Grpc port to server */
  private final int grpcPort;

  /**
   * @param cassandraCredentials {@link #cassandraCredential}
   * @param username {@link #username}
   * @param password {@link #password}
   * @param masterPassword {@link #masterPassword}
   * @param sshPort {@link #sshPort}
   * @param grpcPort {@link #grpcPort}
   */
  public FinalizeRootInstallation(
      final DeploymentCredential cassandraCredentials,
      final String username,
      final String authType,
      final ServerIdentity serverIdentity,
      final String password,
      final String masterPassword,
      final int sshPort,
      final int grpcPort) {
    this.cassandraCredential = cassandraCredentials;
    this.username = username;
    this.authType = authType;
    this.serverIdentity = serverIdentity;
    this.password = password;
    this.masterPassword = masterPassword;
    this.sshPort = sshPort;
    this.grpcPort = grpcPort;
  }

  /**
   * This command will write the root node server record to the table and write the root node
   * deployment record
   */
  @Override
  public void execute() {

    PathStorePrivilegedCluster cluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredential);
    Session session = cluster.psConnect();

    UUID serverUUID = UUID.randomUUID();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(SERVER_UUID, serverUUID.toString())
            .value(IP, this.cassandraCredential.getIp())
            .value(USERNAME, this.username)
            .value(SSH_PORT, this.sshPort)
            .value(GRPC_PORT, this.grpcPort)
            .value(NAME, "Root Node");

    if (this.authType.equals(BootstrapDeploymentConstants.AUTH_TYPES.PASSWORD))
      insert.value(AUTH_TYPE, ServerAuthType.PASSWORD.toString()).value(PASSWORD, this.password);
    else
      insert
          .value(AUTH_TYPE, ServerAuthType.IDENTITY.toString())
          .value(SERVER_IDENTITY, this.serverIdentity.serialize());

    session.execute(insert);

    insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
            .value(NEW_NODE_ID, 1)
            .value(PARENT_NODE_ID, -1)
            .value(PROCESS_STATUS, DeploymentProcessStatus.DEPLOYED.toString())
            .value(WAIT_FOR, new LinkedList<>(Collections.singleton(-1)))
            .value(SERVER_UUID, serverUUID.toString());

    session.execute(insert);

    insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_CREDENTIALS)
            .value(KEYSPACE_NAME, Constants.PATHSTORE_APPLICATIONS)
            .value(PASSWORD, this.masterPassword)
            .value(IS_SUPER_USER, true);

    session.execute(insert);

    cluster.close();
  }

  /** @return info message */
  @Override
  public String toString() {
    return "Writing server, deployment record, and the application credentials for the admin panel to roots table";
  }
}
