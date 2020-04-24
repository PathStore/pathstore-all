package pathstoreweb.pathstoreadminpanel.services.applications;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.common.Constants;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.system.schemaloader.PathStoreSchemaLoaderUtils;
import pathstore.util.SchemaInfo;
import pathstoreweb.pathstoreadminpanel.services.RuntimeErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applications.formatter.AddApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.io.*;
import java.util.List;
import java.util.Map;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.AddApplicationPayload;

/**
 * Creates an available application for the user to deploy on the network.
 *
 * <p>Takes a user defined cql file and writes it to the apps table if no errors occur
 */
public class AddApplication implements IService {

  /** {@link AddApplicationPayload} */
  private final AddApplicationPayload addApplicationPayload;

  /** Privileged session to access */
  private final Session session;

  /** Schema info on current database */
  private final SchemaInfo schemaInfo;

  /** @param payload {@link #addApplicationPayload} */
  public AddApplication(final AddApplicationPayload payload) {
    this.addApplicationPayload = payload;
    this.session = PathStorePriviledgedCluster.getInstance().connect();
    this.schemaInfo = SchemaInfo.getInstance();
  }

  /**
   * Parses the file passed by the user.
   *
   * @return file contents
   */
  private String getUserPassSchema() {

    BufferedReader br;
    StringBuilder schema = new StringBuilder();

    try {

      String line;
      InputStream is = this.addApplicationPayload.getApplicationSchema().getInputStream();
      br = new BufferedReader(new InputStreamReader(is));

      while ((line = br.readLine()) != null) schema.append(line).append("\n");

    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    return schema.toString();
  }

  /**
   * Loads the schema and writes it to the table
   *
   * @return status: success / failure
   */
  @Override
  public String response() {
    try {
      this.loadSchemas(getMaxAppId(), this.getUserPassSchema());
    } catch (Exception e) {
      return new RuntimeErrorFormatter("Schema that was passed is invalid").format();
    }

    return new AddApplicationFormatter(this.addApplicationPayload.applicationName).format();
  }

  /** @return map appid in current table */
  private int getMaxAppId() {
    ResultSet set =
        this.session.execute("select max(appid) from pathstore_applications.apps limit 1");
    return set != null ? set.one().getInt("system.max(" + "appid" + ")") + 1 : 0;
  }

  /**
   * Loads schemas into the database
   *
   * <p>First it parses the file and executes all their commands.
   *
   * <p>Then the {@link SchemaInfo} is updated to the latest version of the schemas
   *
   * <p>Then if the keyspace needs augmentation we augment it and update the database, then the
   * original schema and the augmented schema are placed into the pathstore_applications.apps table
   * for reading by the children nodes as this utility should only ever be run on the ROOTSERVER
   *
   * @param appIdStart app id to start. If command was init this is 0
   * @param schema schema read from multipart file
   */
  private void loadSchemas(final int appIdStart, final String schema) throws Exception {
    int appId = appIdStart;

    try {
      PathStoreSchemaLoaderUtils.parseSchema(schema).forEach(this.session::execute);
    } catch (RuntimeException ignored) { // error loading the schema
      session.execute("drop keyspace if exists " + this.addApplicationPayload.applicationName);
      throw new Exception();
    }

    this.schemaInfo.reset();
    for (SchemaInfo.Table table :
        this.schemaInfo.getSchemaInfo().get(this.addApplicationPayload.applicationName).keySet()) {

      augmentSchema(table);
      createViewTable(table);

      insertApplicationSchema(
          appId++,
          this.addApplicationPayload.applicationName,
          PathStorePriviledgedCluster.getInstance()
              .getMetadata()
              .getKeyspace(table.getKeyspace_name())
              .exportAsString());
    }

    session.execute("drop keyspace if exists " + this.addApplicationPayload.applicationName);

    this.schemaInfo.reset();
  }
  /**
   * Rebuilds schema based on data queried by {@link SchemaInfo} to include a few extra columns
   *
   * <p>These columns are:
   *
   * <p>pathstore_version, pathstore_parent_timestamp, pathstore_dirty, pathstore_deleted,
   * pathstore_insert, pathstore_node
   *
   * @param table table to augment
   */
  private void augmentSchema(final SchemaInfo.Table table) {
    dropTable(table);

    List<SchemaInfo.Column> columns =
        this.schemaInfo.getTableColumns(table.getKeyspace_name(), table.getTable_name());

    StringBuilder query =
        new StringBuilder(
            "CREATE TABLE " + table.getKeyspace_name() + "." + table.getTable_name() + "(");

    for (SchemaInfo.Column col : columns) {
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

    for (SchemaInfo.Column col : columns)
      if (col.kind.equals("partition_key")) query.append(col.column_name).append(",");
    for (SchemaInfo.Column col : columns)
      if (col.kind.equals("clustering")) query.append(col.column_name).append(",");

    query.append("pathstore_version) ");

    query.append(")");

    query.append("WITH CLUSTERING ORDER BY (");

    for (SchemaInfo.Column col : columns)
      if (col.kind.compareTo("clustering") == 0)
        query.append(col.column_name).append(" ").append(col.clustering_order).append(",");

    query.append("pathstore_version DESC) ");

    query
        .append("	    AND caching = ")
        .append(mapToString(table.getCaching()))
        .append("	    AND comment = '")
        .append(table.getComment())
        .append("'")
        .append("	    AND compaction = ")
        .append(mapToString(table.getCompaction()))
        .append("	    AND compression = ")
        .append(mapToString(table.getCompression()))
        .append("	    AND crc_check_chance = ")
        .append(table.getCrc_check_chance())
        .append("	    AND dclocal_read_repair_chance = ")
        .append(table.getDclocal_read_repair_chance())
        .append("	    AND default_time_to_live = ")
        .append(table.getDefault_time_to_live())
        .append("	    AND gc_grace_seconds = ")
        .append(table.getGc_grace_seconds())
        .append("	    AND max_index_interval = ")
        .append(table.getMax_index_interval())
        .append("	    AND memtable_flush_period_in_ms = ")
        .append(table.getMemtable_flush_period_in_ms())
        .append("	    AND min_index_interval = ")
        .append(table.getMin_index_interval())
        .append("	    AND read_repair_chance = ")
        .append(table.getRead_repair_chance())
        .append("	    AND speculative_retry = '")
        .append(table.getSpeculative_retry())
        .append("'");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.getKeyspace_name()
                + "."
                + table.getTable_name()
                + " (pathstore_dirty)");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.getKeyspace_name()
                + "."
                + table.getTable_name()
                + " (pathstore_deleted)");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.getKeyspace_name()
                + "."
                + table.getTable_name()
                + " (pathstore_insert_sid)");

