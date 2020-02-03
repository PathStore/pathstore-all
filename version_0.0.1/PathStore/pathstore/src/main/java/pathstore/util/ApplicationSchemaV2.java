package pathstore.util;

import com.datastax.driver.core.*;

/**
 * TODO: Currently used tables in pathstore_application : apps, stores schema
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

        for(Row row: keyspace_columns){
            printAllDataFromTable(keyspace, row.getString("table_name"));
        }

    }
    private static void printAllDataFromTable(final String keyspace, final String table){
        ResultSet table_data = session.execute("select * from " + keyspace + "." + table);

        for(Row row : table_data){
            System.out.println(row);
        }
    }
}

