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

package pathstoreweb.pathstoreadminpanel.services.deployment;

/** This represents a individual record supplied by the frontend */
public class DeploymentRecord {
  /** ParentId of the new node */
  public int parentId;

  /** The new node's id */
  public int newNodeId;

  /** Where to install it */
  public String serverUUID;

  /** @return string for debug purposes */
  @Override
  public String toString() {
    return "AddDeploymentRecordPayload{"
        + "parentId="
        + parentId
        + ", newNodeId="
        + newNodeId
        + ", serverUUID="
        + serverUUID
        + '}';
  }
}
