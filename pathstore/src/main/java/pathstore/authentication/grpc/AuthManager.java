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
package pathstore.authentication.grpc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.CredentialCache;
import pathstore.authentication.credentials.ClientCredential;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.NodeCredential;
import pathstore.authentication.credentials.NopCredential;

import java.util.*;

/**
 * This class is used for the registration, storage and comparison of credentials related to a grpc
 * server.
 *
 * @implNote The reason why the builder takes {@link CredentialCache#getAllReference()} is because
 *     as the credentials change in the cache they will also change within this class. In the future
 *     we may want to add a more complex event handling system to allow for constant time look up of
 *     if a credential is valid or not. (Myles)
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthManager {

  /** This class is used to build the auth manager with proper credentials per service. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Builder {
    /** map from service name to collection of valid server credentials */
    private final Map<String, Collection<NodeCredential>> serverCredentials = new HashMap<>();

    /** map from service name to collection of valid client credentials */
    private final Map<String, Collection<ClientCredential>> clientCredentials = new HashMap<>();

    /**
     * Set of unauthenticated services. As in regardless of credentials provided you will be able to
     * call any endpoint in that service
     */
    private final Set<String> unauthenticated = new HashSet<>();

    /** Additional credentials */
    private final Map<String, Collection<Credential<?>>> additionalCredentials = new HashMap<>();

    /**
     * This function is used to authenticate a service with a collection of server credentials
     *
     * @param serviceName service name to set permissions on
     * @param serverCredentials server credentials to provide
     * @return this
     * @see CredentialCache#getNodes()
     */
    public Builder serverAuthenticatedEndpoint(
        @NonNull final String serviceName,
        @NonNull final Collection<NodeCredential> serverCredentials) {
      this.serverCredentials.put(serviceName, serverCredentials);
      return this;
    }

    /**
     * This function is used to authenticate a service with a collection of client credentials
     *
     * @param serviceName service name to set permissions on
     * @param clientCredentials client credentials to provide
     * @return this
     * @see CredentialCache#getClients()
     */
    public Builder clientAuthenticatedEndpoint(
        @NonNull final String serviceName,
        @NonNull final Collection<ClientCredential> clientCredentials) {
      this.clientCredentials.put(serviceName, clientCredentials);
      return this;
    }

    /**
     * @param serviceName service name to set permissions on
     * @param serverCredentials server credentials to provide
     * @param clientCredentials client credentials to provide
     * @return this
     * @see #serverAuthenticatedEndpoint(String, Collection)
     * @see #clientAuthenticatedEndpoint(String, Collection)
     */
    public Builder serverAndClientAuthenticatedEndpoint(
        final String serviceName,
        final Collection<NodeCredential> serverCredentials,
        final Collection<ClientCredential> clientCredentials) {
      this.serverAuthenticatedEndpoint(serviceName, serverCredentials);
      this.clientAuthenticatedEndpoint(serviceName, clientCredentials);
      return this;
    }

    /**
     * @param serviceName endpoint to not have authentication
     * @return this
     */
    public Builder unauthenticatedEndpoint(final String serviceName) {
      this.unauthenticated.add(serviceName);
      return this;
    }

    /**
     * This function is used to add a specific username and password combo to the authentication
     *
     * @param serviceName service name to apply to
     * @param credential credential to add
     * @return this
     */
    public Builder addAdditionalCredentials(
        final String serviceName, final Credential<?> credential) {
      this.additionalCredentials.putIfAbsent(serviceName, new ArrayList<>());
      this.additionalCredentials.get(serviceName).add(credential);
      return this;
    }

    /** @return built instance of the auth manager */
    public AuthManager build() {
      return new AuthManager(
          this.serverCredentials,
          this.clientCredentials,
          this.unauthenticated,
          this.additionalCredentials);
    }
  }

  /** @return new instance of the builder class */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** All credentials between servers */
  private final Map<String, Collection<NodeCredential>> serverCredentials;

  /** All credentials between client */
  private final Map<String, Collection<ClientCredential>> clientCredentials;

  /** All unauthenticated endpoints */
  private final Set<String> unauthenticated;

  /** Additional specific credentials add to a service */
  private final Map<String, Collection<Credential<?>>> additionalCredentials;

  /**
   * @param endpoint endpoint called
   * @param username username given
   * @param password and password given
   * @return true if authenticated false if not
   */
  public boolean isAuthenticated(
      final String endpoint, final String username, final String password) {
    if (endpoint == null || username == null || password == null) return false;

    // get the service name of the endpoint as, authentication is at the service layer not the
    // endpoint layer
    String service = endpoint.substring(0, endpoint.indexOf('/'));

    // check to ensure the service is registered with the auth manager
    if (!this.serverCredentials.containsKey(service)
        && !this.clientCredentials.containsKey(service)
        && !this.unauthenticated.contains(service)
        && !this.additionalCredentials.containsKey(service)) return false;

    // check if the service is unauthenticated
    if (this.unauthenticated.contains(service)) return true;

    NopCredential providedCredential = new NopCredential(username, password);

    // check the additional credentials collection
    if (this.doesCollectionContain(service, this.additionalCredentials, providedCredential))
      return true;

    // check client credentials
    if (this.doesCollectionContain(service, this.clientCredentials, providedCredential))
      return true;

    // check server credentials
    return this.doesCollectionContain(service, this.serverCredentials, providedCredential);
  }

  /**
   * This function is used to check if a credential is available in one of three possible ways to
   * register it for a service. We use the {@link Credential#isSame(Credential)} function to
   * determine which credentials are equal regardless of class type. If we could guarantee class
   * type we would use the {@link Credential#equals(Object)} operand
   *
   * @param service service called
   * @param map map of service name to a collection of credentials as a point of valid credentials
   * @param credentialToCompare credential to compare to
   * @param <CredentialT> credential object type
   * @return true if the credential to compare is the same as one of the credentials in the map.
   *     Otherwise false
   */
  private <CredentialT extends Credential<?>> boolean doesCollectionContain(
      final String service,
      final Map<String, Collection<CredentialT>> map,
      final NopCredential credentialToCompare) {
    if (map.containsKey(service))
      for (CredentialT credential : map.get(service))
        if (credential.isSame(credentialToCompare)) return true;
    return false;
  }
}
