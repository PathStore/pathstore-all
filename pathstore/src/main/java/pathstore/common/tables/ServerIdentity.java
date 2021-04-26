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
package pathstore.common.tables;

import pathstore.util.BlobObject;

/**
 * This class is used to denote a server identity for a server object. This specifically represents
 * those server objects who need RSA authentication
 *
 * <p>This class gets serialized to {@link
 * pathstore.common.Constants.SERVERS_COLUMNS#SERVER_IDENTITY}
 */
public class ServerIdentity implements BlobObject {
  /** Private key in bytes */
  public final byte[] privateKey;

  /** Optional passphrase */
  public final String passphrase;

  /** @param privateKey private key to give, if passphrase is not present */
  public ServerIdentity(final byte[] privateKey) {
    this(privateKey, null);
  }

  /**
   * @param privateKey private key to give
   * @param passphrase passphrase (can also be null)
   */
  public ServerIdentity(final byte[] privateKey, final String passphrase) {
    this.privateKey = privateKey;
    this.passphrase = passphrase;
  }
}
