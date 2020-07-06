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

import java.util.*;
import java.util.stream.Collectors;

import pathstore.common.QueryCache;
import pathstore.exception.InvalidKeyspaceException;
import pathstore.exception.InvalidStatementTypeException;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.exception.PathStoreRemoteException;
import pathstore.system.PathStorePriviledgedCluster;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Assignment;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import com.google.common.util.concurrent.ListenableFuture;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

/**
 * TODO: Fix all string literals and move to {@link pathstore.common.Constants} This is a class is
 * used for communication to the local Cassandra instance
 *
 * @apiNote Many of the functions throw {@link UnsupportedOperationException}
 * @see Session
 */
public class PathStoreSession implements Session {

  /**
   * Internal session variable that was gathered from the cassandra cluster object
   *
   * @see Cluster
   */
  private final Session session;

  /** TODO: Find purpose */
  boolean useColumn = false; // temporary

  /** @param cluster set internal session field to the clusters session */
  public PathStoreSession(final Cluster cluster) {
    this.session = cluster.connect();
  }

  /** @return TODO: find purpose */
  public String getLoggedKeyspace() {
    return this.session.getLoggedKeyspace();
  }

  /**
   * @return nothing
   * @apiNote Unsupported Operation
   */
  public Session init() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return nothing
   * @apiNote Unsupported Operation
   */
  public ListenableFuture<Session> initAsync() {
    throw new UnsupportedOperationException();
  }

  /**
   * TODO: Potentially remove functionality to use cql strings as querying TODO: Should not throw
   * {@link UnsupportedOperationException} if the operation is supported
   *
   * @param query cql string to execute on the local cassandra DB
   * @return response from local db
   */
  public ResultSet execute(final String query) {
    if (query.toLowerCase().contains("local_".toLowerCase())) return this.session.execute(query);
    else throw new UnsupportedOperationException();
  }

