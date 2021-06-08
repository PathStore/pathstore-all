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

package pathstoreweb.pathstoreadminpanel.services.availablelogdates.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;
import java.util.Map;

/**
 * This formatter is used to take a parsed integer to list of dates map and display the result to
 * the user.
 *
 * @see pathstoreweb.pathstoreadminpanel.services.availablelogdates.GetAvailableLogDates
 */
public class GetAvailableLogDatesFormatter implements IFormatter {

  /** Map of node id to list of available log dates */
  private final Map<Integer, List<String>> nodeToDates;

  /** @param nodeToDates {@link #nodeToDates} */
  public GetAvailableLogDatesFormatter(final Map<Integer, List<String>> nodeToDates) {
    this.nodeToDates = nodeToDates;
  }

  /** @return return json array of json objects with node id and list of dates */
  @Override
  public ResponseEntity<String> format() {

    JSONArray array = new JSONArray();

    for (Map.Entry<Integer, List<String>> entry : this.nodeToDates.entrySet())
      array.put(
          new JSONObject()
              .put(Constants.AVAILABLE_LOG_DATES_COLUMNS.NODE_ID, entry.getKey())
              .put(Constants.AVAILABLE_LOG_DATES_COLUMNS.DATE, entry.getValue()));

    return new ResponseEntity<>(array.toString(), HttpStatus.OK);
  }
}
