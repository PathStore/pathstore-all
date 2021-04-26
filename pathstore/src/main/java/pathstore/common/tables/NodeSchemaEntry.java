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
package pathstore.common.tables;

import com.datastax.driver.core.Row;

import java.util.List;

import static pathstore.common.Constants.NODE_SCHEMAS_COLUMNS.*;

/** This class is used to define an entry within the node schemas table */
public final class NodeSchemaEntry {
  public static NodeSchemaEntry fromRow(final Row row) {
    return new NodeSchemaEntry(
        row.getInt(NODE_ID),
        row.getString(KEYSPACE_NAME),
        NodeSchemaProcessStatus.valueOf(row.getString(PROCESS_STATUS)),
        row.getList(WAIT_FOR, Integer.class));
  }

  /** Node id of the node to install or remove on */
  public final int nodeId;

  /** What keyspace to perform the operation with */
  public final String keyspaceName;

  /** What is the current process status */
  public final NodeSchemaProcessStatus nodeSchemaProcessStatus;

  /** list of nodes that need to complete their operation before executing this operation */
  public final List<Integer> waitFor;

  /**
   * @param nodeId {@link #nodeId}
   * @param keyspaceName {@link #keyspaceName}
   * @param nodeSchemaProcessStatus {@link #nodeSchemaProcessStatus}
   * @param waitFor {@link #waitFor}
   */
  private NodeSchemaEntry(
      final int nodeId,
      final String keyspaceName,
      final NodeSchemaProcessStatus nodeSchemaProcessStatus,
      final List<Integer> waitFor) {
    this.nodeId = nodeId;
    this.keyspaceName = keyspaceName;
    this.nodeSchemaProcessStatus = nodeSchemaProcessStatus;
    this.waitFor = waitFor;
  }
}
