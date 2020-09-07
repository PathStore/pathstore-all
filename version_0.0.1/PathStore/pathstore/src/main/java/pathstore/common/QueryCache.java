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
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is responsible for storing queries that have already been made. This is to allow for
 * quicker fetching of data.
 */
public class QueryCache {

  private final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> entries =
      new ConcurrentHashMap<>();

  public void remove(final String keyspace) {
    entries.remove(keyspace);
  }

  public ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> getEntries() {
    return entries;
  }

  private static QueryCache instance = null;

  public static synchronized QueryCache getInstance() {
    if (QueryCache.instance == null) QueryCache.instance = new QueryCache();
    return QueryCache.instance;
  }

  private void addKeyspace(final String keyspace) {
    this.entries.putIfAbsent(keyspace, new ConcurrentHashMap<>());
  }

  private void addTable(final String keyspace, final String table) {
    addKeyspace(keyspace);
    this.entries.get(keyspace).putIfAbsent(table, new ArrayList<>());
  }

  public Collection<QueryCacheEntry> getEntries(final SchemaInfo.Table table) {
    // if table is null return empty list
    if (table == null) return Collections.emptyList();

    // get values from keyspace
    ConcurrentMap<String, List<QueryCacheEntry>> tableMap = this.entries.get(table.keyspace_name);

    // if the keyspace doesn't exist return empty list
    if (tableMap == null) return Collections.emptyList();

    // get entries by table
    List<QueryCacheEntry> entries = tableMap.get(table.table_name);

    // if non-null return entries else return empty list
    return entries != null ? entries : Collections.emptyList();
  }

  // called by child
  public QueryCacheEntry updateCache(
      String keyspace, String table, byte[] clausesSerialized, int limit)
      throws ClassNotFoundException, IOException {

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
    ObjectInputStream ois = new ObjectInputStream(bytesIn);
    List<Clause> clauses = (List<Clause>) ois.readObject();

    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry == null) entry = addEntry(keyspace, table, clauses, clausesSerialized, limit);

    entry.waitUntilReady();

