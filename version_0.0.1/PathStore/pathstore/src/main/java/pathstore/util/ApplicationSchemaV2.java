package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Objective of this class is to write the current application schemas to a table in the root server's database
 * Then when a child is started up the first thing it does is it checks it's parent node to see if it has the same schema's.
 * If it doesn't then it pulls it's parents schema's and updates it's current databases. During this time there needs to
 * be a blocker on the session to basically say that if a current request is on the keyspace of a currently updated table wait until that
 * operation is complete. We also need to check to see if you can update a schema while their is data inside the schema.
 * <p>
 * Current Plan of implementation:
 * <p>
 * 1. Write function to drop schema of a certain name.
 * 2. Write function to load pathstore_application schema.
 * 3. Insert the pathstore_application schema into pathstore_application.apps. This will only be used by other pathstore nodes
 * 4. Modify {@link pathstore.system.PathStoreServerImpl} to pull schema from parent server if it doesn't exist as this will never change while the pathstore network is alive (I think.)
 * 5. Write function to augment user defined schemas.
 * 6. Write function to load augment user defined shcemas into pathstore_application.apps
 * 7: Modify {@link pathstore.system.PathStoreServerImpl} and {@link pathstore.system.PathStorePullServer} to pull schema from parent server.
 * 8: Deal with potential use cases that
 */
public class ApplicationSchemaV2 {

    /**
     * Constants class to represent all the sql strings / column names of tables that are modified throughout this class
     * TODO: Potential move to a global locations\
     * TODO: Remove duplicate names
     * TODO: Write loader to load in cql files instead of hard coded tables
     */
    private static class Constants {
        public static final String PATHSTORE_APPLICATIONS_KEYSPACE_CREATION = "CREATE KEYSPACE IF NOT EXISTS " + PathStoreApplications.KEYSPACE + " WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;";
        public static final String PATHSTORE_APPLICATIONS_APPS_TABLE =
                "CREATE TABLE pathstore_applications.apps (" +
                        "		appid int PRIMARY KEY," +
                        "       key_space text," +
                        //   "		code blob," +
                        //   "		funcs list<int>," +
                        //   "		owner text," +
                        //   "		root_domain text," +
                        "	    app_name text," +
                        "	    schema_name text," +
                        "	    app_schema text," +
                        "time_created timestamp," +
                        //   "	    app_schema_augmented text" +
                        "	) WITH bloom_filter_fp_chance = 0.01" +
                        "	    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}" +
                        "	    AND comment = 'table definitions'" +
                        "	    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}" +
                        "	    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}" +
                        "	    AND crc_check_chance = 1.0" +
                        "	    AND dclocal_read_repair_chance = 0.0" +
                        "	    AND default_time_to_live = 0" +
                        "	    AND gc_grace_seconds = 604800" +
                        "	    AND max_index_interval = 2048" +
                        "	    AND memtable_flush_period_in_ms = 3600000" +
                        "	    AND min_index_interval = 128" +
                        "	    AND read_repair_chance = 0.0" +
                        "	    AND speculative_retry = '99PERCENTILE';";
        public static final String PATHSTORE_DEMO_TABLE =
                "CREATE TABLE pathstore_demo.users (" +
                        "    name text PRIMARY KEY," +
                        "    sport text," +
                        "    years int," +
                        "    vegetarian Boolean," +
                        "    color list<int>" +
                        ") WITH bloom_filter_fp_chance = 0.01" +
                        "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}" +
                        "    AND comment = 'table definitions'" +
                        "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}" +
                        "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}" +
                        "    AND crc_check_chance = 1.0" +
                        "    AND dclocal_read_repair_chance = 0.0" +
                        "    AND default_time_to_live = 0" +
                        "    AND gc_grace_seconds = 604800" +
                        "    AND max_index_interval = 2048" +
                        "    AND memtable_flush_period_in_ms = 3600000" +
                        "    AND min_index_interval = 128" +
                        "    AND read_repair_chance = 0.0" +
                        "    AND speculative_retry = '99PERCENTILE';";

        public static final class PathStoreApplications {
            public static final String KEYSPACE = "pathstore_applications";

