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
package pathstore.common;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.*;
import pathstore.client.PathStoreServerClient;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * This class is responsible for storing queries that have already been made. This is to allow for
 * quicker fetching of data.
 */
public class QueryCache {

  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(QueryCache.class);

  final HashMap<String, HashMap<String, List<QueryCacheEntry>>> entries = new HashMap<>();

  public void remove(final String keyspace) {
    entries.remove(keyspace);
  }

  public HashMap<String, HashMap<String, List<QueryCacheEntry>>> getEntries() {
    return entries;
  }

  private static QueryCache instance = null;

  public static QueryCache getInstance() {
    if (QueryCache.instance == null) QueryCache.instance = new QueryCache();
    return QueryCache.instance;
  }

  public List<QueryCacheEntry> getEntriesByTable(final String keyspace, final String table) {
    return this.entries.get(keyspace).get(table);
  }

  private void addKeyspace(String keyspace) {
    if (!entries.containsKey(keyspace)) {
      synchronized (entries) {
        if (!entries.containsKey(keyspace)) entries.put(keyspace, new HashMap<>());
      }
    }
  }

  private void addTable(String keyspace, String table) {
    addKeyspace(keyspace);
    if (!entries.get(keyspace).containsKey(table)) {
      synchronized (entries.get(keyspace)) {
        if (!entries.get(keyspace).containsKey(table))
          entries.get(keyspace).put(table, new ArrayList<>());
      }
    }
  }

  // called by child
  public QueryCacheEntry updateCache(
      String keyspace, String table, byte[] clausesSerialized, int limit, int lca)
      throws ClassNotFoundException, IOException {

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
    ObjectInputStream ois = new ObjectInputStream(bytesIn);
    List<Clause> clauses = (List<Clause>) ois.readObject();

    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry != null) entry.lca = lca;

    if (entry == null) entry = addEntry(keyspace, table, clauses, clausesSerialized, limit, lca);

    entry.waitUntilReady();

