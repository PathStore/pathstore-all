package pathstore.system.garbagecollection;

import pathstore.common.QueryCacheEntry;

import java.util.List;

/** This interface is used to denote the structure of a garbage collection strategy. */
public interface PathStoreGarbageCollection {
  /**
   * This function is used to remove data from the database
   *
   * @param expired all expired entries
   * @param notExpired all not expired entries
   * @apiNote  the union of expired and notExpired should equal the entire cache.
   */
  void garbageCollect(final List<QueryCacheEntry> expired, final List<QueryCacheEntry> notExpired);
}
