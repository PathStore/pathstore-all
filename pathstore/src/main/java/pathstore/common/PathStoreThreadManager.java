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
package pathstore.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class manages all the threads associated with pathstore. Daemon threads are controlled using
 * {@link #daemonInstance} where as other jobs that will eventually terminate are handled through
 * {@link #subProcessInstance}
 */
public class PathStoreThreadManager {

  /** Daemon instance. Only set once */
  private static PathStoreThreadManager daemonInstance = null;

  /** @return daemon instance */
  public static synchronized PathStoreThreadManager getDaemonInstance() {
    if (daemonInstance == null)
      daemonInstance = new PathStoreThreadManager(Executors.newFixedThreadPool(5));
    return daemonInstance;
  }

  /** Sub process instance. Only set once */
  private static PathStoreThreadManager subProcessInstance = null;

  /** @return sub process instance */
  public static synchronized PathStoreThreadManager subProcessInstance() {
    if (subProcessInstance == null)
      subProcessInstance = new PathStoreThreadManager(Executors.newCachedThreadPool());
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
