package pathstore.util;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Refactor the way data is stored for a more optimal dataset
 *
 * <p>This class is used to query information about the current state of a database. It allows you
 * to get a list of tables based on keyspace, list of columns based on tables. You can also get the
 * entire map that contains all pathstore related tables and columns
 */
public class SchemaInfoV2 {
  /** Pass via the constructor. Used to communicate with database */
  private final Session session;

  /** List of tables associated with a keyspace */
  private Map<String, List<String>> keyspaceTables;

  /** List of columns associated with a table */
  private Map<String, List<String>> tableColumns;

  /** Table name associated to table object */
  private Map<String, Table> tables;

  /** Column name associated to column object */
  private Map<String, Column> columns;

  /**
   * @param session Current cassandra database session. Called is required to close this connection
   */
  public SchemaInfoV2(final Session session) {
    this.session = session;
    generate();
  }

  /** Allows you to regenerate this data if a change to the databases schemas has changed. */
  void generate() {
    loadAllKeySpaceInfo();
  }

  /** @return Set of all keyspaces */
  public Set<String> getAllKeySpaces() {
    return this.keyspaceTables.keySet();
  }

  /** @return Set of all tables */
  Set<String> getAllTables() {
    return this.tableColumns.keySet();
  }

  /** @return Map of Keyspaces to list of tables */
  Map<String, List<String>> getTablesByKeySpace() {
    return this.keyspaceTables;
  }

  /**
   * @param keySpace key space you wish to get tables from
   * @return list of tables associated with the passed keySpace
   * @apiNote This can return null
   */
  List<String> getTablesByKeySpace(final String keySpace) {
    return this.keyspaceTables.get(keySpace);
  }

  /** @return Map of tables to list of columns in each table */
  Map<String, List<String>> getColumnsByTable() {
    return this.tableColumns;
  }

  /** @return Map of table names to a object with all their info */
  Map<String, Table> getTableObjects() {
    return this.tables;
  }

  /** @return Map of column names to a object with all their info */
  Map<String, Column> getColumnObjects() {
    return this.columns;
  }

  Table getTableObjectByName(final String tableName) {
    return this.tables.get(tableName);
  }

  Column getColumnObjectByName(final String columnName) {
    return this.columns.get(columnName);
  }

