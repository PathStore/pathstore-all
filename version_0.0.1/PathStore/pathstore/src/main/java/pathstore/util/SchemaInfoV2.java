package pathstore.util;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to query information about the current state of a database. It allows you to
 * get a list of tables based on keyspace, list of columns based on tables. You can also get the
 * entire map that contains all pathstore related tables and columns
 */
public class SchemaInfoV2 {
  /** Pass via the constructor. Used to communicate with database */
  private final Session session;

  /** List of tables associated with a keyspace */
  private Map<String, List<String>> tables;

  /** List of columns associated with a table */
  private Map<String, List<String>> columns;

  /**
   * @param session Current cassandra database session. Called is required to close this connection
   */
  SchemaInfoV2(final Session session) {
    this.session = session;
    generate();
  }

  /** Allows you to regenerate this data if a change to the databases schemas has changed. */
  void generate() {
    loadAllKeySpaceInfo();
  }

  /** @return Set of all keyspaces */
  Set<String> getAllKeySpaces() {
    return this.tables.keySet();
  }

  /** @return Set of all tables */
  Set<String> getAllTables() {
    return this.columns.keySet();
  }

  /** @return Map of Keyspaces to list of tables */
  Map<String, List<String>> getTablesByKeySpace() {
    return this.tables;
  }

  /**
   * @param keySpace key space you wish to get tables from
   * @return list of tables associated with the passed keySpace
   * @apiNote This can return null
   */
  List<String> getTablesByKeySpace(final String keySpace) {
    return this.tables.get(keySpace);
  }

  /** @return Map of tables to list of columns in each table */
  Map<String, List<String>> getColumnsByTable() {
    return this.columns;
  }

  /**
   * @param table table you wish to get columns for
   * @return list of columns associated with the passed table
   * @apiNote This can return null
   */
  List<String> getColumnsByTable(final String table) {
    return this.columns.get(table);
  }

  /**
   * TODO: Remove literal strings
   *
   * <p>There are 3 separate queries that occur in this function.
   *
   * <p>First all the keyspaces are queried Then they are filtered based on if they are related to
   * pathstore or not
   *
   * <p>Second all the tables associated with that key space are queried. No results are filtered
   *
   * <p>Third all the columns associated with the current keyspace and table are queried.
   *
   * <p>{@link #tables} and {@link #columns} are built using this queried information.
   */
  private void loadAllKeySpaceInfo() {
    this.tables = new ConcurrentHashMap<>();
    this.columns = new ConcurrentHashMap<>();

    ResultSet keyspaceQuery = this.session.execute("select * from system_schema.keyspaces");

    for (Row keyspaceRow : keyspaceQuery) {
      String keySpace = keyspaceRow.getString("keyspace_name");

      if (!keySpace.startsWith("pathstore_")) continue;

      ResultSet tableQuery =
          this.session.execute(
              "select table_name from system_schema.tables where keyspace_name='" + keySpace + "'");

      List<String> tablesList = new ArrayList<>();

      for (Row tableRow : tableQuery) {
        String tableName = tableRow.getString("table_name");
        tablesList.add(tableName);

        List<String> columnsList = new ArrayList<>();

        ResultSet columnQuery =
            this.session.execute(
                "select column_name from system_schema.columns where keyspace_name='"
                    + keySpace
                    + "' and table_name='"
                    + tableName
                    + "'");

        for (Row columnRow : columnQuery) columnsList.add(columnRow.getString("column_name"));

        this.columns.put(tableName, columnsList);
      }
      this.tables.put(keySpace, tablesList);
    }
  }
}
