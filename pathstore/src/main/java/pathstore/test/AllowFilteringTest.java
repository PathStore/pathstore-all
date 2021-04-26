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
package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.common.Constants;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * Steps to test allow filtering logic.
 *
 * <p>1) Deploy a new node as a child to the root node
 *
 * <p>2) Register an application and deploy it on both nodes.
 *
 * <p>3) After deployment has finished run this test with args INSTALLING on both the root node and
 * the child node. This should print 0 results. Then run it on both nodes with INSTALLED. This
 * should print 2 records. If both that occurs, the test has passed.
 */
public class AllowFilteringTest {
  public static void main(String[] args) {

    Session psSession = PathStorePrivilegedCluster.getSuperUserInstance().psConnect();

    Select select =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);
    select.where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS, args[0]));
    select.allowFiltering();

    System.out.println(String.format("Executing query %s", select.toString()));

    for (Row row : psSession.execute(select)) {
      System.out.println(
          String.format(
              "Parent Node Id: %d Node id: %d Server UUID: %s Process Status: %s Who to wait for: %s",
              row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID),
              row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
              row.getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID),
              row.getString(Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS),
              row.getList(Constants.DEPLOYMENT_COLUMNS.WAIT_FOR, Integer.class)));
    }

    PathStorePrivilegedCluster.getSuperUserInstance().close();
    System.out.println("Test complete");
  }
}
