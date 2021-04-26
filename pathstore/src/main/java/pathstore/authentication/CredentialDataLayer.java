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

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import pathstore.authentication.credentials.Credential;

import java.util.concurrent.ConcurrentMap;

/**
 * This class is used to denote how to store a credential object in the database depending on where
 * it is generated
 *
 * @see CredentialCache#getNodes() For the pathstore_applications.local_auth table
 * @see CredentialCache#getClients() For the pathstore_applications.local_client_auth table
 * @param <SearchableT> Data type of primary key
 * @param <CredentialT> Credential Type
 */
public interface CredentialDataLayer<SearchableT, CredentialT extends Credential<SearchableT>> {

  /** @return map from primary key of Credential to credential object */
  ConcurrentMap<SearchableT, CredentialT> load(final Session session);

  /**
   * @param row row from a keyspace and table that stores a credential object
   * @see Credential
   * @return Credential credential parsed row
   */
  CredentialT buildFromRow(final Row row);

  /**
   * @param session session object to write to the database
   * @param credential credentials to write
   * @return credential object that was passed
   */
  CredentialT write(final Session session, final CredentialT credential);

  /**
   * @param session session object to delete to the database
   * @param credential credentials to delete
   */
  void delete(final Session session, final CredentialT credential);
}
