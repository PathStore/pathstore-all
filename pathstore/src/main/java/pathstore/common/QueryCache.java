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
import lombok.Getter;
import lombok.NonNull;
import pathstore.client.PathStoreServerClient;
import pathstore.sessions.SessionToken;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.garbagecollection.PathStoreGarbageCollection;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class has a two prong feature set. For Servers this is an in-memory high level
 * representation of the majority of data within the database (excluding non-covered local writes).
 * It is also used to fetch updates for data that is of interest.
 *
 * <p>For clients it is used a representation of what it's local node has within the database that
 * it can freely access (directly to cassandra without communicating with the local node).
 *
 * @see pathstore.client.PathStoreSession Client side usage
 * @see pathstore.system.PathStorePullServer Server side usage
 * @see pathstore.system.network.NetworkImpl for how this can be called from clients are servers.
 */
public class QueryCache {

  /** class logger */
  private static final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(QueryCache.class);

  /** Local instance of query cache */
  @Getter(lazy = true)
  private static final QueryCache instance = new QueryCache();

  /** All qc entries */
  private final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> entries =
      new ConcurrentHashMap<>();

  /**
   * @param entries entries to flat map
   * @return stream of qc entries from map
   */
  public static Stream<QueryCacheEntry> queryCacheEntryMapToStream(
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> entries) {
    return entries.values().stream()
        .parallel()
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(Collection::stream);
  }

