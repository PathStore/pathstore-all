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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;

/**
 * This class is used to test the functionality of client authentication.
 *
 * <p>It is assumed that the cassandra ip/port is defined in the properties file. It is also assumed
 * that the grpc ip/port is defined in the properties file.
 *
 * @implNote You must pass the application name and master password as params, respectively
 */
public class ClientAuthenticationTest {

  public static void main(String[] args) {

    try {
      PathStoreClientAuthenticatedCluster cluster =
          PathStoreClientAuthenticatedCluster.getInstance();

      Session session = cluster.connect();

      try {
        Select select =
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

        session.execute(select);

      } catch (Exception e) {
        System.out.println("Proper permissions are assigned to the role");
      }

      cluster.close();

      System.out.println("Test complete, close completed successfully");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
