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
package pathstore.authentication.credentials;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * This credential is used specifically for the client credentials. This will be used to denote a
 * credential that is solely used for pathstore client.
 */
@EqualsAndHashCode(callSuper = true)
public class ClientCredential extends Credential<String> {

  /** Denotes whether the client is a super user */
  @Getter private final boolean isSuperUser;

  /**
   * @param searchable application name
   * @param username username
   * @param password password
   */
  public ClientCredential(
      final @NonNull String searchable,
      final @NonNull String username,
      final @NonNull String password,
      final boolean isSuperUser) {
    super(searchable, username, password);
    this.isSuperUser = isSuperUser;
  }
}
