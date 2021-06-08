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

package pathstoreweb.pathstoreadminpanel.services.logs;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.logs.formatter.LogRecordsFormatter;
import pathstoreweb.pathstoreadminpanel.services.logs.payload.GetLogRecordsPayload;

import java.util.LinkedList;
import java.util.List;

/**
 * This service is used to get all logs from the root node and parse them into a list of log
 * messages
 */
public class GetLogRecords implements IService {

  /** Payload passed by the user */
  private final GetLogRecordsPayload payload;

  /** @param payload {@link #payload} */
  public GetLogRecords(final GetLogRecordsPayload payload) {
    this.payload = payload;
  }

  /** @return {@link LogRecordsFormatter} */
  @Override
  public ResponseEntity<String> response() {
    return new LogRecordsFormatter(this.getLogs()).format();
  }

  /**
   * Query all records and parse them into log objects
   *
   * @return list of messages filtered by {@link #payload} properties
   */
  private List<String> getLogs() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select selectFilteredLogs =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.LOGS);

    selectFilteredLogs
        .where(QueryBuilder.eq(Constants.LOGS_COLUMNS.NODE_ID, payload.nodeId))
        .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.DATE, payload.date))
        .and(QueryBuilder.eq(Constants.LOGS_COLUMNS.LOG_LEVEL, payload.logLevel));

    LinkedList<String> messages = new LinkedList<>();

    for (Row row : session.execute(selectFilteredLogs))
      messages.addLast(row.getString(Constants.LOGS_COLUMNS.LOG));

    return messages;
  }
}
