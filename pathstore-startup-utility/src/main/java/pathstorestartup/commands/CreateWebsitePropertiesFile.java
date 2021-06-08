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

package pathstorestartup.commands;

import pathstore.common.Constants;
import pathstore.common.Role;
import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static pathstore.common.Constants.PROPERTIES_CONSTANTS.*;

/** This command is used to generate a local copy of the website properties file */
public class CreateWebsitePropertiesFile implements ICommand {

  /** Ip of pathstore root node */
  private final String ip;

  /** cassandra port for pathstore root node */
  private final int cassandraPort;

  /** grpc to connect to pathstore root node */
  private final int grpcPort;

  /** Destination to store file locally */
  private final String destinationToWrite;

  /** admin password */
  private final String masterPassword;

  /**
   * @param ip {@link #ip}
   * @param cassandraPort {@link #cassandraPort}
   * @param grpcPort {@link #grpcPort}
   * @param destinationToWrite {@link #destinationToWrite}
   * @param masterPassword {@link #masterPassword}
   */
  public CreateWebsitePropertiesFile(
      final String ip,
      final int cassandraPort,
      final int grpcPort,
      final String destinationToWrite,
      final String masterPassword) {
    this.ip = ip;
    this.cassandraPort = cassandraPort;
    this.grpcPort = grpcPort;
    this.destinationToWrite = destinationToWrite;
    this.masterPassword = masterPassword;
  }

  /**
   * This function will generate the properties file based on the information provided by the user
   * for the website
   *
   * @throws CommandError iff the properties file can not be written
   */
  @Override
  public void execute() throws CommandError {
    Properties properties = new Properties();

    properties.setProperty(ROLE, Role.CLIENT.toString());
    properties.setProperty(CASSANDRA_IP, this.ip);
    properties.setProperty(CASSANDRA_PORT, String.valueOf(this.cassandraPort));
    properties.setProperty(GRPC_IP, this.ip);
    properties.setProperty(GRPC_PORT, String.valueOf(this.grpcPort));
    properties.setProperty(APPLICATION_NAME, Constants.PATHSTORE_APPLICATIONS);
    properties.setProperty(APPLICATION_MASTER_PASSWORD, this.masterPassword);

    try {
      OutputStream outputStream = new FileOutputStream(this.destinationToWrite);
      properties.store(outputStream, null);
    } catch (IOException e) {
      throw new CommandError(
          String.format(
              "Could not write the website configuration file to %s", this.destinationToWrite));
    }
  }

  /** @return used to display to user what is going on */
  @Override
  public String toString() {
    return "Creating website properties file";
  }
}
