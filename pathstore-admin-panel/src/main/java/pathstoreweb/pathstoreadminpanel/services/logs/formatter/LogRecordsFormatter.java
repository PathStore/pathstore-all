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

package pathstoreweb.pathstoreadminpanel.services.logs.formatter;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.List;

/** This class is used to format a list of logs into a readable json array */
public class LogRecordsFormatter implements IFormatter {

  /** Given from {@link pathstoreweb.pathstoreadminpanel.services.logs.GetLogRecords} */
  private final List<String> messages;

  /** @param messages {@link #messages} */
  public LogRecordsFormatter(final List<String> messages) {
    this.messages = messages;
  }

  /**
   * Parses the list of logs based on the filtering parameters sent by the user into a json object
   * of logs: string[]
   *
   * @return json object of parsed logs
   */
  @Override
  public ResponseEntity<String> format() {

    JSONObject object = new JSONObject();

    object.put(Constants.LOGS, messages);

    return new ResponseEntity<>(object.toString(), HttpStatus.OK);
  }
}
