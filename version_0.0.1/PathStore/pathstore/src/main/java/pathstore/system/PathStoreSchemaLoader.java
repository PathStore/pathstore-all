package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreResultSet;
import pathstore.common.PathStoreProperties;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Our assumption for this class is that all schema allocations for each node occurs on the root
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

  private final Set<String> schemasToLoad;

  private final Map<String, String> availableSchemas;

  private final Set<String> loadedSchemas;

  public PathStoreSchemaLoader() {
    this.schemasToLoad = new HashSet<>();
    this.availableSchemas = new HashMap<>();
    this.loadedSchemas = new HashSet<>();
  }

  /** This function is used to recover the state of the internal schemas on restart */
  public void recover() {
    // TODO: (1)
    // Load values into schemasToLoad
    for (Row row :
        new PathStoreResultSet(
            PathStorePriviledgedCluster.getInstance()
                .connect()
                .execute(
                    QueryBuilder.select().all().from("pathstore_applications", "node_schemas")),
            "pathstore_applications",
            "node_schemas")) this.schemasToLoad.add(row.getString("keyspace_name"));

    System.out.println("Recovered schemas to load: " + this.schemasToLoad);

    // Load values into availableSchemas
    for (String keyspace : this.schemasToLoad) {
      for (Row row :
          new PathStoreResultSet(
              PathStorePriviledgedCluster.getInstance()
                  .connect()
                  .execute(QueryBuilder.select().all().from("pathstore_applications", "apps")),
              "pathstore_applications",
              "apps")) {
        this.availableSchemas.put(keyspace, row.getString("augmented_schema"));
      }
    }

    System.out.println("Recovered available schemas: " + this.availableSchemas);

    // Load values into loadedSchemas
    for (Row row :
        new PathStoreResultSet(
            PathStorePriviledgedCluster.getInstance()
                .connect()
                .execute(QueryBuilder.select("keyspace_name").from("system_schema", "keyspaces")),
            "system_schema",
            "keyspaces")) {
      this.loadedSchemas.add(row.getString("keyspace_name"));
    }

    System.out.println("Recovered loaded schemas: " + this.loadedSchemas);
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
      Session session = PathStoreCluster.getInstance().connect();

      // Load schemas dependent on the this specific node
      Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
      select.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));

      for (Row row : session.execute(select))
        this.schemasToLoad.add(row.getString("keyspace_name"));

      Set<String> comparison_set = new HashSet<>();

      // Load schemas that this nodes child need. So the path from root to edge node is complete
      for (Row row :
          new PathStoreResultSet(
              PathStorePriviledgedCluster.getInstance()
                  .connect()
                  .execute(
                      QueryBuilder.select().all().from("pathstore_applications", "node_schemas")),
              "pathstore_applications",
              "node_schemas")) {
        String keyspace = row.getString("keyspace_name");
        this.schemasToLoad.add(keyspace);
        comparison_set.add(keyspace);
      }

      this.schemasToLoad.removeAll(comparison_set);

      for (String keyspace : this.schemasToLoad)
        PathStorePriviledgedCluster.getInstance()
            .connect()
            .execute("drop keyspace if exists" + keyspace);

      for (String keyspace : comparison_set) {
        if (!this.availableSchemas.containsKey(keyspace)) {

          Select select1 = QueryBuilder.select().all().from("pathstore_applications", "apps");
          select1.where(QueryBuilder.eq("keyspace_name", keyspace));

          for (Row row : session.execute(select1)) {
            this.availableSchemas.put(keyspace, row.getString("augmented_schema"));
          }
        } else {
          if (!this.loadedSchemas.contains(keyspace)) {
            parseSchema(this.availableSchemas.get(keyspace))
                .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);
            this.loadedSchemas.add(keyspace);
          }
        }
      }

      this.schemasToLoad.clear();

      try {
        // TODO: Find some more optimal timing for this
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
