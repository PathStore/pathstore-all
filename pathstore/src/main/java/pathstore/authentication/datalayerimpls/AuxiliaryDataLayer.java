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
package pathstore.authentication.datalayerimpls;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import pathstore.authentication.CredentialDataLayer;
import pathstore.authentication.credentials.AuxiliaryCredential;
import pathstore.authentication.credentials.Credential;
import pathstore.common.Constants;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is an impl of {@link CredentialDataLayer} and is used to manage the local auxiliary auth
 * table
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuxiliaryDataLayer implements CredentialDataLayer<String, AuxiliaryCredential> {

  /** Instance of this data layer */
  @Getter(lazy = true)
  private static final AuxiliaryDataLayer instance = new AuxiliaryDataLayer();

  /**
   * @param session session to execute on
   * @return map from primary key of credential to credential object
   */
  @Override
  public ConcurrentMap<String, AuxiliaryCredential> load(@NonNull final Session session) {
    return StreamSupport.stream(
            session
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUXILIARY_AUTH))
                .spliterator(),
            true)
        .map(this::buildFromRow)
        .collect(Collectors.toConcurrentMap(Credential::getSearchable, Function.identity()));
  }

  /**
   * @param row row from pathstore_applications.local_auxiliary_auth
   * @return auxiliary credential from row
   */
  @Override
  public AuxiliaryCredential buildFromRow(@NonNull final Row row) {
    return new AuxiliaryCredential(
        row.getString(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.NAME),
        row.getString(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.USERNAME),
        row.getString(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.PASSWORD));
  }

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  @Override
  public AuxiliaryCredential write(
      @NonNull final Session session, @NonNull final AuxiliaryCredential credential) {
    session.execute(
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUXILIARY_AUTH)
            .value(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.NAME, credential.getSearchable())
            .value(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.USERNAME, credential.getUsername())
            .value(Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.PASSWORD, credential.getPassword()));
    return credential;
  }

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  @Override
  public void delete(
      @NonNull final Session session, @NonNull final AuxiliaryCredential credential) {
    session.execute(
        QueryBuilder.delete()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_AUXILIARY_AUTH)
            .where(
                QueryBuilder.eq(
                    Constants.LOCAL_AUXILIARY_AUTH_COLUMNS.NAME, credential.getSearchable())));
  }
}
