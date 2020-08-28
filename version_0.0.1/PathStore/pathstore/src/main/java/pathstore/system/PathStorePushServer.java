/**
 * ********
 *
 * <p>Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*********
 */
package pathstore.system;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.*;
import org.apache.commons.cli.*;
import pathstore.common.PathStoreProperties;
import pathstore.sessions.SessionToken;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;

import java.util.Collection;
import java.util.stream.Collectors;

/** TODO: Comment */
public class PathStorePushServer implements Runnable {
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStorePushServer.class);

  private static Insert createInsert(
      Row row, String keyspace, String tablename, Collection<Column> columns, int nodeid) {
    Insert insert = QueryBuilder.insertInto(keyspace, tablename);

    for (Column column : columns) {
      if (!row.isNull(column.column_name))
        if (!column.column_name.equals("pathstore_node"))
          if (!column.column_name.equals("pathstore_insert_sid")) // Hossein
            // if(row.getObject(column.column_name)==null)
            //	{
            // insert.value("pathstore_node", PathStoreProperties.getInstance().NodeID);
            //	continue;
            // }
            if (column.column_name.compareTo("pathstore_parent_timestamp") == 0)
              insert.value(column.column_name, QueryBuilder.now());
            else insert.value(column.column_name, row.getObject(column.column_name));
    }
    insert.value("pathstore_node", nodeid);

    return insert;
  }

  private static Delete createDelete(
      Row row, String keyspace, String tablename, Collection<Column> columns) {
    Delete delete = QueryBuilder.delete("pathstore_dirty").from(keyspace, tablename);

    for (Column column : columns)
      if (column.kind.compareTo("regular") != 0)
        delete.where(QueryBuilder.eq(column.column_name, row.getObject(column.column_name)));

    return delete;
  }

  public static void push(
      final Collection<Table> tables,
      final Session local,
      final Session parent,
      final SchemaInfo schemaInfo,
      final int nodeid) {
    try {
      for (Table table : tables) {
        if (table.table_name.startsWith("view_") || table.table_name.startsWith("local_")) continue;

        Select select = QueryBuilder.select().all().from(table.keyspace_name, table.table_name);
        select.where(QueryBuilder.eq("pathstore_dirty", true));

        ResultSet results = local.execute(select);

        Collection<Column> columns = schemaInfo.getTableColumns(table);

        Batch insertBatch = QueryBuilder.batch();
        Batch deleteBatch = QueryBuilder.batch();

        int insertBatchSize = 0;
        int deleteBatchSize = 0;

        for (Row row : results) {

          Insert insert = createInsert(row, table.keyspace_name, table.table_name, columns, nodeid);
          Delete delete = createDelete(row, table.keyspace_name, table.table_name, columns);

          String str_insert = insert.toString();
          String str_delete = delete.toString();

          // System.out.println("insert: " + str_insert );

          if (str_insert.length() > PathStoreProperties.getInstance().MaxBatchSize
              || str_delete.length() > PathStoreProperties.getInstance().MaxBatchSize) {
            // logger.debug("Executing parent insert and local delete");
            parent.execute(insert);
            local.execute(delete);
          } else {
            if (insertBatchSize + str_insert.length()
                    > PathStoreProperties.getInstance().MaxBatchSize
                || deleteBatchSize + str_delete.length()
                    > PathStoreProperties.getInstance().MaxBatchSize) {
              // logger.debug("Executing parent insert and local delete");
              parent.execute(insertBatch);
              local.execute(deleteBatch);

              insertBatch = QueryBuilder.batch();
              deleteBatch = QueryBuilder.batch();

              insertBatchSize = 0;
              deleteBatchSize = 0;
            }
            // System.out.println("adding: " + insert);

            insertBatch.add(insert);
            insertBatchSize += str_insert.length();

            deleteBatch.add(delete);
            deleteBatchSize += str_delete.length();
          }
        }
        if (insertBatchSize > 0) {
          try {
            parent.execute(insertBatch);
            local.execute(deleteBatch);
          } catch (Exception e) {
            logger.error(e);
          }
        }
      }
    } catch (Exception e) {
      logger.error(e);
      // local.close();
      // parent.close();
    }
  }

  public synchronized void run() {
    logger.info("Spawned pathstore push server thread");

    Session local = PathStorePrivilegedCluster.getDaemonInstance().connect();
    Session parent = PathStorePrivilegedCluster.getParentInstance().connect();

    while (true) {
      try {
        push(
            buildCollectionOfTablesFromSchemaInfo(SchemaInfo.getInstance()),
            local,
            parent,
            SchemaInfo.getInstance(),
            PathStoreProperties.getInstance().NodeID);
        Thread.sleep(PathStoreProperties.getInstance().PushSleep);
      } catch (InterruptedException e) {
        System.err.println("PathStorePushServer exception: " + e.toString());
        e.printStackTrace();
      }
    }
  }

  public static Collection<Table> buildCollectionOfTablesFromSchemaInfo(
      final SchemaInfo schemaInfo) {
    return schemaInfo.getLoadedKeyspaces().stream()
        .map(schemaInfo::getTablesFromKeyspace)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * This function is used to build a collection of tables from a session token. It will built this
   * set differently depending on the granularity of the session
   *
   * @param sessionToken session token
   * @return collection of tables for that session to push
   */
  public static Collection<Table> buildCollectionFromSessionToken(final SessionToken sessionToken) {
    switch (sessionToken.sessionType) {
      case KEYSPACE:
        return sessionToken.getData().stream()
            .map(keyspace -> SchemaInfo.getInstance().getTablesFromKeyspace(keyspace))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      case TABLE:
        return sessionToken.getData().stream()
            .map(
                entry -> {
                  int locationOfPeriod = entry.indexOf('.');
                  return SchemaInfo.getInstance()
                      .getTableFromKeyspaceAndTableName(
                          entry.substring(0, locationOfPeriod),
                          entry.substring(locationOfPeriod + 1));
                })
            .collect(Collectors.toList());
    }
    logger.error(
        String.format(
            "Could not build the set of tables for session token %s", sessionToken.sessionName));
    return null;
  }

  private static void parseCommandLineArguments(String args[]) {
    Options options = new Options();

    options.addOption(
        Option.builder("n").longOpt("nodeid").desc("Number").hasArg().argName("nodeid").build());

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);

      System.exit(1);
      return;
    }

    if (cmd.hasOption("nodeid"))
      PathStoreProperties.getInstance().NodeID = Integer.parseInt(cmd.getOptionValue("nodeid"));
  }
}
