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

package pathstoreweb.pathstoreadminpanel.validator;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class is used to extend every payload.
 *
 * <p>Each payload with implement {@link #calculateErrors()} and the controller will call hasErrors
 * which will generate the list of errors that
 */
public abstract class ValidatedPayload {

  /** Internal list of errors */
  private String[] errors = null;

  /**
   * Sets {@link #errors} to what {@link #calculateErrors()} returns
   *
   * @return whether or not there are errors
   */
  public boolean hasErrors() {
    this.errors = this.calculateErrors();
    return Arrays.stream(errors).anyMatch(Objects::nonNull);
  }

  /** @return generated errors */
  public String[] getErrors() {
    return this.errors;
  }

  /**
   * Simple checker to ensure all values are non-null
   *
   * @param objects objects from payload
   * @return true iff all objects are non-null
   */
  protected boolean bulkNullCheck(final Object... objects) {
    return Arrays.stream(objects).anyMatch(Objects::isNull);
  }

  /** @return list of errors (internal use only) */
  protected abstract String[] calculateErrors();
}
