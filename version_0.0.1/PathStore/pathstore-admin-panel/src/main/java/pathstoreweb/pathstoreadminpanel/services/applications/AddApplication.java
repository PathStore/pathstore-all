package pathstoreweb.pathstoreadminpanel.services.applications;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.system.schemaFSM.PathStoreSchemaLoaderUtils;
import pathstore.util.SchemaInfo;
import pathstoreweb.pathstoreadminpanel.services.RuntimeErrorFormatter;
import pathstoreweb.pathstoreadminpanel.services.applications.formatter.AddApplicationFormatter;
import pathstoreweb.pathstoreadminpanel.services.IService;

import java.io.*;
import java.util.Collection;
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
  public ResponseEntity<String> response() {
    try {
      this.loadSchemas(this.getUserPassSchema());
    } catch (Exception e) {
      this.schemaInfo.removeKeyspace(this.addApplicationPayload.applicationName);
      e.printStackTrace();
      return new RuntimeErrorFormatter("Schema that was passed is invalid").format();
    }

    return new AddApplicationFormatter(this.addApplicationPayload.applicationName).format();
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
   * @param schema schema read from multipart file
   */
  private void loadSchemas(final String schema) throws Exception {

    try {
      PathStoreSchemaLoaderUtils.parseSchema(schema).forEach(this.session::execute);

      // load information for that loaded schema.
      this.schemaInfo.loadKeyspace(this.addApplicationPayload.applicationName);
    } catch (RuntimeException ignored) { // error loading the schema
      throw new Exception();
    } finally {
      session.execute("drop keyspace if exists " + this.addApplicationPayload.applicationName);
    }

    for (SchemaInfo.Table table :
        this.schemaInfo.getTablesFromKeyspace(this.addApplicationPayload.applicationName)) {

      augmentSchema(table);
      createViewTable(table);

      insertApplicationSchema(
          this.addApplicationPayload.applicationName,
          PathStorePriviledgedCluster.getInstance()
              .getMetadata()
              .getKeyspace(table.keyspace_name)
              .exportAsString());
    }

    this.schemaInfo.removeKeyspace(this.addApplicationPayload.applicationName);
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

    Collection<SchemaInfo.Column> columns = this.schemaInfo.getTableColumns(table);

    StringBuilder query =
        new StringBuilder("CREATE TABLE " + table.keyspace_name + "." + table.table_name + "(");

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

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.keyspace_name
                + "."
                + table.table_name
                + " (pathstore_parent_timestamp)");

    session.execute(query.toString());

    query =
        new StringBuilder(
            "CREATE INDEX ON "
                + table.keyspace_name
                + "."
                + table.table_name
                + " (pathstore_node)");

    session.execute(query.toString());

    // load indexes
    for (SchemaInfo.Index index : this.schemaInfo.getTableIndexes(table)) {
      String indexQuery =
          String.format(
              "CREATE INDEX ON %s.%s (%s)",
              index.keyspace_name, index.table_name, index.options.get("target"));

      System.out.println(indexQuery);

      session.execute(indexQuery);
    }

    // load UDT's
    for (SchemaInfo.Type type :
        this.schemaInfo.getKeyspaceTypes(this.addApplicationPayload.applicationName)) {

      // Build types into string
      StringBuilder types = new StringBuilder();
      for (int i = 0; i < type.field_names.size(); i++) {
        if (i > 0) types.append(",").append("\n");
        types.append(type.field_names.get(i)).append(" ").append(type.field_types.get(i));
      }

      String typeQuery =
          String.format(
              "CREATE TYPE %s.%s (\n%s\n)", type.keyspace_name, type.type_name, types.toString());

      System.out.println(typeQuery);

      session.execute(typeQuery);
    }
  }

  /**
   * Creates the corresponding view table for the all user created tables
   *
   * @param table table that needs a view table
   */
  private void createViewTable(final SchemaInfo.Table table) {
    StringBuilder query =
        new StringBuilder(
            "CREATE TABLE " + table.keyspace_name + ".view_" + table.table_name + "(");

    query.append("pathstore_view_id uuid,");

    Collection<SchemaInfo.Column> columns = this.schemaInfo.getTableColumns(table);

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

  /**
   * @param keyspace name of schema. This is used for version control. I.e $appName-0.0.1
   * @param augmentedSchema schema after pathstore augmentation
   */
  private void insertApplicationSchema(final String keyspace, final String augmentedSchema) {
    Insert insert = QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);

    insert.value(Constants.APPS_COLUMNS.KEYSPACE_NAME, keyspace);
    insert.value(Constants.APPS_COLUMNS.AUGMENTED_SCHEMA, augmentedSchema);

    insert.value("pathstore_version", QueryBuilder.now());
    insert.value("pathstore_parent_timestamp", QueryBuilder.now());

    this.session.execute(insert);
  }
}
