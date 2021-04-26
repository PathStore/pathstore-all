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

import lombok.RequiredArgsConstructor;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Change log levels to a generic setting
 *
 * <p>This class is used as an internal logger for pathstore and the pathstore api.
 *
 * <p>Level ranks:
 *
 * <p>FINEST, DEBUG, INFO, ERROR
 */
@RequiredArgsConstructor
public class PathStoreLogger {

  /** Denotes the counter */
  static final AtomicInteger counter = new AtomicInteger(0);

  /** Name of logger */
  private final String name;

  /** Used to denote what level of messages are displayed */
  private final LoggerLevel displayLevel;

  /** Map of messages used to allow for merging of multiple loggers concurrently */
  private final Map<Integer, PathStoreLoggerMessage> messages = new ConcurrentHashMap<>();

  /** Is there new data available to read from */
  private boolean hasNew = false;

  /**
   * Send an information message
   *
   * @param message message to show
   */
  public void info(final String message) {
    this.log(LoggerLevel.INFO, message);
  }

  /**
   * Send a debug message
   *
   * @param message message to show
   */
  public void debug(final String message) {
    this.log(LoggerLevel.DEBUG, message);
  }

  /**
   * Send a finest message
   *
   * @param message message to show
   */
  public void finest(final String message) {
    this.log(LoggerLevel.FINEST, message);
  }

  /**
   * Send an error message
   *
   * @param message message to show
   */
  public void error(final String message) {
    this.log(LoggerLevel.ERROR, message);
  }

  /**
   * Pass an error so the stack trace can be printed
   *
   * @param throwable throwable to print
   */
  public void error(final Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    this.log(LoggerLevel.ERROR, sw.toString());
    System.out.println(sw.toString());
  }

  /**
   * Stores log messages internally and prints message iff the message level <= the display level
   *
   * @param loggerLevel which log level to print to
   * @param message what message to print
   */
  public void log(final LoggerLevel loggerLevel, final String message) {
    if (!PathStoreProperties.getInstance().printLogs) return;

    int count = counter.getAndIncrement();

    PathStoreLoggerMessage loggerMessage =
        new PathStoreLoggerMessage(count, loggerLevel, message, this.name);

    if (PathStoreProperties.getInstance().role != Role.CLIENT) {
      this.hasNew = true;

      this.messages.put(count, loggerMessage);
    }

    if (loggerMessage.getLoggerLevel().ordinal() >= this.displayLevel.ordinal())
      System.out.println(loggerMessage.getFormattedMessage());
  }

  /**
   * Sets has new to false as the logger factory as pulled the messages recently
   *
   * @return internal messages written by this logger
   */
  protected Map<Integer, PathStoreLoggerMessage> getMessages() {
    this.hasNew = false;
    return this.messages;
  }

  /** @return iff there is new data for this logger */
  protected boolean hasNew() {
    return this.hasNew;
  }

  /** Clears the internal messages from memory after merger */
  protected void clear() {
    this.messages.clear();
  }
}
