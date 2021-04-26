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

import com.datastax.driver.core.Session;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.DeploymentCredential;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.function.Consumer;

/** This command is used to load a keyspace onto the child node */
@RequiredArgsConstructor
public class LoadKeyspace implements ICommand {
  /** Cassandra credentials to connect with */
  private final DeploymentCredential cassandraCredentials;

  /**
   * Function that will take a session object as a param and load a keyspace to it
   *
   * @see pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils#loadApplicationSchema(Session)
   */
  private final Consumer<Session> loadKeyspaceFunction;

  /**
   * Name of the keyspace, this is solely used to inform the user of what keyspace is being loaded
   */
  private final String keyspaceName;

  /** Connect to child cassandra, call the load function and close the cluster */
  @Override
  public void execute() {
    PathStorePrivilegedCluster childCluster =
        PathStorePrivilegedCluster.getChildInstance(this.cassandraCredentials);

    this.loadKeyspaceFunction.accept(childCluster.rawConnect());

    childCluster.close();
  }

  /** @return inform the user which keyspace is being loaded */
  @Override
  public String toString() {
    return "Loading keyspace " + this.keyspaceName;
  }
}