  /**
   * This function is used to filter the entry set by some parameter per entry
   *
   * @param filterFunction how to filter the individual entries out
   * @return map from keyspace -> table -> list of qc entries
   */
  public ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> filterEntries(
      final Predicate<QueryCacheEntry> filterFunction) {
    return this.entries.entrySet().stream()
        .collect(
            Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry ->
                    entry.getValue().entrySet().stream()
                        .map(
                            innerEntry ->
                                new AbstractMap.SimpleEntry<>(
                                    innerEntry.getKey(),
                                    innerEntry.getValue().stream()
                                        .filter(filterFunction)
                                        .collect(Collectors.toList())))
                        .collect(
                            Collectors.toConcurrentMap(
                                AbstractMap.SimpleEntry::getKey,
                                AbstractMap.SimpleEntry::getValue))));
  }

  /** @return {@link #entries} as stream */
  public Stream<QueryCacheEntry> stream() {
    return queryCacheEntryMapToStream(this.entries);
  }

  /**
   * This function removes all entries from the qc for a keyspace, this is used on application
   * removal
   *
   * @param keyspace keyspace to remove
   * @see pathstore.system.schemaFSM.PathStoreSlaveSchemaServer
   */
  public void remove(final String keyspace) {
    this.entries.remove(keyspace);
  }

  /**
   * Add a keyspace to the cache if absent
   *
   * @param keyspace keyspace to add
   */
  private void addKeyspace(final String keyspace) {
    this.entries.putIfAbsent(keyspace, new ConcurrentHashMap<>());
  }

  /**
   * Add a keyspace and table to the cache if absent
   *
   * @param keyspace keyspace to add
   * @param table table to add
   */
  private void addTable(final String keyspace, final String table) {
    addKeyspace(keyspace);
    // Myles: This may cause issues. We very well might need to convert this to a concurrent set or
    // queue.
    this.entries.get(keyspace).putIfAbsent(table, Collections.synchronizedList(new ArrayList<>()));
  }

  /**
   * This function is used to remove a queryCacheEntry from the entries list
   *
   * @param queryCacheEntry entry to remove
   */
  public void remove(final QueryCacheEntry queryCacheEntry) {
    if (queryCacheEntry != null)
      if (queryCacheEntry.keyspace != null && queryCacheEntry.table != null) {

        // set the status to removed for the entry if the status is removing (it will only be
        // removing on the server side and the server side will only be waiting, this never occurs
        // on the client side)
        if (queryCacheEntry.isRemoving()) queryCacheEntry.setRemoved();

        // remove entry from cache
        this.entries
            .getOrDefault(queryCacheEntry.keyspace, new ConcurrentHashMap<>())
            .getOrDefault(queryCacheEntry.table, new ArrayList<>())
            .remove(queryCacheEntry);
      }
  }

  /**
   * This function will return a set of querycache entries for a table object
   *
   * @param table table object to gather from
   * @return set of entries if any, always non-null.
   * @see pathstore.system.network.NetworkImpl#forceSynchronize(SessionToken, int)
   */
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

  /**
   * This function is used to handle all expired entries in bulk on the server side.
   *
   * @param garbageCollectionStrategy strategy used for garbage collection
   */
  public void handleExpiredEntries(
      @NonNull final PathStoreGarbageCollection garbageCollectionStrategy) {
    long current = System.currentTimeMillis();
    Date date = new Date(current);

    logger.info(String.format("Garbage collecting data as of time %s", date));

    ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> expired =
        this.filterEntries(
            entry -> entry.isExpired() && entry.isReady() && entry.getIsCovered() == null);

    ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> notExpired =
        this.filterEntries(
            entry -> !entry.isExpired() && entry.isReady() && entry.getIsCovered() == null);

    garbageCollectionStrategy.garbageCollect(
        expired, notExpired, PathStorePrivilegedCluster.getDaemonInstance().rawConnect());

    logger.info(String.format("Garbage collected data as of time %s", date));
  }

  /**
   * This function is used to update the cache from a child node.
   *
   * <p>The logic is, if an entry isn't present in the cache, add it to the cache. Then if it is
   * non-covered, inform the next node in the hierarchy that it must add this entry aswell (this
   * node's parent)
   *
   * @param keyspace keyspace of the query
   * @param table table of the query
   * @param clausesSerialized serialized clause set
   * @param limit return limit
   * @return entry created
   * @throws ClassNotFoundException for de-serialization failure
   * @throws IOException for de-serialization failure
   * @implNote If we get an entry that is {@link QueryCacheEntry.Status#REMOVING} it is in the
   *     process of being garbage collected. Since we cannot halt this process we must wait for it
   *     to be complete and then re-add the entry.
   */
  public QueryCacheEntry updateCache(
      final String keyspace, final String table, final byte[] clausesSerialized, final int limit)
      throws ClassNotFoundException, IOException {

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(clausesSerialized);
    ObjectInputStream ois = new ObjectInputStream(bytesIn);
    List<Clause> clauses = (List<Clause>) ois.readObject();

    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry == null || entry.isRemoving()) {

      // if the entry is removing then wait until it is removed
      if (entry != null) entry.waitUntilRemoved();

      entry = addEntry(keyspace, table, clauses, clausesSerialized, limit);
    }

    entry.waitUntilReady();

    return entry;
  }

  /**
   * This function is used to update the cache when an incoming select statement comes in.
   *
   * <p>This function is called by the client.
   *
   * <p>The logic is, if an entry isn't present in the cache, add it to the cache. Then if it is
   * non-covered, inform the local node that you're interested in the data.
   *
   * @param keyspace keyspace of the query
   * @param table table of the query
   * @param clauses where statements for the query
   * @param limit return limit
   * @return entry created.
   */
  public QueryCacheEntry updateCache(
      final String keyspace, final String table, final List<Clause> clauses, final int limit) {
    QueryCacheEntry entry = getEntry(keyspace, table, clauses, limit);

    if (entry == null || entry.isExpired()) {

      // remove the entry from the cache if the entry is expired
      if (entry != null) {
        logger.debug(String.format("%s was expired, removing", entry));
        this.remove(entry);
      }

      entry = addEntry(keyspace, table, clauses, null, limit);
    }

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
   * @implNote We update the lease time here if the role is not the client because this function is
   *     only ever called by an update cache call
   */
  public QueryCacheEntry getEntry(
      final String keyspace, final String table, final List<Clause> clauses, final int limit) {

    ConcurrentMap<String, List<QueryCacheEntry>> tableMap = this.entries.get(keyspace);
    if (tableMap == null) return null;

    List<QueryCacheEntry> entryList = tableMap.get(table);
    if (entryList == null) return null;

    for (QueryCacheEntry e : entryList)
      if (e.isSame(clauses))
        if (e.limit == -1
            || e.limit > limit) // we already have a bigger query so don't add this one
        {
          // reset the lease on any getCache call on the server side.
          if (PathStoreProperties.getInstance().role != Role.CLIENT) {
            e.resetExpirationTime();
            if (!e.keyspace.equals(Constants.PATHSTORE_APPLICATIONS))
              logger.debug(String.format("Updated %s", e));
          }

          return e;
        }

    return null;
  }

  /**
   * This function is used to add an entry to the cache. It will also calculate if this entry is
   * covered or not.
   *
   * @param keyspace keyspace of entry
   * @param table table of entry
   * @param clauses clauses of entry
   * @param clausesSerialized serialized clauses if applicable
   * @param limit limit of query
   * @return entry created
   * @see #processEntry(QueryCacheEntry) for how this entry is processed after addition
   */
  private QueryCacheEntry addEntry(
      final String keyspace,
      final String table,
      final List<Clause> clauses,
      final byte[] clausesSerialized,
      final int limit) {

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
            newEntry.setIsCovered(entry);
            entry.getCovers().add(newEntry);
            // if the new entry has a higher limit then the original entry is covered by the new
            // entry
          } else if (entry.limit > 0 && newEntry.limit > 0 && entry.limit < newEntry.limit) {
            entry.setIsCovered(newEntry);
            newEntry.getCovers().add(entry);
          }
        }

        // check if the new entry is covered by a set of clauses that is a super set
        if (newEntry.getIsCovered() == null && entry.isSuperSet(clauses)) {
          newEntry.setIsCovered(entry);
          entry.getCovers().add(newEntry);
        }

        // check if the current entry has a subset of clauses to the entry, then that entry is
        // covered
        if (entry.getIsCovered() == null && entry.isSubSet(clauses)) {
          entry.setIsCovered(newEntry);
          newEntry.getCovers().add(entry);
        }
      }

      entryList.add(newEntry);
    }

    // process the entry (determine if cache miss is applicable)
    return this.processEntry(newEntry);
  }

  /**
   * This function is used to determine if a cache miss has occurred if it has occured called the
   * local node or parent node, if this is called on a server also fetch the delta for the new entry
   * once the parent has received this data
   *
   * @param newEntry entry to process
   * @return entry
   */
  private QueryCacheEntry processEntry(final QueryCacheEntry newEntry) {

    try {

      // If the entry isn't covered add the entry to your parents cache (or your local nodes cache)
      if (PathStoreProperties.getInstance().role != Role.ROOTSERVER
          && newEntry.getIsCovered() == null) {

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
      final String keyspace,
      final String table,
      final byte[] clausesSerialized,
      final UUID parentTimestamp,
      final int nodeID,
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

    // Myles: This is used during garbage collection to allow each node to garbage collect things
    // without needing to know what their children are interested in
    // ensure that the passed entry exists within your cache
    this.updateCache(keyspace, table, clausesSerialized, limit);

    Session local = PathStorePrivilegedCluster.getDaemonInstance().rawConnect();

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
  public void fetchDelta(final QueryCacheEntry entry) {
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
   * If deltaID is null then this is the first time data is being pulled. When this occurs all data
   * from the query is read from the data and transferred to this node.
   *
   * <p>Otherwise if deltaID is non-null then a create delta call was made that returned that
   * deltaID. Then instead of pulling from the regular table all data from view_$table_name is made
   * with the primary key being that deltaID. Then similarly that data is written to the local table
   *
   * @param entry entry to fetch data for
   * @param deltaID delta id of the primary key in view_$table, if null regular fetch is performed
   */
  private void fetchData(final QueryCacheEntry entry, final UUID deltaID) {
    Session parent = PathStorePrivilegedCluster.getParentInstance().rawConnect();
    Session local = PathStorePrivilegedCluster.getDaemonInstance().rawConnect();

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
