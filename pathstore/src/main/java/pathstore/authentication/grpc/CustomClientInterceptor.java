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
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.Credential;

/**
 * This class is specifically used for {@link
 * pathstore.client.PathStoreServerClient#getCustom(String, int, Credential)}
 *
 * @param <CredentialT> credential token type
 */
@RequiredArgsConstructor
public class CustomClientInterceptor<CredentialT extends Credential<?>>
    extends AuthClientInterceptor {

  /** Credential provided */
  private final CredentialT credential;

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.USERNAME, this.credential.getUsername());
    header.put(Keys.PASSWORD, this.credential.getPassword());
  }
}
