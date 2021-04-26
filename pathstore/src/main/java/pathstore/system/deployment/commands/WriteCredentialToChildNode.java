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
import pathstore.authentication.CredentialDataLayer;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This command is used to write the current node's daemon account to the child node's
 * pathstore_applications.local_auth table so they can access the parent node's cassandra during
 * push and pull operations
 */
@RequiredArgsConstructor
public class WriteCredentialToChildNode<SearchableT, CredentialT extends Credential<SearchableT>>
    implements ICommand {
  /** Data layer to write to */
  private final CredentialDataLayer<SearchableT, CredentialT> dataLayer;

  /** Credential to write */
  private final CredentialT credential;

  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /** Connect to the child node, write the credentials to that node, and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    this.dataLayer.write(childCluster.rawConnect(), this.credential);

    childCluster.close();
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format(
        "Writing account with username %s to child node", this.credential.getUsername());
  }
}