            public static final class Apps {
                public static final String TABLE = "apps";
                public static final String APPID = "appid";
                public static final String KEYSPACE = "key_space";
                public static final String APP_NAME = "app_name";
                public static final String SCHEMA_NAME = "schema_name";
                public static final String APP_SCHEMA = "app_schema";
                public static final String TIME_CREATED = "time_created";
            }
        }

    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    private static Session session = null;

    public static void main(String[] args) {
        Cluster cluster = new Cluster.Builder()
                .addContactPoints("127.0.0.1")
                .withPort(9052)
                .withSocketOptions(new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
                .withQueryOptions(
                        new QueryOptions()
                                .setRefreshNodeIntervalMillis(0)
                                .setRefreshNodeListIntervalMillis(0)
                                .setRefreshSchemaIntervalMillis(0)
                )
                .build();

        session = cluster.connect();

        deleteSchema(Constants.PathStoreApplications.KEYSPACE);
        loadPathStoreApplicationSchema();
        insertApplicationSchema(
                0,
                Constants.PathStoreApplications.KEYSPACE,
                Constants.PathStoreApplications.KEYSPACE,
                Constants.PathStoreApplications.KEYSPACE,
                Constants.PATHSTORE_APPLICATIONS_APPS_TABLE
        );
        deleteSchema("pathstore_demo");
        createKeySpace("pathstore_demo");
        insertApplicationSchema(1, "pathstore_demo", "demo", "demo-v1", Constants.PATHSTORE_DEMO_TABLE);

        session.close();
        cluster.close();

    }

    /**
     * TODO: Move string literals
     *
     * @param schemaName
     */
    private static void deleteSchema(final String schemaName) {
        String dropSchemaStatement = "drop keyspace if exists " + schemaName;
        System.out.println("Dropping keyspace " + schemaName);
        session.execute(dropSchemaStatement);
        System.out.println("Keyspace dropped");
    }

    /**
     * TODO: Export table to file instead of hard coded feature.
     * <p>
     * Creates pathstore_applications keyspace
     * Inserts One table this table is apps it has the following properties
     * <p>
     * appid: application id bounds must be greater then or equal to 1
     * app_name: name of application
     * keyspace: keyspace name
     * schema_name: i.e for version control
     * app_schema: literal schema stored in plain text
     * time_created: time of creation for log of schemas
     */
    private static void loadPathStoreApplicationSchema() {
        System.out.println("Creating keyspace");
        session.execute(Constants.PATHSTORE_APPLICATIONS_KEYSPACE_CREATION);
        System.out.println("Keyspace created");
    }

    /**
     * TODO: Move string literal
     */
    private static void createKeySpace(final String keySpaceName) {
        session.execute("CREATE KEYSPACE IF NOT EXISTS " + keySpaceName + " WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;");
    }

    /**
     * TODO: Make it so that it does not override current app schema of same name.
     *
     * @param appId      application Id. Must be greater then 1 as 0 is reserved for the pathstore_application schema
     * @param keyspace   keyspace the schema is apart of
     * @param appName    name of application. Easier identifier then its associated appId
     * @param schemaName name of schema. This is used for version control. I.e $appName-0.0.1
     * @param schema     literal contents of schema. This is used to distribute the schema to lower nodes
     */
    private static void insertApplicationSchema(final int appId, final String keyspace, final String appName, final String schemaName, final String schema) {
        session.execute(schema);

        System.out.println("Inserting schema");
        Insert insert = QueryBuilder.insertInto(
                Constants.PathStoreApplications.KEYSPACE,
                Constants.PathStoreApplications.Apps.TABLE
        );

        insert.value(
                Constants.PathStoreApplications.Apps.APPID,
                appId
        );
        insert.value(
                Constants.PathStoreApplications.Apps.KEYSPACE,
                keyspace
        );
        insert.value(
                Constants.PathStoreApplications.Apps.APP_NAME
                , appName
        );
        insert.value(
                Constants.PathStoreApplications.Apps.SCHEMA_NAME
                , schemaName
        );
        insert.value(
                Constants.PathStoreApplications.Apps.APP_SCHEMA
                , schema
        );
        insert.value(
                Constants.PathStoreApplications.Apps.TIME_CREATED
                , getTimeStamp()
        );

        session.execute(insert);

        System.out.println("Schema inserted");
    }

    private static Timestamp getTimeStamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}

