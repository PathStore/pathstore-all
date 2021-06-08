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
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Keys for headers generated from {@link AuthClientInterceptor} and parsed by {@link
 * AuthServerInterceptor}
 *
 * <p>TODO: Add to constants file
 */
public class Keys {
  /** username header key */
  public static final Metadata.Key<String> USERNAME =
      Metadata.Key.of("username", ASCII_STRING_MARSHALLER);

  /** password header key */
  public static final Metadata.Key<String> PASSWORD =
      Metadata.Key.of("password", ASCII_STRING_MARSHALLER);
}
