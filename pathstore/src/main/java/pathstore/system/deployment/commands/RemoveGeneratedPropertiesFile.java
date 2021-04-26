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

import lombok.RequiredArgsConstructor;
import pathstore.system.deployment.utilities.StartupUTIL;

import java.io.File;

/**
 * This command is used to delete the generated properties file for the new node after transferring
 * the file.
 */
@RequiredArgsConstructor
public class RemoveGeneratedPropertiesFile implements ICommand {

  /** Where the generate properties file is stored */
  private final String pathToFile;

  /**
   * Load file from path into memory and delete it.
   *
   * @throws CommandError if file could not be deleted or if the relative path is invalid
   */
  @Override
  public void execute() throws CommandError {

    File file = new File(StartupUTIL.getAbsolutePathFromRelativePath(this.pathToFile));

    if (file.delete()) System.out.println("File delete");
    else throw new CommandError(String.format("Could not delete file %s", this.pathToFile));
  }

  /** @return inform user that the file is being deleted */
  @Override
  public String toString() {
    return String.format(
        "Attempting to delete generated properties file in location %s", this.pathToFile);
  }
}
