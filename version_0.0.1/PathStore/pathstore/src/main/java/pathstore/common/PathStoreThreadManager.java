package pathstore.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class manages all the threads associated with pathstore. Daemon threads are controlled using
 * {@link #daemonInstance} where as other jobs that will eventually terminate are handled through
 * {@link #subProcessInstance}
 */
public class PathStoreThreadManager {

  /** Lock for the creation of the daemon instance */
  private static final Object daemonLock = new Object();

  /** Daemon instance. Only set once */
  private static PathStoreThreadManager daemonInstance = null;

  /** @return daemon instance */
  public static PathStoreThreadManager getDaemonInstance() {
    synchronized (daemonLock) {
      if (daemonInstance == null)
        daemonInstance = new PathStoreThreadManager(Executors.newFixedThreadPool(5));
    }
    return daemonInstance;
  }

  /** Lock for the creation of the sub processes instance */
  private static final Object subProcessLock = new Object();

  /** Sub process instance. Only set once */
  private static PathStoreThreadManager subProcessInstance = null;

  /** @return sub process instance */
  public static PathStoreThreadManager subProcessInstance() {
    synchronized (subProcessLock) {
      if (subProcessInstance == null)
        subProcessInstance = new PathStoreThreadManager(Executors.newCachedThreadPool());
    }
    return subProcessInstance;
  }

  /**
   * @see #getDaemonInstance()
   * @see #subProcessInstance()
   */
  private final ExecutorService service;

  /** @param service thread pool service */
  private PathStoreThreadManager(final ExecutorService service) {
    this.service = service;
  }

  /**
   * @param runnable runnable to add to the thread pool
   * @return instance of this class to chain spawn command
   */
  public PathStoreThreadManager spawn(final Runnable runnable) {
    this.service.submit(runnable);
    return this;
  }
}