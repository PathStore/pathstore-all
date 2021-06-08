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

package pathstorestartup.constants;

import pathstore.system.deployment.commands.CommandError;
import pathstore.system.deployment.commands.ICommand;

import java.io.IOException;
import java.util.List;

public class LocalCommand implements ICommand {

  /** Process builder based on user passed command */
  public final ProcessBuilder processBuilder;

  /** Message to display before executing the command */
  public final String entry;

  /** Message to display after the command has finished executing */
  public final String exit;

  /** Message to display on error of the process */
  public final String error;

  /** Desired exit status for the process (-1 if irrelevant) */
  public final int desiredExitStatus;

  /**
   * @param commands {@link #processBuilder}
   * @param entry {@link #entry}
   * @param exit {@link #exit}
   * @param error {@link #error}
   */
  public LocalCommand(
      final List<String> commands,
      final String entry,
      final String exit,
      final String error,
      final int desiredExitStatus) {
    this.processBuilder = new ProcessBuilder(commands);
    this.entry = entry;
    this.exit = exit;
    this.error = error;
    this.desiredExitStatus = desiredExitStatus;
  }

  @Override
  public void execute() throws CommandError {
    System.out.println(this.entry);
    try {
      Process p =
          this.processBuilder
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start();
      p.waitFor();

      if (this.desiredExitStatus != -1 && this.desiredExitStatus != p.exitValue()) {
        System.err.println(this.error);
        System.exit(-1);
      }

      System.out.println(this.exit);
    } catch (InterruptedException | IOException e) {
      System.err.println(this.error);
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
