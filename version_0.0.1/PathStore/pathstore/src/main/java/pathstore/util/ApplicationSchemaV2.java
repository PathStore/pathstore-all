package pathstore.util;

import com.datastax.driver.core.*;

/**
 * TODO: Currently used tables in pathstore_application : apps, stores schema
 * <p>
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
    private static Session session = null;

    public static void main(String[] args) {
        Cluster cluster = new Cluster.Builder()
                .addContactPoints("127.0.0.1")
                .withPort(9062)
                .withSocketOptions(new SocketOptions().setTcpNoDelay(true).setReadTimeoutMillis(15000000))
                .withQueryOptions(
                        new QueryOptions()
                                .setRefreshNodeIntervalMillis(0)
                                .setRefreshNodeListIntervalMillis(0)
                                .setRefreshSchemaIntervalMillis(0)
                )
                .build();

        session = cluster.connect();

        ResultSet keyspaces = session.execute("select * from system_schema.keyspaces");

        for (Row row : keyspaces) {
            String keyspace = row.getString("keyspace_name");
            if (keyspace.startsWith("pathstore_")) {
                System.out.println(keyspace);
                printDataFromKeyspace(keyspace);
            }
        }

    }

    private static void printDataFromKeyspace(final String keyspace) {
        ResultSet keyspace_columns = session.execute("select * from system_schema.tables where keyspace_name='" + keyspace + "'");

        for (Row row : keyspace_columns) {
            String table = row.getString("table_name");
            System.out.println("\t" + table);
            ResultSet columns = session.execute("select * from system_schema.columns where keyspace_name='" + keyspace + "' and table_name='" + table + "'");
            for (Row column : columns) {
                String column_name = column.getString("column_name");
                System.out.println("\t\t" + column_name);
            }
        }

        for (Row row : keyspace_columns) {
            printAllDataFromTable(keyspace, row.getString("table_name"));
        }

    }

    private static void printAllDataFromTable(final String keyspace, final String table) {
        ResultSet table_data = session.execute("select * from " + keyspace + "." + table);

        for (Row row : table_data) {
            System.out.println(row);
        }
    }
}

