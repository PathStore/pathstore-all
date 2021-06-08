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
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.util.SchemaInfo;

import java.util.Collection;

/** This command is used to force push all dirty data from a client on shutdown */
public class ForcePush implements ICommand {

  /** Logger for this class */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(ForcePush.class);

  /** Child cluster */
  private final PathStorePrivilegedCluster cluster;

  /** Node id of child */
  private final int nodeId;

  /**
   * @param nodeId node of child
   * @param ip ip of child
   * @param cassandraPort port of child
   */
  public ForcePush(final int nodeId, final String ip, final int cassandraPort) {
    this.cluster = PathStorePrivilegedCluster.getChildInstance(nodeId, ip, cassandraPort);
    this.nodeId = nodeId;
  }

  /**
   * Connect to the child, and create a connection to the local database. Then call {@link
   * PathStorePushServer#push(Collection, Session, Session, SchemaInfo, int)} to force push all data
   */
  @Override
  public void execute() {
    Session child = this.cluster.rawConnect();

    SchemaInfo childSchemaInfo = new SchemaInfo(child);

    PathStorePushServer.push(
        PathStorePushServer.buildCollectionOfTablesFromSchemaInfo(childSchemaInfo),
        child,
        PathStorePrivilegedCluster.getDaemonInstance().rawConnect(),
        childSchemaInfo,
        this.nodeId);

    this.cluster.close();

    this.logger.info("Finished pushing all dirty data");
  }

  /** @return inform the user what command is happening */
  @Override
  public String toString() {
    return String.format("Starting the push of all dirty data from child node %d", this.nodeId);
  }
}