  /**
   * @return nothing
   * @apiNote Unsupported Operation
   */
  public ResultSet execute(final String query, final Object... values) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return nothing
   * @apiNote Unsupported Operation
   */
  public ResultSet execute(final String query, final Map<String, Object> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * Only used by {@link pathstore.common.PathStoreMigrate} and {@link
   * pathstore.common.PathStoreAuthenticate} TODO: Potential only allow for internal usage only
   * TODO: Migrate literal strings to strings in {@link pathstore.common.Constants}
   *
   * @param statement statement to execute on local cassandra service
   * @param device TODO: Explain
   * @return result of your query
   */
  public ResultSet executeLocal(final Statement statement, final String device) {
    String keyspace = statement.getKeyspace();
    String table = "";
    if (statement instanceof Select) {
      Select select = (Select) statement;
      table = select.getTable();
    } else if (statement instanceof Insert) {
      Insert insert = (Insert) statement;

      table = insert.getTable();

      if (!table.startsWith("local_")) {
        insert.value("pathstore_version", QueryBuilder.now());
        insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        insert.value("pathstore_dirty", false);

        if (device != null) {
          if (useColumn) insert.value("pathstore_insert_sid", device);
          // or
          else
            QueryCache.getInstance()
                .updateDeviceCommandCache(
                    device, keyspace, table, InsertToSelect(insert).where().getClauses(), -1);
        }
      }
    }

    // hossein here:
    statement.setFetchSize(1000);

    return new PathStoreResultSet(this.session.execute(statement), keyspace, table, false);
  }

  /**
   * @param statement Statement to execute
   * @return result of query
   * @see #executeNomral(Statement, String)
   */
  public ResultSet execute(final Statement statement) {
    return executeNomral(statement, null);
  }

  /**
   * @param statement Statement to execute
   * @param device TODO: Explain
   * @return result of query
   * @see #executeNomral(Statement, String)
   */
  public ResultSet execute(final Statement statement, final String device)
      throws PathMigrateAlreadyGoneException, PathStoreRemoteException {
    return executeNomral(statement, device);
  }

  /**
   * TODO: Potentially private TODO: Migrate all string literals to {@link
   * pathstore.common.Constants}
   *
   * <p>Select: Update query cache to include our current query. TODO: Smart coverage Insert: Append
   * the hidden values. Delete: This operation is actually an insert with the row pathstore_deleted
   * added. Update: This operation is actually an insert with the updated data. To keep our design
   * of a log of data over time
   *
   * <p>Then our operation is executed over our database connection and a result set is returned to
   * be iterated over.
   *
   * @param statement statement to execute. This value does get reset
   * @param device TODO: Explain
   * @return result of statement iff one of the following exceptions is not thrown
   * @throws PathMigrateAlreadyGoneException
   * @throws PathStoreRemoteException
   */
  public ResultSet executeNomral(Statement statement, final String device)
      throws PathMigrateAlreadyGoneException, PathStoreRemoteException {
    String keyspace = statement.getKeyspace();
    String table = "";
    boolean allowFiltering = false;

    if (!keyspace.startsWith("pathstore"))
      throw new InvalidKeyspaceException("Keyspace does not start with pathstore prefix");

    if (statement instanceof Select) {
      Select select = (Select) statement;

      allowFiltering = select.hasAllowFiltering();

      table = select.getTable();

      if (!table.startsWith("local_")) {
        QueryCache.getInstance().updateCache(keyspace, table, this.parseClauses(select), -1);

        /*
        if (device != null)
          QueryCache.getInstance().updateDeviceCommandCache(device, keyspace, table, clauses, l);
         */
      }
    } else if (statement instanceof Insert) {
      Insert insert = (Insert) statement;

      table = insert.getTable();

      if (!table.startsWith("local_")) {
        insert.value("pathstore_version", QueryBuilder.now());
        insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        insert.value("pathstore_dirty", true);

        if (device != null) {
          if (useColumn) insert.value("pathstore_insert_sid", device);
          // or
          else
            QueryCache.getInstance()
                .updateDeviceCommandCache(
                    device, keyspace, table, InsertToSelect(insert).where().getClauses(), -1);
        }
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

    // hossein here:
    statement.setFetchSize(1000);

    ResultSet set = this.session.execute(statement);

    return new PathStoreResultSet(set, keyspace, table, allowFiltering);
  }

  /**
   * TODO: This function currently doesn't account for secondary indexes
   *
   * <p>TODO: Solution for secondary indexes would to be to determine if a where clause points to an
   * index and if it does exclude that where clause
   *
   * <p>This function is used to determine what where clauses are valid to store in the QueryCache.
   *
   * <p>There are 3 main cases that are considered:
   *
   * <p>1) The query doesn't use allow filtering thus all records will be considered with a given
   * primary key so all clauses are returned.
   *
   * <p>2) The query uses allow filtering but not all partition key columns are fixed, so an empty
   * list of clauses is returned.
   *
   * <p>3) The query uses allow filtering and all partition key columns are fixed and some number of
   * clustering key columns are fixed (may be zero). Only the partition key statements and
   * clustering columns are returned.
   *
   * @param select
   * @return
   */
  private List<Clause> parseClauses(final Select select) {

    List<Clause> clauses = select.where().getClauses();

    if (select.hasAllowFiltering()) {

      List<Column> columns =
          SchemaInfo.getInstance().getTableColumns(select.getKeyspace(), select.getTable());

      Set<String> partitionKeys =
          columns.stream()
              .filter(
                  column ->
                      column.kind.compareTo("partition_key") == 0
                          && !column.column_name.startsWith("pathstore_"))
              .map(column -> column.column_name)
              .collect(Collectors.toSet());

      Set<String> clusteringKeys =
          columns.stream()
              .filter(
                  column ->
                      column.kind.compareTo("clustering") == 0
                          && !column.column_name.startsWith("pathstore_"))
              .map(column -> column.column_name)
              .collect(Collectors.toSet());

      Set<String> columnNames =
          select.where().getClauses().stream().map(Clause::getName).collect(Collectors.toSet());

      // if the where clauses contains all parition keys then filter all regular statements out,
      // else return an empty list
      clauses =
          columnNames.containsAll(partitionKeys)
              ? clauses.stream()
                  .filter(
                      clause ->
                          partitionKeys.contains(clause.getName())
                              && clusteringKeys.contains(clause.getName()))
                  .collect(Collectors.toList())
              : Collections.emptyList();
    }

    return clauses;
  }

  /**
   * TODO: Explain TODO: This function is only used when executing queries with a device in mind but
   * those aren't used.
   *
   * @param ins insert statement
   * @return select?
   */
  // Hossein
  public static Select InsertToSelect(final Insert ins) {
    Select slct = QueryBuilder.select().all().from(ins.getKeyspace() + "." + ins.getTable());
    List<ColumnMetadata> pkl =
        PathStorePriviledgedCluster.getInstance()
            .getMetadata()
            .getKeyspace(ins.getKeyspace())
            .getTable(ins.getTable())
            .getPrimaryKey();
    String pk = pkl.get(0).getName();
    for (int i = 0; i < ins.getNamesArrayList().size(); i++) {
      String name = (String) ins.getNamesArrayList().get(i);
      if (pk.equals(name)) {
        slct.where(QueryBuilder.eq(name, ins.getValuesArrayList().get(i)));
      }
    }
    return slct;
  }

  /**
   * TODO: Should not throw {@link UnsupportedOperationException} when the operation is only
   * supported if a condition holds. Modify to another error
   *
   * @param query command to query
   * @return async result set.
   */
  public ResultSetFuture executeAsync(final String query) {
    if (query.toLowerCase().contains("local_".toLowerCase()))
      return this.session.executeAsync(query);
    else throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public ResultSetFuture executeAsync(String query, Object... values) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public ResultSetFuture executeAsync(String query, Map<String, Object> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public ResultSetFuture executeAsync(Statement statement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public PreparedStatement prepare(String query) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public PreparedStatement prepare(RegularStatement statement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public ListenableFuture<PreparedStatement> prepareAsync(String query) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public ListenableFuture<PreparedStatement> prepareAsync(RegularStatement statement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public CloseFuture closeAsync() {
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the current session
   *
   * @see #session
   */
  public void close() {
    this.session.close();
  }

  /**
   * You should call this function if you plan on executing something to ensure the session is
   * opened
   *
   * @return whether the current session is opened.
   */
  public boolean isClosed() {
    return this.session.isClosed();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public Cluster getCluster() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return null
   * @apiNote Unsupported Operation
   */
  public State getState() {
    throw new UnsupportedOperationException();
  }
}
