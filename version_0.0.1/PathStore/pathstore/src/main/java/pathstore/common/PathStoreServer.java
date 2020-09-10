/**
 * ********
 *
 * <p>Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*********
 */
package pathstore.common;

import pathstore.sessions.SessionToken;
import pathstore.util.SchemaInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface PathStoreServer extends Remote {
  String updateCache(String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException;

  UUID createQueryDelta(
      String keyspace, String table, byte[] clauses, UUID parentTimestamp, int nodeID, int limit)
      throws RemoteException;

  String registerApplicationClient(final String applicationName, final String password)
      throws RemoteException;

  SchemaInfo getSchemaInfo(final String keyspace) throws RemoteException;

  boolean validateSession(final SessionToken sessionToken) throws RemoteException;

  void forcePush(final SessionToken sessionToken, final int lca) throws RemoteException;

  void forceSynchronize(final SessionToken sessionToken, final int lca) throws RemoteException;

  int getLocalNodeId() throws RemoteException;
}
