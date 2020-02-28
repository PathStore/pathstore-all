package pathstore.system;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.SchemaChangeListener;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreResultSet;
import pathstore.common.PathStoreProperties;
import pathstore.util.SchemaInfo;

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

  /** Set of strings that denote the keyspaces to load */
  private final Set<String> schemasToLoad;

  /** Map of keyspaces and their respective augmented schemas */
  private final Map<String, String> availableSchemas;

  /** Set of strings that denote the keyspaces already loaded into the database */
  private final Set<String> loadedSchemas;

  /** Initialize all hashmaps */
  PathStoreSchemaLoader() {
    this.schemasToLoad = new HashSet<>();
    this.availableSchemas = new HashMap<>();
    this.loadedSchemas = new HashSet<>();
  }

  /** This function is used to recover the state of the internal schemas on restart */
  void recover() {
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
    for (String keyspace : this.schemasToLoad)
      for (Row row :
          new PathStoreResultSet(
              PathStorePriviledgedCluster.getInstance()
                  .connect()
                  .execute(QueryBuilder.select().all().from("pathstore_applications", "apps")),
              "pathstore_applications",
              "apps")) this.availableSchemas.put(keyspace, row.getString("augmented_schema"));

    System.out.println("Recovered available schemas: " + this.availableSchemas);

    // Load values into loadedSchemas
    for (Row row :
        PathStorePriviledgedCluster.getInstance()
            .connect()
            .execute(QueryBuilder.select("keyspace_name").from("system_schema", "keyspaces"))) {
      String keyspace = row.getString("keyspace_name");
      if (keyspace.startsWith("pathstore_") && !keyspace.equals("pathstore_applications"))
        this.loadedSchemas.add(keyspace);
    }

    System.out.println("Recovered loaded schemas: " + this.loadedSchemas);
  }

  /**
   * This function loads schemas specifically related to this node, this acts as a pathstore client
   * and will move up the node hierarchy. Adds all {@link #schemasToLoad}
   *
   * <p>TODO: Make this function a 1 time call, implement immutable querycache entries
   *
   * @param session {@link PathStoreCluster#connect()}
   */
  private void load_node_related_schemas(final Session session) {
    Select select = QueryBuilder.select().all().from("pathstore_applications", "node_schemas");
    select.where(QueryBuilder.eq("nodeid", PathStoreProperties.getInstance().NodeID));

    for (Row row : session.execute(select)) this.schemasToLoad.add(row.getString("keyspace_name"));
  }

  /**
   * This function loads all needed schemas for itself and its children nodes
   *
   * @return a set of strings denoting all schemas needed to load this is used to calculate the
   *     difference between {@link #schemasToLoad}
   * @see #calculate_difference(Set)
   */
  private Set<String> load_path_related_schemas() {
    Set<String> comparison_set = new HashSet<>();

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

    return comparison_set;
  }

  /**
   * This function calculates a list of differences between what schemas are required to be loaded
   * and what schemas are actually loaded. If a schema is loaded but is no longer needed it is added
   * to the difference list
   *
   * @param current_keyspaces set of current keyspaces
   * @return list of difference
   * @see #remove_all_difference(List)
   */
  private List<String> calculate_difference(final Set<String> current_keyspaces) {
    List<String> differences = new LinkedList<>();

    for (String s : this.schemasToLoad) if (!current_keyspaces.contains(s)) differences.add(s);

    return differences;
  }

  /**
   * This function takes in a list of differences that was calculated {@link
   * #calculate_difference(Set)} removes all keyspaces that are no longer required
   *
   * <p>TODO: Needs to delete all querycache entries from current node that are related to each
   * keyspace
   *
   * @param differences list of differences
   */
  private void remove_all_difference(final List<String> differences) {
    for (String keyspace : differences) {
      if (this.loadedSchemas.contains(keyspace)) {
        SchemaInfo.getInstance().removeKeyspace(keyspace);
        PathStorePriviledgedCluster.getInstance()
            .connect()
            .execute("drop keyspace if exists " + keyspace);
        this.loadedSchemas.remove(keyspace);
      }
      this.schemasToLoad.remove(keyspace);
    }
  }

  /**
   * This function loads a required augmented schema if not already present in {@link
   * #availableSchemas}
   *
   * @param session {@link PathStoreCluster#connect()}
   * @param keyspace keyspace to load
   */
  private void load_required_augmented_schemas(final Session session, final String keyspace) {

    Select select1 = QueryBuilder.select().all().from("pathstore_applications", "apps");
    select1.where(QueryBuilder.eq("keyspace_name", keyspace));

    for (Row row : session.execute(select1)) {
      this.availableSchemas.put(keyspace, row.getString("augmented_schema"));
    }
  }

  /**
   * Load schema from {@link #availableSchemas} and execute all commands on the database
   *
   * @param keyspace keyspace to load into database
   */
  private void load_schema_into_database(final String keyspace) {
    if (!this.loadedSchemas.contains(keyspace)) {
      SchemaInfo info = SchemaInfo.getInstance();
      info.removeKeyspace(keyspace);
      parseSchema(this.availableSchemas.get(keyspace))
          .forEach(PathStorePriviledgedCluster.getInstance().connect()::execute);
      this.loadedSchemas.add(keyspace);
      info.getKeySpaceInfo(keyspace);
    }
  }

  /**
   * Load schemas for this node
   *
   * <p>Remove all differences
   *
   * <p>load schemas not already loaded into database
   */
  @Override
  public void run() {
    while (true) {
      Session session = PathStoreCluster.getInstance().connect();

      this.load_node_related_schemas(session);

      this.remove_all_difference(this.calculate_difference(this.load_path_related_schemas()));

      for (String keyspace : this.schemasToLoad)
        if (!this.availableSchemas.containsKey(keyspace))
          this.load_required_augmented_schemas(session, keyspace);
        else this.load_schema_into_database(keyspace);

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
            + "    process_status text,\n"
            + "    wait_for int,\n"
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
