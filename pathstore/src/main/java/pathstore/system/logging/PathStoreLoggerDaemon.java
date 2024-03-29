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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.deployment.utilities.DeploymentConstants;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Logger daemon used to write logs to log table */
public class PathStoreLoggerDaemon extends Thread {

  /** String to denote how to format the date */
  private static String DATE_FORMAT = "yyyy-MM-dd";

  /** Physical log file directory */
  private static String LOGS_DIRECTORY =
      String.format("/etc/pathstore/%s", DeploymentConstants.LOGS_DIRECTORY_NAME);

  /** Logger to write any errors that occur during the writing of */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreLoggerDaemon.class);

  private final Session session;

  /** Stores the current date for comparison */
  private String currentDate;

  public PathStoreLoggerDaemon() {
    this.session = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

    this.currentDate = this.getAndSetDate();
  }

  /**
   * Every 5 seconds write the lowest ordinal log level to the logs table.
   *
   * @apiNote Parsing of this log based on log level will be done on the frontend to reduce api
   *     traffic and to reduce number of records written to the logs table
   */
  @Override
  public void run() {

    while (true) {
      if (PathStoreLoggerFactory.hasNew()) {
        List<PathStoreLoggerMessage> newMessages = PathStoreLoggerFactory.getMergedLog();

        newMessages.forEach(
            i -> {
              Insert insert =
                  QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);
              insert
                  .value(Constants.LOGS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID)
                  .value(Constants.LOGS_COLUMNS.DATE, this.getAndSetDate())
                  .value(Constants.LOGS_COLUMNS.LOG_LEVEL, i.getLoggerLevel().name())
                  .value(Constants.LOGS_COLUMNS.COUNT, i.getCount())
                  .value(Constants.LOGS_COLUMNS.LOG, i.getFormattedMessage());

              this.session.execute(insert);
            });

        this.appendToFile(newMessages);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.error("Sleep got interrupted for the logger daemon");
        e.printStackTrace();
      }
    }
  }

  /**
   * @return get current date and update internally stored date iff they're different (day change)
   */
  private String getAndSetDate() {
    String date = new SimpleDateFormat(DATE_FORMAT).format(new Date());

    if (!date.equals(this.currentDate)) this.updateDate(date);

    return date;
  }

  /**
   * This function updates {@link #currentDate} to the new date (on date change or startup) and
   * writes a record to {@link Constants#AVAILABLE_LOG_DATES} to inform the website that there is a
   * new date available for querying
   *
   * @param date new current date
   */
  private void updateDate(final String date) {
    this.currentDate = date;

    Insert insertDateChange =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);

    insertDateChange
        .value(
            Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID)
        .value(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE, date);

    this.session.execute(insertDateChange);
  }

  /** @return formats a log file name based on the current date {@link #currentDate} */
  private String formatFileName() {
    return String.format("%s/log-%s.txt", LOGS_DIRECTORY, this.currentDate);
  }

  /**
   * This function will append the new logs to the current date's log file
   *
   * @param newMessages list of new messages to append
   * @see PathStoreLoggerFactory#getMergedLog()
   */
  private void appendToFile(final List<PathStoreLoggerMessage> newMessages) {

    String fileName = this.formatFileName();

    try (FileWriter fw = new FileWriter(fileName, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)) {

      newMessages.stream().map(PathStoreLoggerMessage::getFormattedMessage).forEach(out::println);

    } catch (IOException exception) {
      logger.error(String.format("Could not write to log file: %s", fileName));
    }
  }
}
