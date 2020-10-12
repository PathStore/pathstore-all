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
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;
import pathstore.exception.InvalidKeyspaceException;
import pathstore.exception.InvalidStatementTypeException;
import pathstore.sessions.PathStoreSessionManager;
import pathstore.sessions.SessionToken;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is a wrapper for a session object, this is used to manage selects for data locality,
 * it is also used to manage user defined sessions.
 *
 * @see PathStoreCluster
 * @see PathStoreClientAuthenticatedCluster
 */
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

  /**
   * This function is used to execute a cql request onto the local cassandra database without
   * session tokens.
   *
   * @param statement statement to execute
   * @return result of the request
   */
  public PathStoreResultSet execute(final Statement statement) {
    return executeNormal(statement, null);
  }

  /**
   * This function is used to execute a cql request onto the local cassandra database and supports
   * tracking of session data. Even though sessions are mainly handled on the client side, this is a
   * way to easily allow our library to manage your session data for you.
   *
   * @param statement statement to execute
   * @param sessionToken token to store in
   * @return result set of data
   */
  public PathStoreResultSet execute(final Statement statement, final SessionToken sessionToken) {
    return executeNormal(statement, sessionToken);
  }

  /**
   * This function is used to process a statement with or without a session token.
   *
   * <p>This function behaves different based on what kind of statement you're passing.
   *
   * <p>If you pass a select statement pre-validation occurs so we can verify the validity of the
   * statement, as if it is invalid it potentially will perpetually throw errors on the local nodes
   * end during fetchDelta calls on that qc entry.
   *
   * <p>We also process your statement to ensure that qc entries only contain where clauses on the
   * partition key + some number of clustering clauses as we need to uphold the immutable log
   * structure.
   *
   * <p>We also will set ps_version and ps_parent_timestamp to the current time. and ps_dirty to
   * true as this data is fresh.
   *
   * <p>For more information related to the processing of data and the way it is moved and merged
   * around the network please see the complete documentation present on github.
   *
   * @param statement statement to execute
   * @param sessionToken session token if present
   * @return result set
   * @see #execute(Statement)
   * @see #execute(Statement, SessionToken)
   */
  private PathStoreResultSet executeNormal(Statement statement, final SessionToken sessionToken) {

    String keyspace = statement.getKeyspace();
    String table = "";
    boolean logBreaking = false;
    List<Clause> originalClauses = null;

    if (!keyspace.startsWith(Constants.PATHSTORE_PREFIX))
      throw new InvalidKeyspaceException("Keyspace does not start with pathstore prefix");

    if (statement instanceof Select) {

      // We only do select validation as it is the only query that modifies local state. Everything
      // else will fail at the Cassandra level
      try {
        this.session.execute(statement);
      } catch (Exception e) {
        throw new RuntimeException(
            String.format(
                "Could not execute the select statement %s as it is invalid",
                statement.toString()));
      }

      Select select = (Select) statement;

      table = select.getTable();

      this.checkForViewPrefix(table);

      if (!table.startsWith(Constants.LOCAL_PREFIX)) {

        this.handleSession(sessionToken);

        List<Clause> original = select.where().getClauses();

        int originalSize = original.size();

        List<Clause> strippedClauses = this.parseClauses(select);

        logBreaking = originalSize > strippedClauses.size();

        originalClauses = original;

        QueryCache.getInstance().updateCache(keyspace, table, strippedClauses, -1);
      }
    } else if (statement instanceof Insert) {
      Insert insert = (Insert) statement;

      table = insert.getTable();

      this.checkForViewPrefix(table);

      if (!table.startsWith(Constants.LOCAL_PREFIX)) {
        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VERSION, QueryBuilder.now());
        insert.value(
            Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now());
        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY, true);
      }
    } else if (statement instanceof Delete) {
      Delete delete = (Delete) statement;

      table = delete.getTable();

      this.checkForViewPrefix(table);

      if (!table.startsWith(Constants.LOCAL_PREFIX)) {
        Insert insert = QueryBuilder.insertInto(delete.getKeyspace(), delete.getTable());

        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VERSION, QueryBuilder.now());
        insert.value(
            Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now());
        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY, true);
        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DELETED, true);

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

      this.checkForViewPrefix(table);

      if (!table.startsWith(Constants.LOCAL_PREFIX)) {

        Insert insert = QueryBuilder.insertInto(update.getKeyspace(), update.getTable());

        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VERSION, QueryBuilder.now());
        insert.value(
            Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now());
        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY, true);

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

    return new PathStoreResultSet(this.session, set, keyspace, table, logBreaking, originalClauses);
  }

  /**
   * This simple function is used to determine if a table has the view prefix, as we do not allow
   * the querying of view tables.
   *
   * @param table table to check
   */
  private void checkForViewPrefix(final String table) {
    if (table.startsWith(Constants.VIEW_PREFIX))
      throw new RuntimeException(
          "Cannot perform an operation on a view table with pathstore session");
  }

  /**
   * This function is used to handle a session from the {@link #executeNormal(Statement,
   * SessionToken)} function.
   *
   * <p>If the session passed hasn't been validated (its been loaded in from the sessions file and
   * hasn't been used yet) the session will be validated with the local node. If the session has a
   * different source node then the local node and is valid, the data from the original source node
   * will be migrated to this node. Once that is complete the source node for the session will be
   * changed.
   *
   * <p>If the session is invalid it will be removed from the local session manager.
   *
   * @param sessionToken session token from execute normal
   */
  private void handleSession(final SessionToken sessionToken) {
    if (sessionToken != null && !sessionToken.hasBeenValidated())
      if (PathStoreServerClient.getInstance().validateSession(sessionToken)) {
        logger.info(String.format("Session is valid %s", sessionToken.sessionName));
        sessionToken.isValidated(PathStoreProperties.getInstance().NodeID);
      } else {
        logger.info(String.format("Session is invalid %s", sessionToken.sessionName));
        PathStoreSessionManager.getInstance().removeToken(sessionToken.sessionName);
      }
  }

  /**
   * This function is used to parse all potential log breaking clauses away from a select statement,
   * this is used so we can pull all records from a given primary key during fetchDelta. This
   * however does not affect the end result of the query, but will affect what we internally store
   * within the qc.
   *
   * <p>The logic here is that we need to remove all where clauses on non-primary key columns.
   *
   * <p>We also need to remove all clauses on primary columns if not every partition column is
   * fixed. The only clauses we keep are:
   *
   * <p>if all primary columns are present, then include them and all clustering columns keys, else
   * we keep none of them.
   *
   * @param select select statement to parse.
   * @return list of clauses as described above.
   */
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
