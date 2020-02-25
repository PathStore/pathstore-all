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

import pathstore.exception.InvalidKeyspaceException;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.util.SchemaInfo.Column;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;

/** TODO: Comment */
public class SchemaInfo {
  private static SchemaInfo instance = null;

  public static SchemaInfo getInstance() {
    if (SchemaInfo.instance == null) SchemaInfo.instance = new SchemaInfo();
    return SchemaInfo.instance;
  }

  // Hossein
  public Metadata getMetadataForKeySpaceTable(String keyspace, String table) {
    return PathStorePriviledgedCluster.getInstance().getMetadata();
  }

  final Map<String, Map<Table, List<Column>>> schemaInfo = new ConcurrentHashMap<>();

  public SchemaInfo() {
    loadSchemas();
  }

  public Map<String, Map<Table, List<Column>>> getSchemaInfo() {
    return schemaInfo;
  }

  private void loadSchemas() {
    PathStorePriviledgedCluster cluster = PathStorePriviledgedCluster.getInstance();
    Session session = cluster.connect();

    try {
      String query = "select * from system_schema.keyspaces";
      ResultSet results = session.execute(query);

      for (Row row : results) {
        String keyspace_name = row.getString("keyspace_name");
        if (keyspace_name.startsWith("pathstore_")) getKeySpaceInfo(keyspace_name);
      }

    } finally {
      // session.close();
    }
  }

  public void removeKeyspace(final String keyspace) {
    synchronized (this.schemaInfo) {
      this.schemaInfo.remove(keyspace);
    }
  }

  public Map<Table, List<Column>> getKeySpaceInfo(String keyspaceName) {
    if (schemaInfo.get(keyspaceName) != null) return schemaInfo.get(keyspaceName);

    PathStorePriviledgedCluster cluster = PathStorePriviledgedCluster.getInstance();
    Session session = cluster.connect();

    Map<Table, List<Column>> keyspaceInfo = new ConcurrentHashMap<>();
    schemaInfo.put(keyspaceName, keyspaceInfo);

    PreparedStatement prepared =
        session.prepare("select * from system_schema.tables where keyspace_name=?");

    BoundStatement bound = prepared.bind(keyspaceName);
    ResultSet results = session.execute(bound);

    List<Column> columns = null;

    for (Row row : results) {
      Table table = Table.buildFromRow(row);
      columns = new ArrayList<Column>();
      keyspaceInfo.put(table, columns);
    }

    prepared = session.prepare("select * from system_schema.columns where keyspace_name=?");

    bound = prepared.bind(keyspaceName);
    results = session.execute(bound);

    for (Row row : results) {
      String keyspace_name = row.getString("keyspace_name");
      String table_name = row.getString("table_name");
      String column_name = row.getString("column_name");
      String clustering_order = row.getString("clustering_order");
      String kind = row.getString("kind");
      int position = row.getInt("position");
      String type = row.getString("type");

      Column column =
          new Column(
              keyspace_name, table_name, column_name, clustering_order, kind, position, type);

      columns = getColumns(keyspaceInfo, table_name);

      columns.add(column);
    }

    return keyspaceInfo;
  }

  private List<Column> getColumns(Map<Table, List<Column>> keyspaceInfo, String tableName) {
    for (Table t : keyspaceInfo.keySet()) {
      if (t.table_name.compareTo(tableName) == 0) return keyspaceInfo.get(t);
    }
    return null;
  }

  public List<Column> getTableColumns(String keyspace, String table) {
    return getColumns(getKeySpaceInfo(keyspace), table);
  }

  public static class Column {
    public String keyspace_name;
    public String table_name;
    public String column_name;
    public String clustering_order;
    public String kind;
    public int position;
    public String type;

    public Column(
        String keyspace_name,
        String table_name,
        String column_name,
        String clustering_order,
        String kind,
        int position,
        String type) {
      super();
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.column_name = column_name;
      this.clustering_order = clustering_order;
      this.kind = kind;
      this.position = position;
      this.type = type;
    }
  }

  public static class Table {
    String keyspace_name;
    String table_name;
    double bloom_filter_fp_chance;
    Object caching;
    boolean cdc;
    String comment;
    Object compaction;
    Object compression;
    double crc_check_chance;
    double dclocal_read_repair_chance;
    int default_time_to_live;
    Object extensions;
    Object flags;
    int gc_grace_seconds;
    UUID id;
    int max_index_interval;
    int memtable_flush_period_in_ms;
    int min_index_interval;
    double read_repair_chance;
    String speculative_retry;

