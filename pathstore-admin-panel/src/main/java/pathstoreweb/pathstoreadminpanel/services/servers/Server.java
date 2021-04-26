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

package pathstoreweb.pathstoreadminpanel.services.servers;

import java.util.UUID;

/** This class represents a server object with relevant connection and identification information */
public class Server {

  /** Random uuid to denote this server in other tables */
  public final UUID serverUUID;

  /** Public ip of server (port 22, 9052, 1099 are open) */
  public final String ip;

  /** Username to ssh into server */
  public final String username;

  /** auth type of server */
  public final String authType;

  /** Passphrase of the server */
  public final String passphrase;

  /** Password for server */
  public final String password;

  /** SSH port number for server */
  public final int sshPort;

  /** GRPC port for pathstore grpc server */
  public final int grpcPort;

  /** Human readable name for this server */
  public final String name;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   * @param username {@link #username}
   * @param authType {@link #authType}
   * @param passphrase {@link #passphrase}
   * @param password {@link #password}
   * @param sshPort {@link #sshPort}
   * @param grpcPort {@link #grpcPort}
   * @param name {@link #name}
   */
  public Server(
      final UUID serverUUID,
      final String ip,
      final String username,
      final String authType,
      final String passphrase,
      final String password,
      final int sshPort,
      final int grpcPort,
      final String name) {
    this.serverUUID = serverUUID;
    this.ip = ip;
    this.username = username;
    this.authType = authType;
    this.passphrase = passphrase;
    this.password = password;
    this.sshPort = sshPort;
    this.grpcPort = grpcPort;
    this.name = name;
  }
}
