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
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;

public class LocalTableTest {
  public static void main(String[] args) throws InterruptedException {
    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto("pathstore_demo", "local_table")
            .value("name", "myles")
            .value("number", 5);

    session.execute(insert);

    read(session);

    Update update = QueryBuilder.update("pathstore_demo", "local_table");
    update.where(QueryBuilder.eq("name", "myles"));
    update.with(QueryBuilder.set("number", 6));

    session.execute(update);

    read(session);

    PathStoreClientAuthenticatedCluster.getInstance().close();
  }

  private static void read(final PathStoreSession session) {
    Select select = QueryBuilder.select().all().from("pathstore_demo", "local_table");

    for (Row row : session.execute(select)) {
      System.out.println(
          String.format("Name: %s, number: %d", row.getString("name"), row.getInt("number")));
    }
  }
}
