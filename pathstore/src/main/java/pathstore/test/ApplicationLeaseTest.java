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
import lombok.SneakyThrows;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreSession;
import pathstore.common.ApplicationLeaseCache;
import pathstore.sessions.PathStoreSessionManager;
import pathstore.system.logging.LoggerLevel;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.util.Optional;

/** This test is to ensure that clients properly detect expired qc entries. */
public class ApplicationLeaseTest {
  @SneakyThrows
  public static void main(String[] args) {
    PathStoreLoggerFactory.setGlobalLoggerLevel(LoggerLevel.FINEST);

    PathStoreSession session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    if (args.length == 0) {
      System.out.println(
          "Args are of size 0, witting insert and update records for name=myles and sport=golf");

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

    Optional<ApplicationLeaseCache.ApplicationLease> leaseOptional =
        ApplicationLeaseCache.getInstance().getLease("pathstore_demo");

    if (leaseOptional.isPresent()) {
      ApplicationLeaseCache.ApplicationLease lease = leaseOptional.get();

      long clt = lease.getClientLeaseTime();
      System.out.println("Reading select for first time");
      read(session, select);

      try {
        System.out.println(String.format("Sleeping %d + %d = %d", clt, 1000, clt + 1000));
        Thread.sleep(clt + 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Reading second time");
      read(session, select);

    } else {
      throw new RuntimeException("Lease should be present after successful registration of client");
    }

    PathStoreClientAuthenticatedCluster.getInstance().close();
  }

  private static void read(final PathStoreSession session, final Select select) {
    for (Row row :
        session.execute(
            select, PathStoreSessionManager.getInstance().getKeyspaceToken("demo-session")))
      System.out.println(
          String.format(
              "%s %s %d", row.getString("name"), row.getString("sport"), row.getInt("years")));
  }
}
