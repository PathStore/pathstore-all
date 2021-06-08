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

package pathstoreweb.pathstoreadminpanel.services.applications.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.applications.Application;
import pathstoreweb.pathstoreadminpanel.services.applications.GetApplications;

import java.util.List;

/**
 * Simple formatter for a list of available applications.
 *
 * <p>Used by the frontend to limit what the user can query on application installation
 *
 * @see GetApplications
 */
public class GetApplicationsFormatter implements IFormatter {

  /** List of available applications */
  private final List<Application> applications;

  /** @param applications {@link #applications} */
  public GetApplicationsFormatter(final List<Application> applications) {
    this.applications = applications;
  }

  /**
   * Json array of all available applications
   *
   * @return json array
   */
  @Override
  public ResponseEntity<String> format() {
    JSONArray response = new JSONArray();

    for (Application application : this.applications) {
      JSONObject object = new JSONObject();

      object
          .put(Constants.APPS_COLUMNS.KEYSPACE_NAME, application.keyspaceName)
          .put(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA, application.augmentedSchema);

      response.put(object);
    }

    return new ResponseEntity<>(response.toString(), HttpStatus.OK);
  }
}
