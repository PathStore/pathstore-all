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

public class ReadWriteTest {
  public static void main(String[] args) throws InterruptedException {
    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    if (args.length == 0) {
      Insert insert =
          QueryBuilder.insertInto("pathstore_demo", "users")
              .value("name", "myles")
              .value("sport", "golf");

      session.execute(insert);

      Update update = QueryBuilder.update("pathstore_demo", "users");

      update.where(QueryBuilder.eq("name", "myles"));
      update.with(QueryBuilder.set("years", 20));

      session.execute(update);
    }

    Select select = QueryBuilder.select().all().from("pathstore_demo", "users");

    for (Row row : session.execute(select))
      System.out.println(
          String.format(
              "%s %s %d", row.getString("name"), row.getString("sport"), row.getInt("years")));

    PathStoreClientAuthenticatedCluster.getInstance().close();
  }
}
