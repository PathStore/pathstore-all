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
package pathstore.authentication;

import com.datastax.driver.core.Session;
import org.apache.commons.text.RandomStringGenerator;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

/**
 * This class is used to modify roles and grant / revoke permissions from a role.
 *
 * @see CredentialCache
 */
public class CassandraAuthenticationUtil {

  /**
   * This function is used to generate an alpha numeric password of size 100
   *
   * @return random alpha number password of size 100
   */
  public static String generateAlphaNumericPassword() {
    return new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(1)
        + new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(LETTERS, DIGITS)
            .build()
            .generate(99);
  }

  /**
   * This function is used to create a role on a cassandra cluster
   *
   * @param session session to execute command on
   * @param roleName role name to create
   * @param isSuperUser whether the role is a super user
   * @param canLogin whether the role can login
   * @param password password for the role
   * @apiNote The session provided must have super user access to create roles
   */
  public static void createRole(
      final Session session,
      final String roleName,
      final boolean isSuperUser,
      final boolean canLogin,
      final String password) {
    session.execute(
        String.format(
            "CREATE ROLE %s WITH SUPERUSER = %b AND LOGIN = %b and PASSWORD = '%s'",
            roleName, isSuperUser, canLogin, password));
  }

  /**
   * This function is used to drop a role on a cassandra cluster
   *
   * @param session session to execute command on
   * @param roleName role name to drop
   * @apiNote The session provided must have super user access to drop roles
   */
  public static void dropRole(final Session session, final String roleName) {
    session.execute(String.format("DROP ROLE IF EXISTS %s", roleName));
  }

  /**
   * Function to grant select / insert / update / delete / truncate on a keyspace to a user account
   *
   * @param session session to execute with
   * @param keyspace keyspace to grant on
   * @param user user to give permissions to
   * @apiNote The session provided must have super user access to grant permissions to other roles
   */
  public static void grantAccessToKeyspace(
      final Session session, final String keyspace, final String user) {
    session.execute(String.format("GRANT SELECT ON KEYSPACE %s TO %s", keyspace, user));

    session.execute(String.format("GRANT MODIFY ON KEYSPACE %s TO %s", keyspace, user));
  }

  /**
   * Function to revoke select / insert / update / delete / truncate on a keyspace to a user account
   *
   * @param session session to execute with
   * @param keyspace keyspace to grant on
   * @param user user to give permissions to
   * @apiNote The session provided must have super user access to revoke permission from other roles
   */
  public static void revokeAccessToKeyspace(
      final Session session, final String keyspace, final String user) {
    session.execute(String.format("REVOKE SELECT ON KEYSPACE %s FROM %s", keyspace, user));

    session.execute(String.format("REVOKE MODIFY ON KEYSPACE %s FROM %s", keyspace, user));
  }
}