  /**
   * @param table table you wish to get columns for
   * @return list of columns associated with the passed table
   * @apiNote This can return null
   */
  List<String> getColumnsByTable(final String table) {
    return this.tableColumns.get(table);
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
   * <p>{@link #keyspaceTables} and {@link #tableColumns} are built using this queried information.
   */
  private void loadAllKeySpaceInfo() {
    this.keyspaceTables = new ConcurrentHashMap<>();
    this.tableColumns = new ConcurrentHashMap<>();
    this.tables = new ConcurrentHashMap<>();
    this.columns = new ConcurrentHashMap<>();

    ResultSet keyspaceQuery = this.session.execute("select * from system_schema.keyspaces");

    for (Row keyspaceRow : keyspaceQuery) {
      String keySpace = keyspaceRow.getString("keyspace_name");

      if (!keySpace.startsWith("pathstore_")) continue;

      ResultSet tableQuery =
          this.session.execute(
              "select * from system_schema.tables where keyspace_name='" + keySpace + "'");

      List<String> tablesList = new ArrayList<>();

      for (Row tableRow : tableQuery) {
        Table table = parseTableRow(tableRow);
        tablesList.add(table.table_name);

        List<String> columnsList = new ArrayList<>();

        ResultSet columnQuery =
            this.session.execute(
                "select * from system_schema.columns where keyspace_name='"
                    + keySpace
                    + "' and table_name='"
                    + table.table_name
                    + "'");

        for (Row columnRow : columnQuery) {
          Column column = parseColumnRow(columnRow);
          columnsList.add(column.column_name);
          this.columns.put(column.column_name, column);
        }

        this.tables.put(table.table_name, table);
        this.tableColumns.put(table.table_name, columnsList);
      }
      this.keyspaceTables.put(keySpace, tablesList);
    }
  }

  /**
   * TODO: Remove string literals
   *
   * <p>Parses a row from the system_schema keyspace and the tables table
   *
   * @param row row to parse
   * @return initialized table
   */
  private Table parseTableRow(final Row row) {
    return new Table(
        row.getString("keyspace_name"),
        row.getString("table_name"),
        row.getDouble("bloom_filter_fp_chance"),
        row.getObject("caching"),
        row.getBool("cdc"),
        row.getString("comment"),
        row.getObject("compaction"),
        row.getObject("compression"),
        row.getDouble("crc_check_chance"),
        row.getDouble("dclocal_read_repair_chance"),
        row.getInt("default_time_to_live"),
        row.getObject("extensions"),
        row.getObject("flags"),
        row.getInt("gc_grace_seconds"),
        row.getUUID("id"),
        row.getInt("max_index_interval"),
        row.getInt("memtable_flush_period_in_ms"),
        row.getInt("min_index_interval"),
        row.getDouble("read_repair_chance"),
        row.getString("speculative_retry"));
  }

  /**
   * TODO: Remove string literals
   *
   * <p>Parses a row from the system_schema keyspace and the columns table
   *
   * @param row row to parse
   * @return initialized column object
   */
  private Column parseColumnRow(final Row row) {
    return new Column(
        row.getString("clustering_order"),
        row.getString("column_name"),
        row.getString("keyspace_name"),
        row.getString("kind"),
        row.getInt("position"),
        row.getString("table_name"),
        row.getString("type"));
  }

  /**
   * Stores every piece of data that is queried when reading through a table associated with a
   * keyspace.
   *
   * <p>Everything is named the same way as in cassandra
   */
  public static class Table {
    public final String keyspace_name, table_name, comment, speculative_retry;
    public final double bloom_filter_fp_chance,
        crc_check_chance,
        dclocal_read_repair_chance,
        read_repair_chance;
    public final int default_time_to_live,
        gc_grace_seconds,
        max_index_interval,
        memtable_flush_period_in_ms,
        min_index_interval;
    public final Object caching, compaction, compression, extensions, flags;
    public final boolean cdc;
    public final UUID id;

    Table(
        final String keyspace_name,
        final String table_name,
        final double bloom_filter_fp_chance,
        final Object caching,
        final boolean cdc,
        final String comment,
        final Object compaction,
        final Object compression,
        final double crc_check_chance,
        final double dclocal_read_repair_chance,
        final int default_time_to_live,
        final Object extensions,
        final Object flags,
        final int gc_grace_seconds,
        final UUID id,
        final int max_index_interval,
        final int memtable_flush_period_in_ms,
        final int min_index_interval,
        final double read_repair_chance,
        final String speculative_retry) {

      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.bloom_filter_fp_chance = bloom_filter_fp_chance;
      this.caching = caching;
      this.cdc = cdc;
      this.comment = comment;
      this.compaction = compaction;
      this.compression = compression;
      this.crc_check_chance = crc_check_chance;
      this.dclocal_read_repair_chance = dclocal_read_repair_chance;
      this.default_time_to_live = default_time_to_live;
      this.extensions = extensions;
      this.flags = flags;
      this.gc_grace_seconds = gc_grace_seconds;
      this.id = id;
      this.max_index_interval = max_index_interval;
      this.memtable_flush_period_in_ms = memtable_flush_period_in_ms;
      this.min_index_interval = min_index_interval;
      this.read_repair_chance = read_repair_chance;
      this.speculative_retry = speculative_retry;
    }
  }

  /**
   * TODO: Remove string literals
   *
   * <p>Stores all data queried when selecting a column from a table from a keyspace in cassandra
   *
   * <p>Everything is named the same way as in cassandra
   */
  public static class Column {
    public final String keyspace_name, table_name, column_name, type, clustering_order, kind;
    public final int position;

    Column(
        final String clustering_order,
        final String column_name,
        final String keyspace_name,
        final String kind,
        final int position,
        final String table_name,
        final String type) {
      this.clustering_order = clustering_order;
      this.column_name = column_name;
      this.keyspace_name = keyspace_name;
      this.kind = kind;
      this.position = position;
      this.table_name = table_name;
      this.type = type;
    }
  }
}
