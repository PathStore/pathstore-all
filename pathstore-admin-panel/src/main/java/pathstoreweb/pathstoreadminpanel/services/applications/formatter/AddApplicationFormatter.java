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

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

/**
 * Response for {@link pathstoreweb.pathstoreadminpanel.services.applications.AddApplication}.
 * Returns the status of the request
 *
 * @see pathstoreweb.pathstoreadminpanel.services.applications.AddApplication
 */
public class AddApplicationFormatter implements IFormatter {

  /** Status of request */
  private final String status;

  /** @param status {@link #status} */
  public AddApplicationFormatter(final String status) {
    this.status = status;
  }

  /** @return json wrapped status */
  @Override
  public ResponseEntity<String> format() {
    JSONObject object = new JSONObject();

    object.put("keyspace_created", this.status);

    return new ResponseEntity<>(object.toString(), HttpStatus.OK);
  }
}