    return entry;
  }

  // executed on client
  public QueryCacheEntry updateCache(
      String keyspace, String table, List<Clause> clauses, int limit, int lca) {
    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry != null) entry.lca = lca;

    if (entry == null) entry = addEntry(keyspace, table, clauses, null, limit, lca);
    else if (lca != -1)
      this.processEntry(
          entry); // if the entry is already present and the lca is set inform the parent.

    entry.waitUntilReady();

    return entry;
  }

  public QueryCacheEntry getEntry(String keyspace, String table, List<Clause> clauses, int limit) {

    HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
    if (tableMap == null) return null;

    List<QueryCacheEntry> entryList = tableMap.get(table);
    if (entryList == null) return null;

    for (QueryCacheEntry e : entryList)
      if (e.isSame(clauses))
        if (e.limit == -1
            || e.limit > limit) // we already have a bigger query so don't add this one
        return e;

    return null;
  }

  private QueryCacheEntry addEntry(
      String keyspace,
      String table,
      List<Clause> clauses,
      byte[] clausesSerialized,
      int limit,
      int lca) {

    addTable(keyspace, table);

    HashMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
    List<QueryCacheEntry> entryList = tableMap.get(table);

    QueryCacheEntry newEntry = new QueryCacheEntry(keyspace, table, clauses, limit, lca);
    if (clausesSerialized != null) newEntry.setClausesSerialized(clausesSerialized);

    synchronized (entryList) {
      for (QueryCacheEntry entry : entryList) {
        if (entry.isSame(clauses)) {
          if (entry.limit == newEntry.limit) return entry;
          else if (entry.limit == -1 && newEntry.limit > 0) {
            newEntry.isCovered = entry;
            entry.covers.add(newEntry);
          } else if (entry.limit > 0 && newEntry.limit > 0 && entry.limit < newEntry.limit) {
            entry.isCovered = newEntry;
            newEntry.covers.add(entry);
          }
        }

        if (newEntry.isCovered == null && entry.isSuperSet(clauses)) {
          newEntry.isCovered = entry;
          entry.covers.add(newEntry);
        }

        if (entry.isCovered == null && entry.isSubSet(clauses)) {
          entry.isCovered = newEntry;
          newEntry.covers.add(entry);
        }
      }

      entryList.add(newEntry);
    }

    if (lca != -1)
      logger.info(
          String.format(
              "Entry added for keyspace %s and table %s with caluses %s",
              keyspace, table, clauses));

    return this.processEntry(newEntry);
  }

  private QueryCacheEntry processEntry(final QueryCacheEntry newEntry) {

    final int lca = newEntry.lca;

    // TODO add entry to DB (of your parent)
    try {

      // If the entry isn't covered or the lca value is set
      if (PathStoreProperties.getInstance().role != Role.ROOTSERVER
          && (newEntry.isCovered == null || lca != -1)) {

        if (lca != -1) logger.info(String.format("Adding parent entry for lca %d", lca));

        PathStoreServerClient.getInstance().addQueryEntry(newEntry);

        // Hossein: don't update your parents query cache (for now)

        if (PathStoreProperties.getInstance().role == Role.SERVER) {
          if (lca != -1) logger.info(String.format("Fetching data for lca %d", lca));

          fetchDelta(newEntry);
        }
      }

      // This is so that the entry can exist within the query cache after session migration has
      // occurred as normal
      if (newEntry.lca != -1) newEntry.lca = -1;
    } finally {
      newEntry.setReady();
    }

    return newEntry;
  }

  public UUID createDelta(
      String keyspace,
      String table,
      byte[] clausesSerialized,
      UUID parentTimestamp,
      int nodeID,
      int limit)
      throws IOException, ClassNotFoundException {
    ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
    ObjectInputStream ois = new ObjectInputStream(bytesIn);
    @SuppressWarnings("unchecked")
    List<Clause> clauses = (List<Clause>) ois.readObject();

    UUID deltaID = UUID.randomUUID();

    if (limit == -1) limit = Integer.MAX_VALUE;

    Select select = QueryBuilder.select().all().from(keyspace, table);
    select.allowFiltering();

    for (Clause clause : clauses) select.where(clause);

    Session local = PathStorePrivilegedCluster.getDaemonInstance().connect();

    try {

      // hossein here:
      select.setFetchSize(1000);

      ResultSet results = local.execute(select);

      Collection<Column> columns = SchemaInfo.getInstance().getTableColumns(keyspace, table);

      Batch batch = QueryBuilder.batch();

      int batchSize = 0;

      PathStorePrivilegedCluster cluster = PathStorePrivilegedCluster.getDaemonInstance();

      String primary =
          cluster
              .getMetadata()
              .getKeyspace(keyspace)
              .getTable(table)
              .getPrimaryKey()
              .get(0)
              .getName();

      Object previousKey = null;
      Object currentKey = null;
      int count = -1;

      int totalRowsChanged = 0;
      for (Row row : results) {
        currentKey = row.getObject(primary);
        if (!currentKey.equals(previousKey)) count++;
        if (count >= limit) break;

        if (row.getInt("pathstore_node") == nodeID
            || row.getUUID("pathstore_parent_timestamp").timestamp() <= parentTimestamp.timestamp())
          continue;

        totalRowsChanged++;
        Insert insert = QueryBuilder.insertInto(keyspace, "view_" + table);

        insert.value("pathstore_view_id", deltaID);

        // Hossein
        for (Column column : columns) {
          if (!row.isNull(column.column_name)
              && column.column_name.compareTo("pathstore_dirty") != 0
              && column.column_name.compareTo("pathstore_insert_sid") != 0)
            insert.value(column.column_name, row.getObject(column.column_name));
        }

        String statement = insert.toString();

        if (statement.length() > PathStoreProperties.getInstance().MaxBatchSize)
          local.execute(insert);
        else {
          if (batchSize + statement.length() > PathStoreProperties.getInstance().MaxBatchSize) {
            local.execute(batch);
            batch = QueryBuilder.batch();
            batchSize = 0;
          }
          batch.add(insert);
          batchSize += statement.length();
        }
      }
      if (batchSize > 0) local.execute(batch);

      if (totalRowsChanged == 0) return null;

      return deltaID;
    } finally {
      // local.close();
    }
  }

  public void fetchDelta(QueryCacheEntry entry) {
    UUID deltaId = null;

    if (entry.getParentTimeStamp() != null) {
      deltaId = PathStoreServerClient.getInstance().cretateQueryDelta(entry);
      if (deltaId == null) return;
    }

    if (entry.lca != -1)
      logger.info(
          String.format("fetching Delta for with deltaId %s with lca %d", deltaId, entry.lca));

    fetchData(entry, deltaId);
  }

  private void fetchData(QueryCacheEntry entry, UUID deltaID) {
    Session parent = PathStorePrivilegedCluster.getParentInstance().connect();
    Session local = PathStorePrivilegedCluster.getDaemonInstance().connect();

    String table = deltaID != null ? "view_" + entry.table : entry.table;

    if (entry.lca != -1)
      logger.info(String.format("Fetching data for deltaId %s on table %s", deltaID, table));

    Select select = QueryBuilder.select().all().from(entry.keyspace, table);

    select.allowFiltering();

    if (deltaID != null) select.where(QueryBuilder.eq("pathstore_view_id", deltaID));

    for (Clause clause : entry.clauses) select.where(clause);

    // hossein here again
    select.setFetchSize(1000);

    ResultSet results = parent.execute(select);

    Collection<Column> columns =
        SchemaInfo.getInstance().getTableColumns(entry.keyspace, entry.table);

    Batch batch = QueryBuilder.batch();

    int batchSize = 0;

    UUID highest_timestamp = null;

    // check this
    for (Row row : results) {

      Insert insert = QueryBuilder.insertInto(entry.keyspace, entry.table);

      for (Column column : columns) {
        if (column.column_name.compareTo("pathstore_parent_timestamp") == 0) {
          UUID row_timestamp = row.getUUID("pathstore_parent_timestamp");

          if (highest_timestamp == null
              || highest_timestamp.timestamp() < row_timestamp.timestamp())
            highest_timestamp = row_timestamp;

          insert.value("pathstore_parent_timestamp", QueryBuilder.now());
        } else {
          try {
            if (column.column_name.compareTo("pathstore_insert_sid") != 0
                && column.column_name.compareTo("pathstore_dirty") != 0
                && !row.isNull(column.column_name))
              insert.value(column.column_name, row.getObject(column.column_name));
          } catch (Exception e) {
            e.printStackTrace();
            System.err.println(
                " some error here: entry.keyspace entry.table"
                    + entry.keyspace
                    + " "
                    + entry.table);
          }
        }
      }

      String statement = insert.toString();

      if (statement.length() > PathStoreProperties.getInstance().MaxBatchSize)
        local.execute(insert);
      else {
        if (batchSize + statement.length() > PathStoreProperties.getInstance().MaxBatchSize) {
          local.execute(batch);
          batch = QueryBuilder.batch();
          batchSize = 0;
        }
        batch.add(insert);
        batchSize += statement.length();
      }
    }
    if (batchSize > 0) local.execute(batch);

    UUID entry_timestamp = entry.getParentTimeStamp();

    assert (entry_timestamp == null || entry_timestamp.timestamp() < highest_timestamp.timestamp());

    entry.setParentTimeStamp(highest_timestamp);
  }
}
