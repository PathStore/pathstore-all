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
package pathstore.system.logging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;

/** This class denotes some log message in the system */
@RequiredArgsConstructor
public class PathStoreLoggerMessage {

  /** What position in the array are you */
  private final int count;

  /** What level to print at */
  private final LoggerLevel loggerLevel;

  /** What information to show */
  private final String message;

  /** Name of logger */
  private final String loggerName;

  /** Formatted message */
  @Getter(lazy = true)
  private final String formattedMessage = formatMessage(this.loggerLevel, this.loggerName, this.message);

  /**
   * Formats a message to [type][type][loggername] message
   *
   * @return formatted message
   */
  private static String formatMessage(final LoggerLevel loggerLevel, final String loggerName, final String message) {
    return String.format(
        "[%-6s][%-40s][%s] %s",
        loggerLevel.toString(),
        loggerName,
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()),
        message);
  }

  /** @return count */
  public int getCount() {
    return count;
  }

  /** @return logger level of message */
  public LoggerLevel getLoggerLevel() {
    return this.loggerLevel;
  }
}
