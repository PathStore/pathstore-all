package pathstore.system.garbagecollection;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import pathstore.common.Constants;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.util.SchemaInfo;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class is a naive implementation of a garbage collector based on our interest based partially
 * replicated storage system.
 *
 * <p>Briefly: at time {@code t}
 *
 * <ul>
 *   <li>we calculate all expired and notExpired entries in the {@link QueryCache} that are ready
 *       and not covered.
 *   <li>we then set all expired entries to not ready so clients re-request the data
 *   <li>we then calculate the difference between {@code total \ notExpired} rows. This results in
 *       all rows that are in expired plus any writes the clients have made that are outside the
 *       bounds of any interest in the cache
 *   <li>we then remove all expired entries from the query cache (which sets them to removed).
 * </ul>
 */
public class SimpleGarbageCollector implements PathStoreGarbageCollection {

  /**
   * This function is used to garbage collect all expired data from the database
   *
   * @param expired all expired entries keyspace -> table -> list of entries
   * @param notExpired all not expired entries keyspace -> table -> list of entries
   * @param rawSession raw session to cassandra
   */
  @Override
  public void garbageCollect(
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> expired,
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> notExpired,
      final Session rawSession) {

    // set all expired entries to not ready
    this.bulkSetRemoving(expired);

    // remove all expired data that doesn't intersect with notExpired.
    for (String keyspace : notExpired.keySet()) {
      for (String table : notExpired.get(keyspace).keySet()) {

        Collection<QueryCacheEntry> expiredEntriesForKeyspaceTable =
            expired.get(keyspace).get(table);

        Collection<QueryCacheEntry> notExpiredEntriesForKeyspaceTable =
            notExpired.get(keyspace).get(table);

        Collection<PrimaryKey> primaryKeysToRemove =
            this.calculatePrimaryKeysForRemoval(
                notExpiredEntriesForKeyspaceTable, rawSession, keyspace, table);

        // removal all data from table
        this.removeData(primaryKeysToRemove, rawSession);

        // remove expired entries from querycache
        this.removeEntries(expiredEntriesForKeyspaceTable);
      }
    }
  }

  /**
   * This function is used to set all entries in a qc entry map to removing
   *
   * @param entries expired removing map
   */
  private void bulkSetRemoving(
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> entries) {
    QueryCache.queryCacheEntryMapToStream(entries).forEach(QueryCacheEntry::setRemoving);
  }

  /**
   * This function is used to calculate all primary key's that are expired that are not overlapping
   * with non-expired entries.
   *
   * <p>We have two Sets {@code total} and {@code notExpired}. First we query all elements of these
   * sets to produce a stream of {@link PrimaryKey} objects. This is a map from column_name ->
   * column_value.
   *
   * <p>We then compute the set difference between these {@code total \ notExpired} and return the
   * resulting primary key set.
   *
   * @param notExpired all querycache entries that are not expired at time of calling {@link
   *     pathstore.common.QueryCache#handleExpiredEntries(PathStoreGarbageCollection)}
   * @param rawSession raw session to local cassandra cluster
   * @param keyspace keyspace of all entries
   * @param table table of all entries
   * @return all primary keys for keyspace.table to remove. (Not present in notExpired entries)
   */
  private Collection<PrimaryKey> calculatePrimaryKeysForRemoval(
      final Collection<QueryCacheEntry> notExpired,
      final Session rawSession,
      final String keyspace,
      final String table) {

    Collection<QueryCacheEntry> total =
        Collections.singleton(new QueryCacheEntry(keyspace, table, Collections.emptyList(), -1));

    Collection<String> primaryKeyColumns =
        SchemaInfo.getInstance().getPrimaryColumnNames(keyspace, table);

    Stream<PrimaryKey> totalStream =
        this.psVersionStreamFromEntriesList(total, rawSession, primaryKeyColumns, keyspace, table);

    Stream<PrimaryKey> notExpiredStream =
        this.psVersionStreamFromEntriesList(
            notExpired, rawSession, primaryKeyColumns, keyspace, table);

    Set<PrimaryKey> notExpiredPrimaryKeySet = notExpiredStream.collect(Collectors.toSet());

    return totalStream
        .filter(expiredPrimaryKey -> !notExpiredPrimaryKeySet.contains(expiredPrimaryKey))
        .collect(Collectors.toSet());
  }

  /**
   * Removes all data from the primary keys to remove collection
   *
   * @param primaryKeysToRemove data to remove
   * @param rawSession session to remove from
   */
  public void removeData(
      final Collection<PrimaryKey> primaryKeysToRemove, final Session rawSession) {
    primaryKeysToRemove.stream().map(PrimaryKey::delete).forEach(rawSession::execute);
  }

  /**
   * Removes all entries from the querycache
   *
   * @param entriesPerKeyspaceTable entries to remove (expired entries)
   */
  public void removeEntries(final Collection<QueryCacheEntry> entriesPerKeyspaceTable) {
    entriesPerKeyspaceTable.forEach(
        queryCacheEntry -> QueryCache.getInstance().remove(queryCacheEntry));
  }

  /**
   * This function is used to convert an entry set to a stream of primary key objects. This is used
   * for difference calculations which are used to determine which rows should be removed
   *
   * @param entries entries for a specific keyspace.table
   * @param rawSession session
   * @param primaryKeyColumns primary key column names from {@link SchemaInfo}
   * @param keyspace keyspace
   * @param table table
   * @return stream of primary key objects from qc entry list
   */
  private Stream<PrimaryKey> psVersionStreamFromEntriesList(
      final Collection<QueryCacheEntry> entries,
      final Session rawSession,
      final Collection<String> primaryKeyColumns,
      final String keyspace,
      final String table) {
    return entries
        .parallelStream()
        .map(queryCacheEntry -> rawSession.execute(queryCacheEntry.select()))
        .flatMap(resultSet -> StreamSupport.stream(resultSet.spliterator(), true))
        .filter(row -> !row.getBool(Constants.PATHSTORE_META_COLUMNS.PATHSTORE_DIRTY))
        .map(
            row ->
                new PrimaryKey(
                    keyspace,
                    table,
                    primaryKeyColumns.stream()
                        .collect(Collectors.toMap(Function.identity(), row::getObject))));
  }

  /**
   * This class denotes a row in the database only consenting of user-defined primary key columns.
   *
   * <p>This allows for the creation of a delete statement for an arbitrary row.
   */
  @RequiredArgsConstructor
  @EqualsAndHashCode
  private static class PrimaryKey {
    /** Keyspace of row */
    private final String keyspace;

    /** Table of row */
    private final String table;

    /** Map of user defined primary column to data in row. */
    private final Map<String, Object> primaryColumnsMap;

    /** @return delete statement from data provided in object */
    public Delete delete() {
      Delete delete = QueryBuilder.delete().from(this.keyspace, this.table);

      this.primaryColumnsMap.forEach((key, value) -> delete.where(QueryBuilder.eq(key, value)));

      return delete;
    }
  }
}
