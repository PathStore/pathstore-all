package pathstore.system;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO: First load all schemas into database.
 *
 * <p>TODO: Then make it so that you only load in the schemas based on the node_schemas table
 */
public class PathStoreSchemaLoader extends Thread {

  private static PathStoreSchemaLoader schemaLoader = null;

  public static PathStoreSchemaLoader getInstance() {
    return schemaLoader == null ? new PathStoreSchemaLoader() : schemaLoader;
  }

  private final Session localSession;

  private final Set<String> schemasToLoad;

  private PathStoreSchemaLoader() {
    this.localSession = PathStorePriviledgedCluster.getInstance().connect();
    this.schemasToLoad = new HashSet<>();
    QueryCache.getInstance()
        .updateCache(
            "pathstore_applications",
            "node_schemas",
            Collections.singletonList(
                QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID)),
            1);
  }

  @Override
  public void run() {
    while (true) {
      System.out.println("Running");
      ResultSet schemas =
          this.localSession.execute(
              "select * from pathstore_applications.node_schemas where nodeid="
                  + PathStoreProperties.getInstance().NodeID);

      for (Row row : schemas) {
        this.schemasToLoad.add(row.getString("keyspace_name"));
      }

      for (String s : this.schemasToLoad) {
        System.out.println(s);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void loadApplicationSchema(final Session session) {
    String schema =
        "CREATE KEYSPACE pathstore_applications WITH REPLICATION = { 'class' : 'org.apache.cassandra.locator.SimpleStrategy', 'replication_factor': '1' } AND DURABLE_WRITES = false;\n"
            + "\n"
            + "CREATE TABLE pathstore_applications.view_node_schemas (\n"
            + "    pathstore_view_id uuid,\n"
            + "    nodeid int,\n"
            + "    pathstore_version timeuuid,\n"
            + "    keyspace_name text,\n"
            + "    pathstore_deleted boolean,\n"
            + "    pathstore_dirty boolean,\n"
            + "    pathstore_node int,\n"
            + "    pathstore_parent_timestamp timeuuid,\n"
            + "    PRIMARY KEY (pathstore_view_id, nodeid, pathstore_version)\n"
            + ") WITH read_repair_chance = 0.0\n"
            + "   AND dclocal_read_repair_chance = 0.1\n"
            + "   AND gc_grace_seconds = 864000\n"
            + "   AND bloom_filter_fp_chance = 0.01\n"
            + "   AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
            + "   AND comment = ''\n"
            + "   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
            + "   AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
            + "   AND default_time_to_live = 0\n"
            + "   AND speculative_retry = '99PERCENTILE'\n"
            + "   AND min_index_interval = 128\n"
            + "   AND max_index_interval = 2048\n"
            + "   AND crc_check_chance = 1.0;\n"
            + "\n"
            + "CREATE TABLE pathstore_applications.node_schemas (\n"
            + "    nodeid int,\n"
            + "    pathstore_version timeuuid,\n"
            + "    keyspace_name text,\n"
            + "    pathstore_deleted boolean,\n"
            + "    pathstore_dirty boolean,\n"
            + "    pathstore_insert_sid text,\n"
            + "    pathstore_node int,\n"
            + "    pathstore_parent_timestamp timeuuid,\n"
            + "    PRIMARY KEY (nodeid, pathstore_version)\n"
            + ") WITH CLUSTERING ORDER BY (pathstore_version DESC)\n"
            + "   AND read_repair_chance = 0.0\n"
            + "   AND dclocal_read_repair_chance = 0.0\n"
            + "   AND gc_grace_seconds = 604800\n"
            + "   AND bloom_filter_fp_chance = 0.01\n"
            + "   AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
            + "   AND comment = 'table definitions'\n"
            + "   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
            + "   AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
            + "   AND default_time_to_live = 0\n"
            + "   AND speculative_retry = '99PERCENTILE'\n"
            + "   AND min_index_interval = 128\n"
            + "   AND max_index_interval = 2048\n"
            + "   AND crc_check_chance = 1.0;\n"
            + "CREATE INDEX node_schemas_pathstore_deleted_idx ON pathstore_applications.node_schemas (pathstore_deleted);\n"
            + "CREATE INDEX node_schemas_pathstore_dirty_idx ON pathstore_applications.node_schemas (pathstore_dirty);\n"
            + "CREATE INDEX node_schemas_pathstore_insert_sid_idx ON pathstore_applications.node_schemas (pathstore_insert_sid);\n"
            + "\n"
            + "CREATE TABLE pathstore_applications.view_apps (\n"
            + "    pathstore_view_id uuid,\n"
            + "    appid int,\n"
            + "    pathstore_version timeuuid,\n"
            + "    augmented_schema text,\n"
            + "    keyspace_name text,\n"
            + "    pathstore_deleted boolean,\n"
            + "    pathstore_dirty boolean,\n"
            + "    pathstore_node int,\n"
            + "    pathstore_parent_timestamp timeuuid,\n"
            + "    PRIMARY KEY (pathstore_view_id, appid, pathstore_version)\n"
            + ") WITH read_repair_chance = 0.0\n"
            + "   AND dclocal_read_repair_chance = 0.1\n"
            + "   AND gc_grace_seconds = 864000\n"
            + "   AND bloom_filter_fp_chance = 0.01\n"
            + "   AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
            + "   AND comment = ''\n"
            + "   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
            + "   AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
            + "   AND default_time_to_live = 0\n"
            + "   AND speculative_retry = '99PERCENTILE'\n"
            + "   AND min_index_interval = 128\n"
            + "   AND max_index_interval = 2048\n"
            + "   AND crc_check_chance = 1.0;\n"
            + "\n"
            + "CREATE TABLE pathstore_applications.apps (\n"
            + "    appid int,\n"
            + "    pathstore_version timeuuid,\n"
            + "    augmented_schema text,\n"
            + "    keyspace_name text,\n"
            + "    pathstore_deleted boolean,\n"
            + "    pathstore_dirty boolean,\n"
            + "    pathstore_insert_sid text,\n"
            + "    pathstore_node int,\n"
            + "    pathstore_parent_timestamp timeuuid,\n"
            + "    PRIMARY KEY (appid, pathstore_version)\n"
            + ") WITH CLUSTERING ORDER BY (pathstore_version DESC)\n"
            + "   AND read_repair_chance = 0.0\n"
            + "   AND dclocal_read_repair_chance = 0.0\n"
            + "   AND gc_grace_seconds = 604800\n"
            + "   AND bloom_filter_fp_chance = 0.01\n"
            + "   AND caching = { 'keys' : 'ALL', 'rows_per_partition' : 'NONE' }\n"
            + "   AND comment = 'table definitions'\n"
            + "   AND compaction = { 'class' : 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold' : 32, 'min_threshold' : 4 }\n"
            + "   AND compression = { 'chunk_length_in_kb' : 64, 'class' : 'org.apache.cassandra.io.compress.LZ4Compressor' }\n"
            + "   AND default_time_to_live = 0\n"
            + "   AND speculative_retry = '99PERCENTILE'\n"
            + "   AND min_index_interval = 128\n"
            + "   AND max_index_interval = 2048\n"
            + "   AND crc_check_chance = 1.0;\n"
            + "CREATE INDEX apps_pathstore_deleted_idx ON pathstore_applications.apps (pathstore_deleted);\n"
            + "CREATE INDEX apps_pathstore_dirty_idx ON pathstore_applications.apps (pathstore_dirty);\n"
            + "CREATE INDEX apps_pathstore_insert_sid_idx ON pathstore_applications.apps (pathstore_insert_sid);";

    parseSchema(schema).forEach(session::execute);
  }

  public static List<String> parseSchema(final String schema) {
    return Arrays.stream(schema.split(";"))
        .map(String::trim)
        .filter(i -> i.length() > 0)
        .collect(Collectors.toList());
  }
}
