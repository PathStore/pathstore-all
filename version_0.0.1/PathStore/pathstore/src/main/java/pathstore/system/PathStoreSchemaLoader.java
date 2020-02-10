package pathstore.system;

import com.datastax.driver.core.Session;
import pathstore.common.PathStoreProperties;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TODO: Make it so that if a current schema is loaded on a current node it can be deleted
 *
 * <p>Our assumption for this class is that all schema allocations for each node occurs on the root
 * node. We can assume this as in our design we want our "management" decisions to be performed on
 * the root node or at lest all management operations to be directly injected into the root nodes
 * database.
 *
 * <p>This class is one of 3 daemons running alongside pathstore. This daemons sole purpose is to
 * handle the loading and offloading of schemas. This is because we anticipate a system where
 * applications and their corresponding database schemas can be loaded and offloaded.
 *
 * <p>This thread gets started upon startup of {@link PathStoreServerImpl}
 */
public class PathStoreSchemaLoader extends Thread {

  /**
   * This is a simple interface to allow for a double parameter consumer
   *
   * @param <T1> Parameter type 1
   * @param <T2> Parameter type 2
   */
  public interface PathstoreConsumer<T1, T2> {
    /**
     * Function to implement either literally or via lamabda
     *
     * @param t1 parameter 1
     * @param t2 parameter 2
     */
    void function(final T1 t1, final T2 t2);

    /**
     * Function to invoke said function above
     *
     * @param t1 parameter 1
     * @param t2 parameter 2
     */
    default void apply(final T1 t1, final T2 t2) {
      this.function(t1, t2);
    }
  }

  private final PathstoreConsumer<Integer, Set<String>> getNodeInfo;

  private final Consumer<String> getSchema;

  private final Session localSession;

  private final Set<String> schemasToLoad;

  private final Map<String, String> availableSchemas;

  private final Set<String> loadedSchemas;

  public PathStoreSchemaLoader(
      final PathstoreConsumer<Integer, Set<String>> getNodeInfo, final Consumer<String> getSchema) {
    this.getNodeInfo = getNodeInfo;
    this.getSchema = getSchema;
    this.localSession = PathStorePriviledgedCluster.getInstance().connect();
    this.schemasToLoad = new HashSet<>();
    this.availableSchemas = new HashMap<>();
    this.loadedSchemas = new HashSet<>();
  }

  public Map<String, String> getAvailableSchemas() {
    return this.availableSchemas;
  }

  /**
   * This functions runs every second
   *
   * <p>It first queries any node info which resides in pathstore_applications.node_schemas this
   * table represents a node id which is our node identifier and a keyspace_name of the schema it
   * needs. There can be many entries for a singular nodeid. Then we gather all schemas that aren't
   * currently available in our local pathstore_applications.apps table. Then if we haven't already
   * loaded the schema we first parse it and load it directly into our local database then we ensure
   * that we have denoted that schema name in our loadedSchemas set
   */
  @Override
  public void run() {
    while (true) {
      this.getNodeInfo.apply(PathStoreProperties.getInstance().NodeID, this.schemasToLoad);

      for (String s : this.schemasToLoad) {
        if (!this.availableSchemas.containsKey(s)) this.getSchema.accept(s);

        if (!this.loadedSchemas.contains(s)) {
          parseSchema(this.availableSchemas.get(s)).forEach(this.localSession::execute);
          this.loadedSchemas.add(s);
          System.out.println(this.schemasToLoad);
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This is a hardcoded function that allows for loading the base application schema.
   *
   * <p>Its keyspace name is pathstore_applications.
   *
   * @param session database session to execute on
   */
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
            + "    PRIMARY KEY (pathstore_view_id, keyspace_name, pathstore_version)\n"
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
            + "    PRIMARY KEY (keyspace_name, pathstore_version)\n"
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

  /**
   * Simple function to filter out commands when a schema is passed
   *
   * @param schema schema to parse
   * @return list of commands from passed schema
   */
  public static List<String> parseSchema(final String schema) {
    return Arrays.stream(schema.split(";"))
        .map(String::trim)
        .filter(i -> i.length() > 0)
        .collect(Collectors.toList());
  }
}
