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
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.system.PathStorePrivilegedCluster;

/**
 * This test will check to make sure that the secondary index recognition works. In order to perform
 * this test you need to do the follow
 *
 * <p>(1): Deploy a child node to the root node
 *
 * <p>(2): Register the pathstore_demo application
 *
 * <p>(3): Deploy the application on the root node and its child.
 *
 * <p>(4): Run the test on the root node with no args. It will write 2 records to the table and will
 * spit out 1 row after the insert and 0 after the update
 *
 * <p>(5): Run the test on the child node with atleast 1 arg. It will print out nothing. If this
 * occurs then the test as passed. This should that all the data is properly loaded through the
 * querycache and that the filtering mechanism in the pathstore iterator / pathstore session is
 * working properly.
 */
public class SecondaryIndexTest {
  private static final Session psSession =
      PathStorePrivilegedCluster.getDaemonInstance().psConnect();

  public static void main(String[] args) {

    if (args.length == 0) {

      Insert insert =
          QueryBuilder.insertInto("pathstore_demo", "users")
              .value("name", "myles")
              .value("sport", "golf")
              .value("years", 20)
              .value("vegetarian", true);

      psSession.execute(insert);

      System.out.println("inserted");

      select(true);

      Update update = QueryBuilder.update("pathstore_demo", "users");
      update.where(QueryBuilder.eq("name", "myles")).with(QueryBuilder.set("vegetarian", false));

      psSession.execute(update);

      System.out.println("updated");
    }

    select(true);

    System.out.println("Test complete");
    psSession.close();
  }

  public static void select(final boolean bool) {
    Select select = QueryBuilder.select().all().from("pathstore_demo", "users");
    select.where(QueryBuilder.eq("vegetarian", bool));

    for (Row row : psSession.execute(select)) {
      System.out.println(
          String.format(
              "%s %s %d %b",
              row.getString("name"),
              row.getString("sport"),
              row.getInt("years"),
              row.getBool("vegetarian")));
    }
  }
}