    public String getKeyspace_name() {
      return keyspace_name;
    }

    public void setKeyspace_name(String keyspace_name) {
      this.keyspace_name = keyspace_name;
    }

    public String getTable_name() {
      return table_name;
    }

    public void setTable_name(String table_name) {
      this.table_name = table_name;
    }

    public double getBloom_filter_fp_chance() {
      return bloom_filter_fp_chance;
    }

    public void setBloom_filter_fp_chance(double bloom_filter_fp_chance) {
      this.bloom_filter_fp_chance = bloom_filter_fp_chance;
    }

    public Object getCaching() {
      return caching;
    }

    public void setCaching(Object caching) {
      this.caching = caching;
    }

    public boolean isCdc() {
      return cdc;
    }

    public void setCdc(boolean cdc) {
      this.cdc = cdc;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    public Object getCompaction() {
      return compaction;
    }

    public void setCompaction(Object compaction) {
      this.compaction = compaction;
    }

    public Object getCompression() {
      return compression;
    }

    public void setCompression(Object compression) {
      this.compression = compression;
    }

    public double getCrc_check_chance() {
      return crc_check_chance;
    }

    public void setCrc_check_chance(double crc_check_chance) {
      this.crc_check_chance = crc_check_chance;
    }

    public double getDclocal_read_repair_chance() {
      return dclocal_read_repair_chance;
    }

    public void setDclocal_read_repair_chance(double dclocal_read_repair_chance) {
      this.dclocal_read_repair_chance = dclocal_read_repair_chance;
    }

    public int getDefault_time_to_live() {
      return default_time_to_live;
    }

    public void setDefault_time_to_live(int default_time_to_live) {
      this.default_time_to_live = default_time_to_live;
    }

    public Object getExtensions() {
      return extensions;
    }

    public void setExtensions(Object extensions) {
      this.extensions = extensions;
    }

    public Object getFlags() {
      return flags;
    }

    public void setFlags(Object flags) {
      this.flags = flags;
    }

    public int getGc_grace_seconds() {
      return gc_grace_seconds;
    }

    public void setGc_grace_seconds(int gc_grace_seconds) {
      this.gc_grace_seconds = gc_grace_seconds;
    }

    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public int getMax_index_interval() {
      return max_index_interval;
    }

    public void setMax_index_interval(int max_index_interval) {
      this.max_index_interval = max_index_interval;
    }

    public int getMemtable_flush_period_in_ms() {
      return memtable_flush_period_in_ms;
    }

    public void setMemtable_flush_period_in_ms(int memtable_flush_period_in_ms) {
      this.memtable_flush_period_in_ms = memtable_flush_period_in_ms;
    }

    public int getMin_index_interval() {
      return min_index_interval;
    }

    public void setMin_index_interval(int min_index_interval) {
      this.min_index_interval = min_index_interval;
    }

    public double getRead_repair_chance() {
      return read_repair_chance;
    }

    public void setRead_repair_chance(double read_repair_chance) {
      this.read_repair_chance = read_repair_chance;
    }

    public String getSpeculative_retry() {
      return speculative_retry;
    }

    public void setSpeculative_retry(String speculative_retry) {
      this.speculative_retry = speculative_retry;
    }

    static Table buildFromRow(Row row) {
      Table t = new Table();

      t.keyspace_name = row.getString("keyspace_name");
      t.table_name = row.getString("table_name");
      t.bloom_filter_fp_chance = row.getDouble("bloom_filter_fp_chance");
      t.caching = row.getObject("caching");
      t.cdc = row.getBool("cdc");
      t.comment = row.getString("comment");
      t.compaction = row.getObject("compaction");
      t.compression = row.getObject("compression");
      t.crc_check_chance = row.getDouble("crc_check_chance");
      t.dclocal_read_repair_chance = row.getDouble("dclocal_read_repair_chance");
      t.default_time_to_live = row.getInt("default_time_to_live");
      t.extensions = row.getObject("extensions");
      t.flags = row.getObject("flags");
      t.gc_grace_seconds = row.getInt("gc_grace_seconds");
      t.id = row.getUUID("id");
      t.max_index_interval = row.getInt("max_index_interval");
      t.memtable_flush_period_in_ms = row.getInt("memtable_flush_period_in_ms");
      t.min_index_interval = row.getInt("min_index_interval");
      t.read_repair_chance = row.getDouble("read_repair_chance");
      t.speculative_retry = row.getString("speculative_retry");

      return t;
    }
  }
}
