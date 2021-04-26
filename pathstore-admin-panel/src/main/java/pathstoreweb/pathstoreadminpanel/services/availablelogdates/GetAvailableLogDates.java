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

package pathstoreweb.pathstoreadminpanel.services.availablelogdates;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.availablelogdates.formatter.GetAvailableLogDatesFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service is used to read the available log dates table to inform the website of which logs
 * are available to query
 */
public class GetAvailableLogDates implements IService {

  /** @return {@link GetAvailableLogDatesFormatter} */
  @Override
  public ResponseEntity<String> response() {
    return new GetAvailableLogDatesFormatter(this.getAvailableLogDates()).format();
  }

  /** @return parsed response from database. The select statement filters based on the payload */
  private Map<Integer, List<String>> getAvailableLogDates() {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select selectAll =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.AVAILABLE_LOG_DATES);

    Map<Integer, List<String>> nodeToDates = new HashMap<>();

    for (Row row : session.execute(selectAll)) {
      int nodeId = row.getInt(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID);
      nodeToDates.computeIfAbsent(nodeId, k -> new ArrayList<>());
      nodeToDates.get(nodeId).add(row.getString(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE));
    }

    return nodeToDates;
  }
}
