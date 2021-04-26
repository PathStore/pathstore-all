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
package pathstore.sessions;

import pathstore.common.PathStoreProperties;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is to be used on the client side to store their session tokens in a file which can be
 * used to when an application will migrate from one node to another.
 */
public class PathStoreSessionManager {

  /** Logger for information during dump */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreSessionManager.class);

  /** Instance of session manager (one instance per client) */
  private static PathStoreSessionManager instance = null;

  /**
   * This function should be called on start of your application if you plan to have sessions
   * consistency within your application
   *
   * @param sessionFile where you want your sessions to be stored, and or where to load previous
   *     sessions from
   */
  private static void init(final String sessionFile) {
    if (sessionFile == null)
      throw new RuntimeException(
          "You have not set the sessionFile properties in the properties file");

    if (instance == null) instance = new PathStoreSessionManager(sessionFile);
  }

  /** @return get session manager instance */
  public static synchronized PathStoreSessionManager getInstance() {
    if (instance == null) init(PathStoreProperties.getInstance().sessionFile);
    return instance;
  }

  /** File name where session storage exists or will exist */
  private final String sessionFile;

  /**
   * Store all sessions that are keyspace based tokens (when migrated all data within a keyspace is
   * transferred)
   */
  private final ConcurrentMap<String, SessionToken> sessionStore = new ConcurrentHashMap<>();

  /**
   * Loads all session tokens in from the given file (if present)
   *
   * @param sessionFile session file to load sessions in from and to store to
   */
  private PathStoreSessionManager(final String sessionFile) {
    this.sessionFile = sessionFile;

    if (PathStoreProperties.getInstance().NodeID == -1)
      throw new RuntimeException(
          "Node id gathered from local node returned -1, sessions will not be migrateable");

    this.loadFromFile();
  }

  /**
   * This function is used to gather a token with a keyspace granularity. If the token doesn't
   * already exist a fresh token will be generated
   *
   * @param sessionName session name of keyspace session
   * @return existing session token or newly generated session token
   */
  public SessionToken getKeyspaceToken(final String sessionName) {
    return getOrGenerateToken(sessionName, SessionType.KEYSPACE);
  }

  /**
   * This function is used to gather a token with a table granularity. If the token doesn't already
   * exist a fresh token will be generated
   *
   * @param sessionName session name of table session
   * @return existing session token or newly generated session token
   */
  public SessionToken getTableToken(final String sessionName) {
    return getOrGenerateToken(sessionName, SessionType.TABLE);
  }

  /**
   * This function is used to remove a session token from the session store.
   *
   * @param sessionTokenName session Token name to remove from the session store
   */
  public void removeToken(final String sessionTokenName) {
    this.sessionStore.remove(sessionTokenName);
  }

  /**
   * This function is called on initialization of the session manager to load any sessions from a
   * previously created session file. All migration of previous sessions will occur here.
   */
  private void loadFromFile() {
    File sessionFile = new File(this.sessionFile);

    try {
      BufferedReader sessionFileReader = new BufferedReader(new FileReader(sessionFile));

      String tokenJsonString;
      while ((tokenJsonString = sessionFileReader.readLine()) != null) {
        SessionToken tempToken = SessionToken.buildFromJsonString(tokenJsonString);
        if (tempToken != null) this.sessionStore.put(tempToken.sessionName, tempToken);
      }

      sessionFileReader.close();

      logger.info(
          String.format(
              "Loaded %d sessions from session file %s",
              this.sessionStore.size(), this.sessionFile));
    } catch (FileNotFoundException ignored) {
      logger.info(
          String.format("%s does not exist so no sessions were loaded in", this.sessionFile));
    } catch (IOException e) {
      logger.error(e);
      this.sessionStore.clear();
    }
  }

  /**
   * This function is used to get or generated a token
   *
   * @param sessionName name of session to get or generate
   * @param sessionType type of session (if needed to be generated)
   * @return previously existing session or freshly generated session
   */
  private SessionToken getOrGenerateToken(final String sessionName, final SessionType sessionType) {

    String transformedSessionName = sessionName.concat("-").concat(sessionType.toString());

    SessionToken token = this.sessionStore.get(transformedSessionName);

    if (token == null) {
      SessionToken temp =
          new SessionToken(
              PathStoreProperties.getInstance().NodeID, transformedSessionName, sessionType);
      this.sessionStore.put(transformedSessionName, temp);
      token = temp;
    }

    return token;
  }

  /**
   * This function is used to take a snapshot of all sessions in memory and dump it to a file.
   *
   * @apiNote Note that when a swap occurs it will override anything in the local session file
   * @implNote Since PathStore is dockerized this file name should be an absolute path and for ease
   *     of use should also be related to a virtual directory to the host machine for access.
   * @throws IOException if creation of file fails
   */
  public void swap() throws IOException {
    File sessionFile = new File(this.sessionFile);

    if (sessionFile.createNewFile())
      logger.info(String.format("Session file created with path %s", sessionFile));
    else logger.info(String.format("Session file exists with path %s", sessionFile));

    FileWriter sessionFileWriter = new FileWriter(sessionFile, false);

    // write sessions to file
    this.sessionStore
        .values()
        .forEach(
            sessionToken -> {
              try {
                sessionFileWriter.write(sessionToken.exportToJson().concat("\n"));
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    sessionFileWriter.close();

    logger.info(
        String.format("Swap operation successfully performed on file %s", this.sessionFile));
  }

  /**
   * Used on shutdown of client
   *
   * <p>This will perform a final swap and then reset the instance.
   *
   * @throws IOException {@link #swap()} for reason
   */
  public void close() throws IOException {
    this.swap();

    instance = null;
  }
}
