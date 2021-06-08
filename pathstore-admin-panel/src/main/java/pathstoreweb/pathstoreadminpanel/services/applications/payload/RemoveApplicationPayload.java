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

package pathstoreweb.pathstoreadminpanel.services.applications.payload;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.validator.ValidatedPayload;

import static pathstoreweb.pathstoreadminpanel.validator.ErrorConstants.REMOVE_APPLICATION_PAYLOAD.*;

/**
 * This payload is used to remove an application from the network iff it is not deployed on any node
 */
public class RemoveApplicationPayload extends ValidatedPayload {

  /** Application to remove */
  public String applicationName;

  /**
   * Validation
   *
   * <p>(1): Application is present
   *
   * <p>(2): Application is valid
   *
   * <p>(3): Application is not deployed on any node
   *
   * @return all null iff all validation checks pass
   */
  @Override
  protected String[] calculateErrors() {

    // (1)
    if (this.applicationName == null || this.applicationName.length() == 0)
      return new String[] {WRONG_SUBMISSION_FORMAT};

    String[] errors = {APPLICATION_DOESNT_EXIST, null};

    Session session = PathStoreClientAuthenticatedCluster.getInstance().connect();

    // (2)
    Select appsSelect =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    appsSelect.where(QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.applicationName));

    for (Row row : session.execute(appsSelect)) errors[0] = null;

    // (3)
    Select nodeSchemaSelect =
        QueryBuilder.select().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    for (Row row : session.execute(nodeSchemaSelect))
      if (row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME)
          .equals(this.applicationName)) {
        errors[1] = APPLICATION_IS_DEPLOYED;
        break;
      }

    return errors;
  }
}
