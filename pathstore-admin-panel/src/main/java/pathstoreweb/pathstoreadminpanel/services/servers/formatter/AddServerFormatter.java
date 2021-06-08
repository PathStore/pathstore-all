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

package pathstoreweb.pathstoreadminpanel.services.servers.formatter;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.UUID;

/** This formatter is used to format the response of adding a server */
public class AddServerFormatter implements IFormatter {

  /** Server UUID that was added */
  private final UUID serverUUID;

  /** Ip of server that was added */
  private final String ip;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   */
  public AddServerFormatter(final UUID serverUUID, final String ip) {
    this.serverUUID = serverUUID;
    this.ip = ip;
  }

  /** @return {server_uuid: {@link #serverUUID}, ip: {@link #ip}} */
  @Override
  public ResponseEntity<String> format() {

    JSONObject object = new JSONObject();

    object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, this.serverUUID.toString());
    object.put(Constants.SERVERS_COLUMNS.IP, this.ip);

    return new ResponseEntity<>(object.toString(), HttpStatus.OK);
  }
}
