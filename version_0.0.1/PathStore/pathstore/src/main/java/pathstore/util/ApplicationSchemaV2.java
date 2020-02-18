package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.PathStoreProperties;
import pathstore.system.PathStoreSchemaLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * TODO: Should we augment the pathstore_applications keyspace. If not fix push server to not check
 * that keyspace
 *
 * <p>Objective of this class is to write the current application schemas to a table in the root
 * server's database Then when a child is started up the first thing it does is it checks it's
 * parent node to see if it has the same schema's. If it doesn't then it pulls it's parents schema's
 * and updates it's current databases. During this time there needs to be a blocker on the session
 * to basically say that if a current request is on the keyspace of a currently updated table wait
 * until that operation is complete. We also need to check to see if you can update a schema while
 * their is data inside the schema.
 *
 * <p>Current Plan of implementation:
 *
 * <p>1. Write function to drop schema of a certain name. 2. Write function to load
 * pathstore_application schema. 3. Insert the pathstore_application schema into
 * pathstore_application.apps. This will only be used by other pathstore nodes 4. Modify {@link
 * pathstore.system.PathStoreServerImpl} to pull schema from parent server if it doesn't exist as
 * this will never change while the pathstore network is alive (I think.) 5. Write function to
 * augment user defined schemas. 6. Write function to load augment user defined shcemas into
 * pathstore_application.apps 7: Modify {@link pathstore.system.PathStoreServerImpl} and {@link
 * pathstore.system.PathStorePullServer} to pull schema from parent server. 8: Deal with potential
 * use cases that
 */
public class ApplicationSchemaV2 {

  /**
   * Constants class to represent all the sql strings / column names of tables that are modified
   * throughout this class TODO: Potential move to a global locations\ TODO: Write loader to load in
   * cql files instead of hard coded tables
   */
  private static class Constants {
    public static final class PathStoreApplications {
      public static final String KEYSPACE = "pathstore_applications";

      public static final class Apps {
        public static final String TABLE = "apps";
        public static final String APPID = "appid";
        public static final String KEYSPACE = "keyspace_name";
        public static final String AUGMENTED_SCHEMA = "augmented_schema";
      }
    }
  }

  /** Stored to gather meta data of keyspaces */
  private final Cluster cluster;

  /**
   * TODO: Move to {@link pathstore.system.PathStorePriviledgedCluster}
   *
   * <p>Local session to local database
   */
  private final Session session;

  /** Stores info based on the schema's we are loading in */
  private final SchemaInfoV2 schemaInfo;

