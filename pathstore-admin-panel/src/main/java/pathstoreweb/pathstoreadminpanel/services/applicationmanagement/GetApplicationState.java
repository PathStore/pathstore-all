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

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.NodeSchemaEntry;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.formatter.GetApplicationStateFormatter;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO: Maybe use a map from nodeid, to linked list of states
 *
 * <p>This is the Application State service. It queries all nodes and what state they are in for
 * each application if applicable
 *
 * @see Constants#NODE_SCHEMAS
 * @see Constants.NODE_SCHEMAS_COLUMNS
 */
public class GetApplicationState implements IService {
  /**
   * @return json response of data queried
   * @see GetApplicationStateFormatter
   */
  @Override
  public ResponseEntity<String> response() {
    return new GetApplicationStateFormatter(this.getApplicationStates()).format();
  }

  /**
   * Queries all application states for each node into a list
   *
   * @return list of application entries
   * @see ApplicationEntry
   */
  @SuppressWarnings("ALL")
  private List<NodeSchemaEntry> getApplicationStates() {
    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select queryAllNodeSchemas =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    LinkedList<NodeSchemaEntry> entries = new LinkedList<>();

    for (Row row : session.execute(queryAllNodeSchemas))
      entries.addFirst(NodeSchemaEntry.fromRow(row));

    return entries;
  }
}
