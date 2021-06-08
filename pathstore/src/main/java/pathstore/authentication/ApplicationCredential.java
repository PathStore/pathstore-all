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
package pathstore.authentication;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This represents a row in the application_credentials table
 *
 * @see ClientAuthenticationUtil#getApplicationCredentialRow(String, String)
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ApplicationCredential {
  /** keyspace for the application */
  @Getter @NonNull private final String keyspaceName;

  /** Master application credential password */
  @Getter @NonNull private final String password;

  /** are the application clients going to be super users */
  @Getter private final boolean isSuperUser;
}
