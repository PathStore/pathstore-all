package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * Objective of this class is to write the current application schemas to a table in the root
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
        public static final String APP_NAME = "app_name";
        public static final String SCHEMA_NAME = "schema_name";
        public static final String APP_SCHEMA = "app_schema";
        public static final String TIME_CREATED = "time_created";
      }
    }
  }

  private final Session session;
  private final SchemaInfoV2 schemaInfo;

  ApplicationSchemaV2(final String[] args) {
    Cluster cluster =
        new Cluster.Builder()
            .addContactPoints("127.0.0.1")
            .withPort(9052)
            .withSocketOptions(
                new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
            .withQueryOptions(
                new QueryOptions()
                    .setRefreshNodeIntervalMillis(0)
                    .setRefreshNodeListIntervalMillis(0)
                    .setRefreshSchemaIntervalMillis(0))
            .build();

    this.session = cluster.connect();

    this.schemaInfo = new SchemaInfoV2(this.session);

    switch (args[0]) {
      case "init":
        for (String s : this.schemaInfo.getAllKeySpaces()) {
          dropKeySpace(s);
        }
        this.loadSchemas(0, Arrays.copyOfRange(args, 1, args.length));
        break;
      case "import":
        if (!this.schemaInfo.getAllKeySpaces().contains("pathstore_applications")) {
          System.out.println(
              "You cannot run the import command if your current PathStore instance does not contain the pathstore_applications keyspace. Please run the init command first");
          System.exit(1);
        }
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
    cluster.close();
  }

  private void loadSchemas(final int appIdStart, final String[] schemasToLoad) {
    int appId = appIdStart;
    for (String s : schemasToLoad) {
      File f = new File(s);
      if (f.exists())
        try {
          insertApplicationSchema(appId++, f.getName(), f.getName(), readFileContents(s));
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
  }

  private String readFileContents(final String fileName) throws IOException {
    return new String(Files.readAllBytes(Paths.get(fileName)));
  }

  /** TODO: Move string literals */
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
   * @param appName name of application. Easier identifier then its associated appId
   * @param schemaName name of schema. This is used for version control. I.e $appName-0.0.1
   * @param schema literal contents of schema. This is used to distribute the schema to lower nodes
   */
  private void insertApplicationSchema(
      final int appId, final String appName, final String schemaName, final String schema) {
    String[] commands = schema.split(";");
    for (String c : commands) {
      String cql = c.trim();
      if (cql.length() > 0) this.session.execute(cql);
    }

    System.out.println("Inserting schema");
    Insert insert =
        QueryBuilder.insertInto(
            Constants.PathStoreApplications.KEYSPACE, Constants.PathStoreApplications.Apps.TABLE);

    insert.value(Constants.PathStoreApplications.Apps.APPID, appId);
    insert.value(Constants.PathStoreApplications.Apps.APP_NAME, appName);
    insert.value(Constants.PathStoreApplications.Apps.SCHEMA_NAME, schemaName);
    insert.value(Constants.PathStoreApplications.Apps.APP_SCHEMA, schema);
    insert.value(Constants.PathStoreApplications.Apps.TIME_CREATED, getTimeStamp());

    this.session.execute(insert);

    System.out.println("Schema inserted");
  }

  private Timestamp getTimeStamp() {
    return new Timestamp(System.currentTimeMillis());
  }

  public static void main(String[] args) {
    new ApplicationSchemaV2(args);
  }
}