  /**
   * TODO: Change cluster to {@link pathstore.system.PathStorePriviledgedCluster}
   *
   * <p>TODO: Document command usage
   *
   * <p>Executes load schemas based on commands
   *
   * @param args commands to execute
   */
  ApplicationSchemaV2(final String[] args) {
    this.cluster =
        new Cluster.Builder()
            .addContactPoints(PathStoreProperties.getInstance().CassandraIP)
            .withPort(PathStoreProperties.getInstance().CassandraPort)
            .withSocketOptions(
                new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
            .withQueryOptions(
                new QueryOptions()
                    .setRefreshNodeIntervalMillis(0)
                    .setRefreshNodeListIntervalMillis(0)
                    .setRefreshSchemaIntervalMillis(0))
            .build();
    this.session = this.cluster.connect();

    this.schemaInfo = new SchemaInfoV2(this.session);

    switch (args[0]) {
      case "init":
        for (String s : this.schemaInfo.getAllKeySpaces()) {
          dropKeySpace(s);
        }
        this.loadSchemas(0, Arrays.copyOfRange(args, 1, args.length));
        break;
      case "import":
        ResultSet set =
            this.session.execute("select max(appid) from pathstore_applications.apps limit 1");
        this.loadSchemas(
            set != null
                ? set.one().getInt("system.max(" + Constants.PathStoreApplications.Apps.APPID + ")")
                    + 1
                : 0,
            Arrays.copyOfRange(args, 1, args.length));
        break;
      default:
        System.out.println(
            "Unknown Operation, the valid operations are init, and import, followed by the cql schemas you wish to import");
        System.exit(1);
        break;
    }

    this.session.close();
    this.cluster.close();
  }

  /**
   * Loads schemas into the database
   *
   * <p>First it parses the file and executes all their commands.
   *
   * <p>Then the {@link SchemaInfoV2} is updated to the latest version of the schemas
   *
   * <p>Then if the keyspace needs augmentation we augment it and update the database, then the
   * original schema and the augmented schema are placed into the pathstore_applications.apps table
   * for reading by the children nodes as this utility should only ever be run on the ROOTSERVER
   *
   * @param appIdStart app id to start. If command was init this is 0
   * @param schemasToLoad list of file names that point to cql files to load
   */
  private void loadSchemas(final int appIdStart, final String[] schemasToLoad) {
    this.schemaInfo.generate();
    if (!this.schemaInfo.getAllKeySpaces().contains("pathstore_applications")) {
      System.out.println("Loading default applications schema");
      PathStoreSchemaLoader.loadApplicationSchema(this.session);
    }

    int appId = appIdStart;
    for (String s : schemasToLoad) {
      File f = new File(s);
      if (f.exists())
        try {
          String keyspaceName = f.getName().substring(0, f.getName().indexOf('.'));
          String schema = readFileContents(s);
          PathStoreSchemaLoader.parseSchema(schema).forEach(this.session::execute);

          this.schemaInfo.generate();
          for (String tableName : this.schemaInfo.getTablesByKeySpace(keyspaceName)) {

            SchemaInfoV2.Table table = this.schemaInfo.getTableObjectByName(tableName);

            augmentSchema(table);
            createViewTable(table);

            insertApplicationSchema(
                appId++,
                keyspaceName,
                this.cluster.getMetadata().getKeyspace(table.keyspace_name).exportAsString());
          }

        } catch (Exception e) {
          System.out.println("There was an error with the file: " + f.getName());
          e.printStackTrace();
          System.exit(1);
        }
      else {
        System.out.println("File " + f.getName() + " does not exist");
        System.exit(1);
      }
    }

    this.schemaInfo.generate();
  }

  /**
   * @param fileName file contents to read
   * @return returns all contents in a singular string
   * @throws IOException if the file does not exist
   */
  private String readFileContents(final String fileName) throws IOException {
    return new String(Files.readAllBytes(Paths.get(fileName)));
  }

  /**
   * TODO: Maybe remove string literals, This may be an exception
   *
   * <p>Rebuilds schema based on data queried by {@link SchemaInfoV2} to include a few extra columns
   *
   * <p>These columns are:
   *
   * <p>pathstore_version, pathstore_parent_timestamp, pathstore_dirty, pathstore_deleted,
   * pathstore_insert, pathstore_node
   *
   * @param table table to augment
   */
  private void augmentSchema(final SchemaInfoV2.Table table) {

    List<String> columnNames = this.schemaInfo.getColumnsByTable(table.table_name);
    Set<SchemaInfoV2.Column> columns = new HashSet<>();

    for (String columnName : columnNames)
      columns.add(this.schemaInfo.getColumnObjectByName(columnName));

    dropTable(table);

    StringBuilder query =
        new StringBuilder("CREATE TABLE " + table.keyspace_name + "." + table.table_name + "(");

    for (SchemaInfoV2.Column col : columns) {
      String type = col.type.compareTo("counter") == 0 ? "int" : col.type;
      query.append(col.column_name).append(" ").append(type).append(",");
    }

    query.append("pathstore_version timeuuid,");
    query.append("pathstore_parent_timestamp timeuuid,");
    query.append("pathstore_dirty boolean,");
    query.append("pathstore_deleted boolean,");
    query.append("pathstore_insert_sid text,");
    query.append("pathstore_node int,");
    query.append("PRIMARY KEY(");

    for (SchemaInfoV2.Column col : columns)
      if (col.kind.equals("partition_key")) query.append(col.column_name).append(",");
    for (SchemaInfoV2.Column col : columns)
      if (col.kind.equals("clustering")) query.append(col.column_name).append(",");

    query.append("pathstore_version) ");

    query.append(")");

    query.append("WITH CLUSTERING ORDER BY (");

    for (SchemaInfoV2.Column col : columns)
      if (col.kind.compareTo("clustering") == 0)
        query.append(col.column_name).append(" ").append(col.clustering_order).append(",");

    query.append("pathstore_version DESC) ");

    query
        .append("	    AND caching = ")
        .append(mapToString(table.caching))
        .append("	    AND comment = '")
        .append(table.comment)
        .append("'")
        .append("	    AND compaction = ")
        .append(mapToString(table.compaction))
        .append("	    AND compression = ")
        .append(mapToString(table.compression))
        .append("	    AND crc_check_chance = ")
        .append(table.crc_check_chance)
        .append("	    AND dclocal_read_repair_chance = ")
        .append(table.dclocal_read_repair_chance)
        .append("	    AND default_time_to_live = ")
        .append(table.default_time_to_live)
        .append("	    AND gc_grace_seconds = ")
        .append(table.gc_grace_seconds)
        .append("	    AND max_index_interval = ")
        .append(table.max_index_interval)
        .append("	    AND memtable_flush_period_in_ms = ")
        .append(table.memtable_flush_period_in_ms)
        .append("	    AND min_index_interval = ")
        .append(table.min_index_interval)
        .append("	    AND read_repair_chance = ")
        .append(table.read_repair_chance)
        .append("	    AND speculative_retry = '")
        .append(table.speculative_retry)
        .append("'");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.keyspace_name
                + "."
                + table.table_name
                + " (pathstore_dirty)");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.keyspace_name
                + "."
                + table.table_name
                + " (pathstore_deleted)");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.keyspace_name
                + "."
                + table.table_name
                + " (pathstore_insert_sid)");

    session.execute(query.toString());
  }

  private void createViewTable(final SchemaInfoV2.Table table) {
    final List<String> columns = this.schemaInfo.getColumnsByTable(table.table_name);

    StringBuilder query =
        new StringBuilder(
            "CREATE TABLE " + table.keyspace_name + ".view_" + table.table_name + "(");

    query.append("pathstore_view_id uuid,");

    for (String column_name : columns) {
      SchemaInfoV2.Column col = this.schemaInfo.getColumnObjectByName(column_name);
      String type = col.type.compareTo("counter") == 0 ? "int" : col.type;
      query.append(col.column_name).append(" ").append(type).append(",");
    }

    // BUG?!
    query.append("pathstore_version timeuuid,");
    query.append("pathstore_parent_timestamp timeuuid,");
    query.append("pathstore_dirty boolean,");
    query.append("pathstore_deleted boolean,");
    query.append("pathstore_node int,");

    query.append("PRIMARY KEY(pathstore_view_id,");

    for (String column_name : columns) {
      SchemaInfoV2.Column col = this.schemaInfo.getColumnObjectByName(column_name);
      if (col.kind.compareTo("regular") != 0) query.append(col.column_name).append(",");
    }

    query.append("pathstore_version) ");

    query.append(")");

    this.session.execute(query.toString());
  }

  /**
   * Converts a map object from cassandra into a string based representation that is interpretable
   * by cassandra on re-insert
   *
   * @param map object to convert
   * @return converted version
   */
  @SuppressWarnings("unchecked")
  private String mapToString(final Object map) {

    Map<String, String> m = (Map<String, String>) map;

    StringBuilder result = new StringBuilder("{");

    for (String key : m.keySet())
      result.append("'").append(key).append("':'").append(m.get(key)).append("',");

    return result.substring(0, result.length() - 1) + "}";
  }

  /**
   * TODO: Remove string literals
   *
   * @param table table to drop.
   */
  private void dropTable(final SchemaInfoV2.Table table) {
    String query = "DROP TABLE " + table.keyspace_name + "." + table.table_name;
    this.session.execute(query);
  }

  /**
   * TODO: Remove string literals
   *
   * <p>Drops entire keyspace
   *
   * @param keySpace keyspace to drop
   */
  private void dropKeySpace(final String keySpace) {
    String dropSchemaStatement = "drop keyspace if exists " + keySpace;
    System.out.println("Dropping keyspace " + keySpace);
    this.session.execute(dropSchemaStatement);
    System.out.println("Keyspace dropped");
  }

  /**
   * TODO: Make it so that it does not override current app schema of same name.
   *
   * @param appId application Id. Must be greater then 1 as 0 is reserved for the
   *     pathstore_application schema
   * @param keyspace name of schema. This is used for version control. I.e $appName-0.0.1
   * @param augmentedSchema schema after pathstore augmentation
   */
  private void insertApplicationSchema(
      final int appId, final String keyspace, final String augmentedSchema) {

    System.out.println("Inserting schema");
    Insert insert =
        QueryBuilder.insertInto(
            Constants.PathStoreApplications.KEYSPACE, Constants.PathStoreApplications.Apps.TABLE);

    insert.value(Constants.PathStoreApplications.Apps.APPID, appId);
    insert.value(Constants.PathStoreApplications.Apps.KEYSPACE, keyspace);
    insert.value(Constants.PathStoreApplications.Apps.AUGMENTED_SCHEMA, augmentedSchema);

    insert.value("pathstore_version", QueryBuilder.now());
    insert.value("pathstore_parent_timestamp", QueryBuilder.now());

    this.session.execute(insert);

    System.out.println("Schema inserted");
  }

  /**
   * Used to be run as a utility outside of the main pathstore instance
   *
   * @param args commands
   */
  public static void main(String[] args) {
    new ApplicationSchemaV2(args);
  }
}