    session.execute(query.toString());
  }

  /**
   * Creates the corresponding view table for the all user created tables
   *
   * @param table table that needs a view table
   */
  private void createViewTable(final SchemaInfo.Table table) {
    StringBuilder query =
        new StringBuilder(
            "CREATE TABLE " + table.getKeyspace_name() + ".view_" + table.getTable_name() + "(");

    query.append("pathstore_view_id uuid,");

    List<SchemaInfo.Column> columns =
        this.schemaInfo.getTableColumns(table.getKeyspace_name(), table.getTable_name());

    for (SchemaInfo.Column col : columns) {
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

    for (SchemaInfo.Column col : columns) {
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

  /** @param table table to drop. */
  private void dropTable(final SchemaInfo.Table table) {
    String query = "DROP TABLE " + table.getKeyspace_name() + "." + table.getTable_name();
    this.session.execute(query);
  }

  /**
   * @param appId application Id. Must be greater then 1 as 0 is reserved for the
   *     pathstore_application schema
   * @param keyspace name of schema. This is used for version control. I.e $appName-0.0.1
   * @param augmentedSchema schema after pathstore augmentation
   */
  private void insertApplicationSchema(
      final int appId, final String keyspace, final String augmentedSchema) {
    Insert insert = QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);

    insert.value("appid", appId);
    insert.value(Constants.APPS_COLUMNS.KEYSPACE_NAME, keyspace);
    insert.value(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA, augmentedSchema);

    insert.value("pathstore_version", QueryBuilder.now());
    insert.value("pathstore_parent_timestamp", QueryBuilder.now());

    this.session.execute(insert);
  }
}
