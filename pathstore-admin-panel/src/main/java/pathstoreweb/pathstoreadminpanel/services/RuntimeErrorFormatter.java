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

package pathstoreweb.pathstoreadminpanel.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

/**
 * This class is used to pass an error that has occurred during a request to install/uninstall an
 * application
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.DeployApplications
 * @see pathstoreweb.pathstoreadminpanel.services.applicationmanagement.UnDeployApplications
 */
public class RuntimeErrorFormatter implements IFormatter {

  /** error message that was generated */
  private final String errorMessage;

  /** @param errorMessage {@link #errorMessage} */
  public RuntimeErrorFormatter(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Wraps the error message into a json string
   *
   * @return wrapped error message
   */
  @Override
  public ResponseEntity<String> format() {
    JSONObject response = new JSONObject();

    response.put("error", this.errorMessage);

    return new ResponseEntity<>(new JSONArray().put(response).toString(), HttpStatus.BAD_REQUEST);
  }
}
