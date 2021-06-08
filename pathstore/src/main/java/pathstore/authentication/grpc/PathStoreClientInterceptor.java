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

import io.grpc.Metadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.NopCredential;

/**
 * This AuthClientInterceptor is specifically used to deal with the case when the grpc client is a
 * pathstore {@link pathstore.common.Role#CLIENT}. As {@link pathstore.client.PathStoreServerClient}
 * is the wrapper for the grpc connection it needs to be able to handle the case of having to make a
 * grpc call to the local node before they know the credentials. This instance covers that case.
 * However it assumes that the credentials will have a primary key of a type string.
 *
 * @see PathStoreServerInterceptor for when the grpc client is a pathstore {@link
 *     pathstore.common.Role#SERVER}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathStoreClientInterceptor extends AuthClientInterceptor {

  /** instance of client interceptor */
  @Getter(lazy = true)
  private static final PathStoreClientInterceptor instance = new PathStoreClientInterceptor();

  /**
   * Credentials to set in the header. Default values are noop.
   *
   * @see pathstore.client.PathStoreClientAuthenticatedCluster for invokation of setter
   */
  @Getter @Setter private Credential<?> credential = new NopCredential("nop", "nop");

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.USERNAME, this.credential.getUsername());
    header.put(Keys.PASSWORD, this.credential.getPassword());
  }
}
