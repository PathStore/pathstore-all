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
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.NodeCredential;

/**
 * This AuthClientInterceptor is specifically used to deal with the case when the grpc client is a
 * pathstore {@link pathstore.common.Role#SERVER}. As {@link pathstore.client.PathStoreServerClient}
 * is the wrapper for the grpc connection it needs to be able to handle the case of having an
 * instance of the credentials available all the time.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathStoreServerInterceptor extends AuthClientInterceptor {
  /** instance of the class */
  private static PathStoreServerInterceptor instance;

  /**
   * @param daemonCredentials daemon credentials of the parent node
   * @return Server Interceptor instance for that node
   * @see pathstore.client.PathStoreServerClient
   */
  public static synchronized PathStoreServerInterceptor getInstance(
      final NodeCredential daemonCredentials) {
    if (instance == null) instance = new PathStoreServerInterceptor(daemonCredentials);
    return instance;
  }

  /** Credential from caller */
  private final NodeCredential credential;

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.USERNAME, this.credential.getUsername());
    header.put(Keys.PASSWORD, this.credential.getPassword());
  }
}
