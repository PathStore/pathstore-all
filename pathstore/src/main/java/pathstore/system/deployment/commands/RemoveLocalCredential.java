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
import pathstore.authentication.CredentialCache;

/**
 * This command is used to remove credentials from the local node's pathstore_applications.local_auth table during
 * un-deployment of a child node
 */
@RequiredArgsConstructor
public class RemoveLocalCredential implements ICommand {
  /** Node id of child that is being undeployed */
  private final int nodeId;

  /** Remove the data from the cache and the table */
  @Override
  public void execute() {
    CredentialCache.getNodes().remove(this.nodeId);
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format("Removing local credential with nodeid %d", this.nodeId);
  }
}
