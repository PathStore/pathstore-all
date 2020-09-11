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
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.sessions.SessionToken;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This daemon is present on every node within the network except for the root node.
 *
 * <p>Its sole purpose is to 'push' or propagate all 'dirty' data that has arrived to this node. We
 * classify data has dirty when either A) the data was written to this node from a client or B) a
 * child node has pushed data to this node.
 *
 * <p>We also use the {@link #push(Collection, Session, Session, SchemaInfo, int)} function during
 * session migration and application un-deployment
 *
 * @implNote We only push data for augmented pathstore tables, not view_ or local_
 * @see pathstore.system.schemaFSM.PathStoreSlaveSchemaServer
 * @see PathStoreServerImplRMI#forcePush(SessionToken, int)
 */
public class PathStorePushServer implements Runnable {
  /** logger for errors */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStorePushServer.class);

  /** Filter function to filter out all local and view prefixed tables */
  public static final Predicate<Table> filterOutViewAndLocal =
      table ->
          !table.table_name.startsWith(Constants.VIEW_PREFIX)
              && !table.table_name.startsWith(Constants.LOCAL_PREFIX);

  /**
   * This function is used to produce an insert value for the parent node.
   *
   * <p>Add all non-null columns, set pathstore_parent_timestamp to now and set the pathstore_node
   * column to current node id
   *
   * @param row row to build from
   * @param keyspace keyspace of row
   * @param tablename table of row
   * @param columns columns for row
   * @param nodeid node id of current node
   * @return insert statement
   */
  private static Insert createInsert(
      final Row row,
      final String keyspace,
      final String tablename,
      final Collection<Column> columns,
      final int nodeid) {
    Insert insert = QueryBuilder.insertInto(keyspace, tablename);

    for (Column column : columns) {
      if (!row.isNull(column.column_name))
        if (!column.column_name.equals(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_NODE))
          if (column.column_name.compareTo(
                  Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP)
              == 0) insert.value(column.column_name, QueryBuilder.now());
          else insert.value(column.column_name, row.getObject(column.column_name));
    }
    insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_NODE, nodeid);

    return insert;
  }

  /**
   * This function is used to produce a delete statement to execute locally. This is used to remove
   * the dirty flag from a row that was pushed.
   *
   * <p>Where clauses are added for all non-regular columns (all primary key columns)
   *
   * @param row row to remove dirty flag from
   * @param keyspace keyspace of row
   * @param tablename table of row
   * @param columns columns for row
   * @return delete statement to remove dirty flag
   */
  private static Delete createDelete(
      final Row row,
      final String keyspace,
      final String tablename,
      final Collection<Column> columns) {
    Delete delete =
        QueryBuilder.delete(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY)
            .from(keyspace, tablename);

    for (Column column : columns)
      if (column.kind.compareTo("regular") != 0)
        delete.where(QueryBuilder.eq(column.column_name, row.getObject(column.column_name)));

    return delete;
  }

  /**
   * This function will push all dirty data from a set of table objects from local -> parent. For
   * all dirty data pushed it will also remove the dirty flag so it won't be pushed again
   *
   * @param tables tables to check for pushses
   * @param source node to push from
   * @param destination where to push to
   * @param schemaInfo schema info for source node
   * @param nodeid node id of the source node
   */
  public static void push(
      final Collection<Table> tables,
      final Session source,
      final Session destination,
      final SchemaInfo schemaInfo,
      final int nodeid) {
    try {
      for (Table table : tables) {

        // sanity check
        if (table.table_name.startsWith(Constants.VIEW_PREFIX)
            || table.table_name.startsWith(Constants.LOCAL_PREFIX)) continue;

        Select select = QueryBuilder.select().all().from(table.keyspace_name, table.table_name);
        select.where(QueryBuilder.eq(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY, true));

        ResultSet results = source.execute(select);

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

          if (str_insert.length() > PathStoreProperties.getInstance().MaxBatchSize
              || str_delete.length() > PathStoreProperties.getInstance().MaxBatchSize) {
            destination.execute(insert);
            source.execute(delete);
          } else {
            if (insertBatchSize + str_insert.length()
                    > PathStoreProperties.getInstance().MaxBatchSize
                || deleteBatchSize + str_delete.length()
                    > PathStoreProperties.getInstance().MaxBatchSize) {
              destination.execute(insertBatch);
              source.execute(deleteBatch);

              insertBatch = QueryBuilder.batch();
              deleteBatch = QueryBuilder.batch();

              insertBatchSize = 0;
              deleteBatchSize = 0;
            }

            insertBatch.add(insert);
            insertBatchSize += str_insert.length();

            deleteBatch.add(delete);
            deleteBatchSize += str_delete.length();
          }
        }
        if (insertBatchSize > 0) {
          try {
            destination.execute(insertBatch);
            source.execute(deleteBatch);
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

  /** Continuously call push every delta T defined by PushSleep property */
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

  /**
   * Used to build a set of tables to push from a schema info object
   *
   * @param schemaInfo schema info object to build table set for
   * @return collection of tables to push
   */
  public static Collection<Table> buildCollectionOfTablesFromSchemaInfo(
      final SchemaInfo schemaInfo) {
    return schemaInfo.getLoadedKeyspaces().stream()
        .map(schemaInfo::getTablesFromKeyspace)
        .flatMap(Collection::stream)
        .filter(filterOutViewAndLocal)
        .collect(Collectors.toSet());
  }
}
