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
package pathstore.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import io.netty.util.internal.ConcurrentSet;
import pathstore.common.logger.PathStoreLogger;
import pathstore.common.logger.PathStoreLoggerFactory;
import pathstore.system.PathStorePriviledgedCluster;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class SchemaInfo {
  private static SchemaInfo instance = null;

  public static synchronized SchemaInfo getInstance() {
    if (SchemaInfo.instance == null)
      SchemaInfo.instance = new SchemaInfo(PathStorePriviledgedCluster.getInstance().connect());
    return SchemaInfo.instance;
  }

  private static final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(SchemaInfo.class);

  private final Set<String> keyspacesLoaded = new ConcurrentSet<>();

  private final ConcurrentMap<String, ConcurrentMap<String, Table>> tableMap =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<Column>>> columnInfo =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<Index>>> indexInfo =
      new ConcurrentHashMap<>();

  private final Session session;

  public SchemaInfo(final Session session) {
    this.session = session;
    this.loadSchemas();
  }

  public void reset() {
    this.columnInfo.clear();
    this.loadSchemas();
  }

  private void loadSchemas() {
    StreamSupport.stream(
            this.session
                .execute(QueryBuilder.select("keyspace_name").from("system_schema", "keyspaces"))
                .spliterator(),
            true)
        .map(row -> row.getString("keyspace_name"))
        .forEach(this::loadKeyspace);
  }

  public void removeKeyspace(final String keyspace) {
    this.keyspacesLoaded.remove(keyspace);
    this.tableMap.remove(keyspace);
    this.columnInfo.remove(keyspace);
    this.indexInfo.remove(keyspace);

    logger.info(String.format("Removed keyspace %s", keyspace));
  }

  public void loadKeyspace(final String keyspace) {
    this.tableMap.put(keyspace, this.loadTableCollectionsForKeyspace(keyspace));
    this.columnInfo.put(keyspace, this.getColumnInfoPerKeyspace(keyspace));
    this.indexInfo.put(keyspace, this.getIndexInfoPerKeyspace(keyspace));
    this.keyspacesLoaded.add(keyspace);

    logger.info(
        String.format(
            "Loaded keyspace %s it has %d tables",
            keyspace, this.tableMap.get(keyspace).values().size()));
  }

  public boolean isKeyspaceLoaded(final String keyspace) {
    return this.keyspacesLoaded.contains(keyspace);
  }

  public Collection<String> getLoadedKeyspaces() {
    return this.keyspacesLoaded;
  }

  public Collection<Table> getTablesFromKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values();
  }

  private ConcurrentMap<String, Table> loadTableCollectionsForKeyspace(final String keyspaceName) {
    return StreamSupport.stream(
            this.session
                .execute(
                    QueryBuilder.select()
                        .all()
                        .from("system_schema", "tables")
                        .where(QueryBuilder.eq("keyspace_name", keyspaceName))
                        .limit(-1))
                .spliterator(),
            true)
        .map(Table::buildFromRow)
        .collect(Collectors.toConcurrentMap(table -> table.table_name, Function.identity()));
  }

  private ConcurrentMap<Table, Collection<Column>> getColumnInfoPerKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values().stream()
        .collect(
            Collectors.toConcurrentMap(
                Function.identity(), table -> Column.buildFromTable(this.session, table)));
  }

  private ConcurrentMap<Table, Collection<Index>> getIndexInfoPerKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values().stream()
        .collect(
            Collectors.toConcurrentMap(
                Function.identity(), table -> Index.buildFromTable(this.session, table)));
  }

  public Collection<Column> getTableColumns(final String keyspace, final String tableName) {
    return Optional.of(
            this.columnInfo.get(keyspace).get(this.tableMap.get(keyspace).get(tableName)))
        .orElse(Collections.emptySet());
  }

  public Collection<Index> getTableIndexes(final String keyspaceName, final String tableName) {
    return Optional.of(
            this.indexInfo.get(keyspaceName).get(this.tableMap.get(keyspaceName).get(tableName)))
        .orElse(Collections.emptySet());
  }

  /**
   * This class represents a row in the system_schema.indexes table.
   *
   * @see SchemaInfo#loadSchemas()
   */
  public static class Index {
    /** keyspace name where the index exists */
    public final String keyspace_name;

    /** Table name where the index exists */
    public final String table_name;

    /** Name of index that was provided at time of creation */
    public final String index_name;

    /** type of index (TODO: What are the options?) */
    public final String kind;

    /**
     * key name is 'target' according to apache docs to get the name of the column where the index
     * is present
     */
    public final Map<String, String> options;

    private Index(
        final String keyspace_name,
        final String table_name,
        final String index_name,
        final String kind,
        final Map<String, String> options) {
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.index_name = index_name;
      this.kind = kind;
      this.options = options;
    }

    public static Collection<Index> buildFromTable(final Session session, final Table table) {
      return StreamSupport.stream(
              session
                  .execute(
                      QueryBuilder.select()
                          .all()
                          .from("system_schema", "indexes")
                          .where(QueryBuilder.eq("keyspace_name", table.keyspace_name))
                          .and(QueryBuilder.eq("table_name", table.table_name))
                          .limit(-1))
                  .spliterator(),
              true)
          .map(Index::buildFromRow)
          .collect(Collectors.toSet());
    }

    // system_schema.indexes
    public static Index buildFromRow(final Row row) {
      return new Index(
          row.getString("keyspace_name"),
          row.getString("table_name"),
          row.getString("index_name"),
          row.getString("kind"),
          row.getMap("options", String.class, String.class));
    }
  }

  public static class Column {
    public final String keyspace_name;
    public final String table_name;
    public final String column_name;
    public final String clustering_order;
    public final String kind;
    public final int position;
    public final String type;

    private Column(
        final String keyspace_name,
        final String table_name,
        final String column_name,
        final String clustering_order,
        final String kind,
        final int position,
        final String type) {
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.column_name = column_name;
      this.clustering_order = clustering_order;
      this.kind = kind;
      this.position = position;
      this.type = type;
    }

    public static Collection<Column> buildFromTable(final Session session, final Table table) {
      return StreamSupport.stream(
              session
                  .execute(
                      QueryBuilder.select()
                          .all()
                          .from("system_schema", "columns")
                          .where(QueryBuilder.eq("keyspace_name", table.keyspace_name))
                          .and(QueryBuilder.eq("table_name", table.table_name))
                          .limit(-1))
                  .spliterator(),
              true)
          .map(Column::buildFromRow)
          .collect(Collectors.toSet());
    }

    // row from system_schema.columns
    private static Column buildFromRow(final Row row) {
      return new Column(
          row.getString("keyspace_name"),
          row.getString("table_name"),
          row.getString("column_name"),
          row.getString("clustering_order"),
          row.getString("kind"),
          row.getInt("position"),
          row.getString("type"));
    }
  }

  public static class Table {
    public final String keyspace_name;
    public final String table_name;
    public final double bloom_filter_fp_chance;
    public final Object caching;
    public final boolean cdc;
    public final String comment;
    public final Object compaction;
    public final Object compression;
    public final double crc_check_chance;
    public final double dclocal_read_repair_chance;
    public final int default_time_to_live;
    public final Object extensions;
    public final Object flags;
    public final int gc_grace_seconds;
    public final UUID id;
    public final int max_index_interval;
    public final int memtable_flush_period_in_ms;
    public final int min_index_interval;
    public final double read_repair_chance;
    public final String speculative_retry;

    private Table(
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

    // row from system_schema.tables
    public static Table buildFromRow(final Row row) {
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
  }
}
