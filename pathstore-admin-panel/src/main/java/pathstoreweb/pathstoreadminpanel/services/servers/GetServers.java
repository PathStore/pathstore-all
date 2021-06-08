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

package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.ServerEntry;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.GetServersFormatter;

import java.util.List;
import java.util.stream.Collectors;

/** Getter service to query all servers stored in the database */
public class GetServers implements IService {

  /** @return {@link GetServersFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new GetServersFormatter(this.getServers()).format();
  }

  /**
   * Select all servers from table and parse them into a list of {@link ServerEntry} object
   *
   * @return list of servers
   */
  private List<ServerEntry> getServers() {
    return PathStoreClientAuthenticatedCluster.getInstance().connect()
        .execute(
            QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS))
        .stream()
        .map(ServerEntry::fromRow)
        .collect(Collectors.toList());
  }
}
