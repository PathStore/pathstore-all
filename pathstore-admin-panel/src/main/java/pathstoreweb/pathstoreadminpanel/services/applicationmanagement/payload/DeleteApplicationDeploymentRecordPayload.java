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

package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.NodeSchemaProcessStatus;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.ApplicationRecord;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import java.util.*;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.DELETE_APPLICATION_DEPLOYMENT_RECORD_PAYLOAD.*;

/** This payload is used to group records together to request to un-deploy */
public class DeleteApplicationDeploymentRecordPayload extends ValidatedPayload {

  public List<ApplicationRecord> records;

  /**
   * Validation
   *
   * <p>(1): Atleast one record
   *
   * <p>(2): no more then one keyspace can be in the records set
   *
   * <p>(3): If at least one node id isn't valid or at least one node doesn't have the keyspace
   * installed this is a invalid record set
   *
   * <p>(4): All records must wait for all their children
   *
   * @return all null iff valid record set
   */
  @Override
  @SuppressWarnings("ALL")
  protected String[] calculateErrors() {

    // (1)
    if (records == null || records.size() == 0) return new String[] {EMPTY};

    // (2)
    if (records.stream().map(i -> i.keyspaceName).distinct().count() != 1)
      return new String[] {TO_MANY_KEYSPACES};

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Select queryAllDeployment =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

    Set<Integer> validNodeIdSet = new HashSet<>();

    Map<Integer, Set<Integer>> parentNodeToListOfChildren = new HashMap<>();

    for (Row row : session.execute(queryAllDeployment)) {
      int newNode = row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID);
      int parentNode = row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID);
      validNodeIdSet.add(newNode);
      parentNodeToListOfChildren.computeIfAbsent(parentNode, k -> new HashSet<>());
      parentNodeToListOfChildren.get(parentNode).add(newNode);
    }

    Select nodeSchemaSelect =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    Set<Integer> applicationNodeIdSet = new HashSet<>();

    for (Row row : session.execute(nodeSchemaSelect))
      if (row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME)
              .equals(this.records.get(0).keyspaceName)
          && NodeSchemaProcessStatus.valueOf(
                  row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS))
              == NodeSchemaProcessStatus.INSTALLED)
        applicationNodeIdSet.add(row.getInt(Constants.NODE_SCHEMAS_COLUMNS.NODE_ID));

    // (3)
    if (!this.records.stream().map(i -> i.nodeId).allMatch(validNodeIdSet::contains)
        || !this.records.stream().map(i -> i.nodeId).allMatch(applicationNodeIdSet::contains))
      return new String[] {INVALID_RECORD};

    // (4)
    if (!this.records.stream()
        .allMatch(
            o ->
                parentNodeToListOfChildren.containsKey(o.nodeId)
                    ? o.waitFor.equals(parentNodeToListOfChildren.get(o.nodeId))
                    : o.waitFor.equals(Collections.singleton(-1))))
      return new String[] {INVALID_WAIT_FOR};

    return new String[0];
  }
}
