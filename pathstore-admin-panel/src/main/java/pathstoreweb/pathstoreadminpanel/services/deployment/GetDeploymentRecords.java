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

package pathstoreweb.pathstoreadminpanel.services.deployment;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.DeploymentEntry;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.deployment.formatter.DeploymentRecordsFormatter;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used when the user wants to get all deployment records to understand the topology
 * of the network
 */
public class GetDeploymentRecords implements IService {

  /** @return {@link DeploymentRecordsFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new DeploymentRecordsFormatter(this.getRecords()).format();
  }

  /**
   * Query all records from database and parse them into a entry class
   *
   * @return list of all parsed entries
   */
  private List<DeploymentEntry> getRecords() {

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select queryAllRecords =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    LinkedList<DeploymentEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllRecords)) entries.addFirst(DeploymentEntry.fromRow(row));

    return entries;
  }
}
