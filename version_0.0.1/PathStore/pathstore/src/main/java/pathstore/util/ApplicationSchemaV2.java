package pathstore.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
     * TODO: Write loader to load in cql files instead of hard coded tables
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

        if (!new File(args[0]).getName().equals("pathstore_applications.cql")) {
            System.out.println("You need to include the applications schema as the first parameter");
            System.exit(1);
        }

        for (String s : getAllKeySpaces()) {
            dropKeySpace(s);
        }

        try {
            int appId = 0;
            for (String s : args) {
                File f = new File(s);
                if (f.exists())
                    insertApplicationSchema(appId++, f.getName(), f.getName(), readFileContents(s));
                else
                    System.out.println("File " + f.getName() + " does not exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        session.close();
        cluster.close();

    }

    private static String readFileContents(final String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    private static List<String> getAllKeySpaces() {
        ResultSet set = session.execute("select * from system_schema.keyspaces");

        List<String> list = new LinkedList<>();

        for (Row row : set) {
            String keyspaceName = row.getString("keyspace_name");
            if (keyspaceName.startsWith("pathstore_"))
                list.add(keyspaceName);
        }

        return list;
    }

    /**
     * TODO: Move string literals
     */
    private static void dropKeySpace(final String keySpace) {
        String dropSchemaStatement = "drop keyspace if exists " + keySpace;
        System.out.println("Dropping keyspace " + keySpace);
        session.execute(dropSchemaStatement);
        System.out.println("Keyspace dropped");
    }

    /**
     * TODO: Make it so that it does not override current app schema of same name.
     *
     * @param appId      application Id. Must be greater then 1 as 0 is reserved for the pathstore_application schema
     * @param appName    name of application. Easier identifier then its associated appId
     * @param schemaName name of schema. This is used for version control. I.e $appName-0.0.1
     * @param schema     literal contents of schema. This is used to distribute the schema to lower nodes
     */
    private static void insertApplicationSchema(final int appId, final String appName, final String schemaName, final String schema) {
        String[] commands = schema.split(";");
        for (String c : commands) {
            String cql = c.trim();
            if (cql.length() > 0)
                session.execute(cql);
        }

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