    return entry;
  }

  /**
   * This function is used to update the cache when an incoming select statement comes in.
   *
   * <p>This function is called by the client.
   *
   * @param keyspace keyspace of the query
   * @param table table of the query
   * @param clauses where statements for the query
   * @param limit return limit
   * @return entry created.
   */
  public QueryCacheEntry updateCache(
      String keyspace, String table, List<Clause> clauses, int limit) {
    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry == null) entry = addEntry(keyspace, table, clauses, null, limit);
    else System.out.println("Cache hit");

    entry.waitUntilReady();

    return entry;
  }

  /**
   * This function is used to retrieve an entry from the local cache.
   *
   * @param keyspace keyspace of entry
   * @param table table of entry
   * @param clauses clauses of entry
   * @param limit limit
   * @return entry if it already exists, else null
   */
  public QueryCacheEntry getEntry(String keyspace, String table, List<Clause> clauses, int limit) {

    ConcurrentMap<String, List<QueryCacheEntry>> tableMap = this.entries.get(keyspace);
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

  /**
   * This function is used to add an entry to the cache.
   *
   * @param keyspace keyspace of entry
   * @param table table of entry
   * @param clauses clauses of entry
   * @param clausesSerialized serialized clauses if applicable
   * @param limit limit of query
   * @return entry created
   */
  private QueryCacheEntry addEntry(
      String keyspace, String table, List<Clause> clauses, byte[] clausesSerialized, int limit) {

    addTable(keyspace, table);

    // where to place entry
    ConcurrentMap<String, List<QueryCacheEntry>> tableMap = entries.get(keyspace);
    List<QueryCacheEntry> entryList = tableMap.get(table);

    // create entry
    QueryCacheEntry newEntry = new QueryCacheEntry(keyspace, table, clauses, limit);
    if (clausesSerialized != null) newEntry.setClausesSerialized(clausesSerialized);

    // iterate over all entries to setup new entry and add to list if applicable
    synchronized (entryList) {
      for (QueryCacheEntry entry : entryList) {
        // if the clauses are the same check for limit difs
        if (entry.isSame(clauses)) {
          // limits are the same so short circuit can occur as this new entry is a duplicate
          if (entry.limit == newEntry.limit) return entry;
          // if the new entry has a higher limit than the existing entry it is covered
          else if (entry.limit == -1 && newEntry.limit > 0) {
            newEntry.isCovered = entry;
            entry.covers.add(newEntry);
            // if the new entry has a higher limit then the original entry is covered by the new
            // entry
          } else if (entry.limit > 0 && newEntry.limit > 0 && entry.limit < newEntry.limit) {
            entry.isCovered = newEntry;
            newEntry.covers.add(entry);
          }
        }

        // check if the new entry is covered by a set of clauses that is a super set
        if (newEntry.isCovered == null && entry.isSuperSet(clauses)) {
          newEntry.isCovered = entry;
          entry.covers.add(newEntry);
        }

        // check if the current entry has a subset of clauses to the entry, then that entry is
        // covered
        if (entry.isCovered == null && entry.isSubSet(clauses)) {
          entry.isCovered = newEntry;
          newEntry.covers.add(entry);
        }
      }

      entryList.add(newEntry);
    }

    // process the entry (determine if cache miss is applicable)
    return this.processEntry(newEntry);
  }

  /**
   * This function is used to determine if a cache miss has occured
   *
   * @param newEntry entry to process
   * @return entry
   */
  private QueryCacheEntry processEntry(final QueryCacheEntry newEntry) {

    // TODO add entry to DB (of your parent)
    try {

      // If the entry isn't covered add the entry to your parents cache (or your local nodes cache)
      if (PathStoreProperties.getInstance().role != Role.ROOTSERVER && newEntry.isCovered == null) {

        System.out.println("Calling parent for non-covered cache miss");

        // call update cache on parent node
        PathStoreServerClient.getInstance().updateCache(newEntry);

        // when the entry
        if (PathStoreProperties.getInstance().role == Role.SERVER) {
          fetchDelta(newEntry);
        }
      }
    } finally {
      newEntry.setReady();
    }

    return newEntry;
  }

  /**
   * This function is used to write all updates to the view_table for a given table that is newer
   * then a parentTimestamp
   *
   * @param keyspace keyspace for entry
   * @param table table for entry
   * @param clausesSerialized clauses
   * @param parentTimestamp timestamp of the current entry's latest parent timestamp
   * @param nodeID node id that this request is coming from, this is to exclude rows that were
   *     pushed using
   * @param limit how many rows can be processed
   * @return uuid of delta used in {@link #fetchDelta(QueryCacheEntry)}
   * @throws IOException reading from bytes
   * @throws ClassNotFoundException can't find class needed
   */
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

    // ensure that the passed entry exists within your cache
    this.updateCache(keyspace, table, clausesSerialized, limit);

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

      // used to account for limits
      int count = -1;

      int totalRowsChanged = 0;
      for (Row row : results) {
        currentKey = row.getObject(primary);
        if (!currentKey.equals(previousKey)) count++;
        if (count >= limit) break;

        if (row.getInt(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_NODE) == nodeID
            || row.getUUID(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP).timestamp()
                <= parentTimestamp.timestamp()) continue;

        totalRowsChanged++;
        Insert insert = QueryBuilder.insertInto(keyspace, Constants.VIEW_PREFIX + table);

        insert.value(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VIEW_ID, deltaID);

        // Hossein
        for (Column column : columns) {
          if (!row.isNull(column.column_name)
              && column.column_name.compareTo(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY)
                  != 0) insert.value(column.column_name, row.getObject(column.column_name));
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

        previousKey = currentKey;
      }
      if (batchSize > 0) local.execute(batch);

      if (totalRowsChanged == 0) return null;

      return deltaID;
    } finally {
      // local.close();
    }
  }

  /**
   * This function is used to fetch a delta on a given entry. As in it will grab all updates that
   * pertain to a query
   *
   * @param entry entry to get updates for.
   */
  public void fetchDelta(QueryCacheEntry entry) {
    UUID deltaId = null;

    // the parentTimeStamp is only present after the initial fetch
    if (entry.getParentTimeStamp() != null) {
      deltaId = PathStoreServerClient.getInstance().createQueryDelta(entry);

      // if no new rows were written to the view table return as there is no data to fetch
      if (deltaId == null) return;
    }

    // fetch Data from parent
    fetchData(entry, deltaId);
  }

  /**
   * If deltaID is null then this is the first time data is being pulled. When this occurs
   *
   * @param entry
   * @param deltaID
   */
  private void fetchData(QueryCacheEntry entry, UUID deltaID) {
    Session parent = PathStorePrivilegedCluster.getParentInstance().connect();
    Session local = PathStorePrivilegedCluster.getDaemonInstance().connect();

    String table = deltaID != null ? Constants.VIEW_PREFIX + entry.table : entry.table;

    // select all from the table
    Select select = QueryBuilder.select().all().from(entry.keyspace, table);

    select.allowFiltering();

    // if the deltaID exists pull only rows with that view id
    if (deltaID != null)
      select.where(QueryBuilder.eq(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_VIEW_ID, deltaID));

    // add all additional clauses from the entry
    for (Clause clause : entry.clauses) select.where(clause);

    // hossein here again
    select.setFetchSize(1000);

    // execute the query on the parent node
    ResultSet results = parent.execute(select);

    Collection<Column> columns =
        SchemaInfo.getInstance().getTableColumns(entry.keyspace, entry.table);

    Batch batch = QueryBuilder.batch();

    int batchSize = 0;

    UUID highest_timestamp = null;

    // iterate over each returned row
    for (Row row : results) {

      // insert object
      Insert insert = QueryBuilder.insertInto(entry.keyspace, entry.table);

      for (Column column : columns) {
        if (column.column_name.compareTo(
                Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP)
            == 0) { // used to calculate the highest time stamp to set
          UUID row_timestamp =
              row.getUUID(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP);

          if (highest_timestamp == null
              || highest_timestamp.timestamp() < row_timestamp.timestamp())
            highest_timestamp = row_timestamp;

          insert.value(
              Constants.PATHSTORE_META_COLUMNS.PATHSTORE_PARENT_TIMESTAMP, QueryBuilder.now());
        } else {
          // for all other columns except for dirty add them to the insert value
          try {
            if (column.column_name.compareTo(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY) != 0
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

      // either add the statement to the batch or execute the insert locally.
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

    // if the batch still has data execute the rest of the batch
    if (batchSize > 0) local.execute(batch);

    // update the entry's timestamp to the highest timestamp from the data provided
    UUID entry_timestamp = entry.getParentTimeStamp();

    assert (entry_timestamp == null || entry_timestamp.timestamp() < highest_timestamp.timestamp());

    entry.setParentTimeStamp(highest_timestamp);
  }
}
