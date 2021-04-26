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
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.NodeCredential;

/**
 * This command is used to write child daemon credential information to the local node. This is used
 * to connect to the child node on un-deployment to force push all dirty data.
 *
 * @implNote This class uses {@link CredentialCache} as when this gets written it also gets stored
 *     in memory for more convenient access when used in the future
 */
@RequiredArgsConstructor
public class WriteChildCredentialsToCassandra implements ICommand {

  /** Child credential */
  private final NodeCredential childCredential;

  /** Calls {@link CredentialCache#add(Credential)} */
  @Override
  public void execute() {
    CredentialCache.getNodes().add(this.childCredential);
  }

  /** @return informs user what is occurring */
  @Override
  public String toString() {
    return "Writing child credentials to local cassandra";
  }
}
