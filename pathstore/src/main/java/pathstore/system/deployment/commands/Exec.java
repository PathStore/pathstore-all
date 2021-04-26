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
package pathstore.system.deployment.commands;

import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import pathstore.util.Pair;
import pathstore.system.deployment.utilities.SSHUtil;

import java.io.IOException;

/**
 * This class is used to denote a step in the installation process where you want to execute a
 * single command on the remote host and you except a certain exit code
 */
@RequiredArgsConstructor
public class Exec implements ICommand {

  /** Used to remotely execute {@link #command} */
  private final SSHUtil sshUtil;

  /** Command to execute */
  private final String command;

  /** Exit code you want */
  private final int wantedResponse;

  /**
   * Execute command on remote host
   *
   * @throws CommandError contains a message to denote what went wrong
   */
  @Override
  public void execute() throws CommandError {
    try {
      Pair<String, Integer> response = this.sshUtil.execCommand(this.command);

      if (this.wantedResponse != -1 && this.wantedResponse != response.t2)
        throw new CommandError(
            String.format(
                "We expected the exit status of the command %s to be %d but we received %d instead. The response of the command is %s",
                this.command, this.wantedResponse, response.t2, response.t1));

    } catch (JSchException ignored) {
      throw new CommandError(
          "We were unable to create an exec channel to execute the command please ensure that the system is online and is connectable over ssh");
    } catch (IOException ignore) {
      throw new CommandError(
          " We were unable to read the output stream of the command. This is most likely caused by a local issue. Please ensure the machine executing the deployment has no issues and try again");
    }
  }

  /** @return To show the user what command is currently being executed */
  @Override
  public String toString() {
    return String.format(
        "Executing command: %s looking for response %d", this.command, this.wantedResponse);
  }
}
