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

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstore.common.tables.ServerEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.GetServers;

import java.util.List;

/** This formatter is used to format the response of querying all available servers */
public class GetServersFormatter implements IFormatter {

  /** List of servers stored in {@link Constants#SERVERS} and gathered from {@link GetServers} */
  private final List<ServerEntry> listOfServers;

  /** @param listOfServers {@link #listOfServers} */
  public GetServersFormatter(final List<ServerEntry> listOfServers) {
    this.listOfServers = listOfServers;
  }

  /** @return list of all servers queried in json format */
  @Override
  public ResponseEntity<String> format() {

    JSONArray jsonArray = new JSONArray();

    for (ServerEntry server : this.listOfServers) {
      JSONObject object = new JSONObject();

      object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, server.serverUUID.toString());
      object.put(Constants.SERVERS_COLUMNS.IP, server.ip);
      object.put(Constants.SERVERS_COLUMNS.USERNAME, server.username);
      object.put(Constants.SERVERS_COLUMNS.AUTH_TYPE, server.authType.toString());
      object.put(Constants.SERVERS_COLUMNS.SSH_PORT, server.sshPort);
      object.put(Constants.SERVERS_COLUMNS.GRPC_PORT, server.grpcPort);
      object.put(Constants.SERVERS_COLUMNS.NAME, server.name);

      jsonArray.put(object);
    }

    return new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
  }
}
