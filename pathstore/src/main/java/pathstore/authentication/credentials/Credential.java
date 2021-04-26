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

import lombok.*;

/**
 * This is the base credential class. All other credential references should extend this class.
 *
 * @param <SearchableT> type of class you want to be able to index by in {@link
 *     pathstore.util.ClusterCache}
 */
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "searchable")
@ToString
public class Credential<SearchableT> {
  /**
   * How to search for clusters in the cache
   *
   * @see NopCredential If you wish to just compare a username and password to a credential in the
   *     cache without extracting the information from the credential and performing a manual
   *     comparison
   */
  @Getter @NonNull private final SearchableT searchable;

  /** Username of the credential */
  @Getter @NonNull private final String username;

  /** Password of the credential */
  @Getter @NonNull private final String password;

  /**
   * Compare any credential type to the parent class to see if the authentication information is the
   * same.
   *
   * @param credentialT object to compare
   * @param <CredentialT> some Credential type
   * @return true if username and password are equal, else false
   */
  public <CredentialT extends Credential<?>> boolean isSame(final CredentialT credentialT) {
    return this.getUsername().equals(credentialT.getUsername())
        && this.getPassword().equals(credentialT.getPassword());
  }
}
