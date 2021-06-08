/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package pathstore.system;

import com.datastax.driver.core.Statement;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreThreadManager;
import pathstore.common.QueryCache;
import pathstore.common.QueryCacheEntry;
import pathstore.sessions.SessionToken;
import pathstore.system.garbagecollection.PathStoreGarbageCollection;
import pathstore.system.garbagecollection.SimpleGarbageCollector;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.util.UUID;

/**
 * This class is used ran as a daemon on every server except for the root node.
 *
 * <p>Its sole purpose is to periodically fetch deltas for all non-covered querycache entries.
 *
 * <p>This is to "update" our local nodes data set in an eventually consistent manner.
 *
 * @see pathstore.common.PathStoreThreadManager
 */
public class PathStorePullServer implements Runnable {

  /** Logger to handle errors */
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStorePullServer.class);

  /** Garbage collection service impl */
  private final PathStoreGarbageCollection garbageCollectionService = new SimpleGarbageCollector();

  /** Garbage collection executor service */
  private final PathStoreGarbageCollection.Executor garbageCollectionExecutorService =
      new PathStoreGarbageCollection.Executor(this.garbageCollectionService);

  /**
   * First we try to spawn an garbage collection executor service
   *
   * <p>For all entries in the qc that are ready, aren't covered, and aren't expired fetch their
   * delta.
   *
   * <p>Entries are added to the qc by {@link pathstore.client.PathStoreSession#execute(Statement)}
   * and {@link pathstore.client.PathStoreSession#execute(Statement, SessionToken)}
   *
   * @see QueryCache#fetchDelta(QueryCacheEntry)
   * @see QueryCache#createDelta(String, String, byte[], UUID, int, int)
   */
  private void pull() {

    PathStoreThreadManager.subProcessInstance().spawn(this.garbageCollectionExecutorService);

    QueryCache queryCache = QueryCache.getInstance();

    queryCache.stream()
        .filter(entry -> entry.isReady() && entry.getIsCovered() == null && !entry.isExpired())
        .forEach(queryCache::fetchDelta);
  }

  /** Run the pull server ever delta T defined by PullSleep properties */
  public synchronized void run() {
    logger.info("Pull Server spawned");
    while (true) {
      try {
        this.pull();
        Thread.sleep(PathStoreProperties.getInstance().PullSleep);
      } catch (Exception e) {
        System.err.println("PathStorePullServer exception: " + e.toString());
        this.logger.error(e);
      }
    }
  }
}
