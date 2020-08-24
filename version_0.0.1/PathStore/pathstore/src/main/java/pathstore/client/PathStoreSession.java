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
package pathstore.client;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import com.google.common.util.concurrent.ListenableFuture;
import pathstore.common.QueryCache;
import pathstore.exception.InvalidKeyspaceException;
import pathstore.exception.InvalidStatementTypeException;
import pathstore.exception.PathStoreRemoteException;
import pathstore.sessions.PathStoreSessionManager;
import pathstore.sessions.SessionToken;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;

import java.util.*;
import java.util.stream.Collectors;

/** This class is used to execute queries onto the database and adhere to the log structure */
public class PathStoreSession implements Session {

  /** Logger */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreSession.class);

  /** Raw session used to execute queries */
  private final Session session;

  /** @param cluster cluster to create session */
  public PathStoreSession(final Cluster cluster) {
    this.session = cluster.connect();
  }

  public String getLoggedKeyspace() {
    return this.session.getLoggedKeyspace();
  }

  public Session init() {
    throw new UnsupportedOperationException();
  }

  public ListenableFuture<Session> initAsync() {
    throw new UnsupportedOperationException();
  }

  public ResultSet execute(final String query) {
    throw new UnsupportedOperationException();
  }

  public ResultSet execute(final String query, final Object... values) {
    throw new UnsupportedOperationException();
  }

  public ResultSet execute(final String query, final Map<String, Object> values) {
    throw new UnsupportedOperationException();
  }

  public ResultSet execute(final Statement statement) {
    return executeNormal(statement, null);
  }

  public ResultSet execute(final Statement statement, final SessionToken sessionToken)
      throws PathStoreRemoteException {
    return executeNormal(statement, sessionToken);
  }

  public ResultSet executeNormal(Statement statement, SessionToken sessionToken)
      throws PathStoreRemoteException {

    // Myles: Temporary to test session management
    if (sessionToken == null) {
      sessionToken = PathStoreSessionManager.getInstance().getKeyspaceToken("test-session");
    }

    // TODO: Check if statement throws errors

    String keyspace = statement.getKeyspace();
    String table = "";
    boolean allowFiltering = false;

    if (!keyspace.startsWith("pathstore"))
      throw new InvalidKeyspaceException("Keyspace does not start with pathstore prefix");

    if (statement instanceof Select) {
      Select select = (Select) statement;

      table = select.getTable();

      if (!table.startsWith("local_")) {

        List<Clause> original = select.where().getClauses();

        int originalSize = original.size();

        List<Clause> strippedClauses = this.parseClauses(select);

        allowFiltering = originalSize > strippedClauses.size();

        this.printDifference(original, strippedClauses);

        QueryCache.getInstance().updateCache(keyspace, table, strippedClauses, -1);
      }
    } else if (statement instanceof Insert) {
      Insert insert = (Insert) statement;

      table = insert.getTable();

      if (!table.startsWith("local_")) {
        insert.value("pathstore_version", QueryBuilder.now());
        insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        insert.value("pathstore_dirty", true);
      }
    } else if (statement instanceof Delete) {
      Delete delete = (Delete) statement;

      table = delete.getTable();
      if (!table.startsWith("local_")) {
        Insert insert = QueryBuilder.insertInto(delete.getKeyspace(), delete.getTable());

        insert.value("pathstore_version", QueryBuilder.now());
        insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        insert.value("pathstore_dirty", true);
        insert.value("pathstore_deleted", true);

        List<Clause> clauses = delete.where().getClauses();

        for (Clause clause : clauses) {
          String name = clause.getName();
          Object value = clause.getValue();
          insert.value(name, value);
        }

        statement = insert;
      }
    } else if (statement instanceof Update) {
      Update update = (Update) statement;

      table = update.getTable();

      if (!table.startsWith("local_")) {

        Insert insert = QueryBuilder.insertInto(update.getKeyspace(), update.getTable());

        insert.value("pathstore_version", QueryBuilder.now());
        insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        insert.value("pathstore_dirty", true);

        Assignments assignment = update.with();

        for (Assignment a : assignment.getAssignments()) {
          String name = a.name;
          Object value = ((Assignment.SetAssignment) a).value;
          insert.value(name, value);
        }

        List<Clause> clauses = Update.where().getClauses();

        for (Clause clause : clauses) {
          String name = clause.getName();
          Object value = clause.getValue();
          insert.value(name, value);
        }
        statement = insert;
      }
    } else throw new InvalidStatementTypeException();

    // Update session token if present to include either the table or keyspace depending on session
    // type
    if (sessionToken != null) {
      switch (sessionToken.sessionType) {
        case KEYSPACE:
          sessionToken.addEntry(keyspace);
          break;
        case TABLE:
          sessionToken.addEntry(String.format("%s.%s", keyspace, table));
          break;
      }
    }

    // hossein here:
    statement.setFetchSize(1000);

    ResultSet set = this.session.execute(statement);

    return new PathStoreResultSet(this.session, set, keyspace, table, allowFiltering);
  }

  private void printDifference(final List<Clause> original, final List<Clause> stripped) {
    List<Clause> originalClone = new ArrayList<>(original);

    originalClone.removeAll(stripped);

    for (Clause clause : originalClone) {
      logger.info(String.format("stripped clause on column %s", clause.getName()));
    }
  }

  private List<Clause> parseClauses(final Select select) {

    List<Clause> clauses = select.where().getClauses();

    Collection<String> partitionKeys =
        SchemaInfo.getInstance().getPartitionColumnNames(select.getKeyspace(), select.getTable());

    Collection<String> clusteringKeys =
        SchemaInfo.getInstance().getClusterColumnNames(select.getKeyspace(), select.getTable());

    return select.where().getClauses().stream()
            .map(Clause::getName)
            .allMatch(partitionKeys::contains)
        ? clauses.stream()
            .filter(
                clause ->
                    partitionKeys.contains(clause.getName())
                        || clusteringKeys.contains(clause.getName()))
            .collect(Collectors.toList())
        : Collections.emptyList();
  }

  public ResultSetFuture executeAsync(final String query) {
    throw new UnsupportedOperationException();
  }

  public ResultSetFuture executeAsync(String query, Object... values) {
    throw new UnsupportedOperationException();
  }

  public ResultSetFuture executeAsync(String query, Map<String, Object> values) {
    throw new UnsupportedOperationException();
  }

  public ResultSetFuture executeAsync(Statement statement) {
    throw new UnsupportedOperationException();
  }

  public PreparedStatement prepare(String query) {
    throw new UnsupportedOperationException();
  }

  public PreparedStatement prepare(RegularStatement statement) {
    throw new UnsupportedOperationException();
  }

  public ListenableFuture<PreparedStatement> prepareAsync(String query) {
    throw new UnsupportedOperationException();
  }

  public ListenableFuture<PreparedStatement> prepareAsync(RegularStatement statement) {
    throw new UnsupportedOperationException();
  }

  public CloseFuture closeAsync() {
    throw new UnsupportedOperationException();
  }

  public void close() {
    this.session.close();
  }

  public boolean isClosed() {
    return this.session.isClosed();
  }

  public Cluster getCluster() {
    throw new UnsupportedOperationException();
  }

  public State getState() {
    throw new UnsupportedOperationException();
  }
}
