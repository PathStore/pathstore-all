package pathstore.system.garbagecollection;

import com.datastax.driver.core.Session;
import lombok.RequiredArgsConstructor;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** This interface is used to denote the structure of a garbage collection strategy. */
public interface PathStoreGarbageCollection {
  /**
   * This function is used to remove data from the database that is expired
   *
   * @param expired all expired entries keyspace -> table -> list of entries
   * @param notExpired all not expired entries keyspace -> table -> list of entries
   * @param rawSession raw session to cassandra
   */
  void garbageCollect(
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> expired,
      final ConcurrentMap<String, ConcurrentMap<String, List<QueryCacheEntry>>> notExpired,
      final Session rawSession);

  /**
   * This class is used to denote how to invoke the {@link PathStoreGarbageCollection} impl through
   * the {@link QueryCache}
   */
  @RequiredArgsConstructor
  class Executor implements Runnable {

    /** Denotes what the current status of the garbage collection service */
    private final AtomicBoolean ready = new AtomicBoolean(true);

    /** Garbage collection service instance */
    private final PathStoreGarbageCollection garbageCollectionService;

    /** @return value of {@link #ready} */
    private boolean isReady() {
      return this.ready.get();
    }

    /**
     * If the service is ready:
     *
     * <ul>
     *   <li>Set to false
     *   <li>call {@link QueryCache#handleExpiredEntries(PathStoreGarbageCollection)}
     *   <li>Set to true
     * </ul>
     */
    @Override
    public void run() {
      if (this.isReady()) {
        this.ready.set(false);

        QueryCache.getInstance().handleExpiredEntries(this.garbageCollectionService);

        this.ready.set(true);
      }
    }
  }
}
