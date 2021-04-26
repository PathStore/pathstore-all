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
package pathstore.system.deployment.commands;

import lombok.RequiredArgsConstructor;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

/** This command is used to create a role on the child node during deployment */
@RequiredArgsConstructor
public class CreateRole implements ICommand {

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Role to create credentials */
  private final Credential<?> credential;

  /** Is the role a super user */
  private final boolean isSuperUser;

  /** Connect to the child node, create the role and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    // load new child role and delete old role.
    CassandraAuthenticationUtil.createRole(
        childCluster.rawConnect(),
        this.credential.getUsername(),
        this.isSuperUser,
        true,
        this.credential.getPassword());

    childCluster.close();
  }

  /** @return inform print out message */
  @Override
  public String toString() {
    return String.format(
        "Creating user account for child node with username %s and super user %b",
        this.credential.getUsername(), this.isSuperUser);
  }
}
