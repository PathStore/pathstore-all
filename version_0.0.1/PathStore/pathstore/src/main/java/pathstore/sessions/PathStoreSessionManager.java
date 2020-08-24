package pathstore.sessions;

import pathstore.common.PathStoreProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is to be used on the client side to store their session tokens in a file which can be
 * used to when an application will migrate from one node to another.
 */
public class PathStoreSessionManager {

  /** Instance of session manager (one instance per client) */
  private static PathStoreSessionManager instance = null;

  /** @return get session manager instance */
  public static synchronized PathStoreSessionManager getInstance() {
    if (instance == null) instance = new PathStoreSessionManager();
    return instance;
  }

  /**
   * Store all sessions that are keyspace based tokens (when migrated all data within a keyspace is
   * transferred)
   */
  private final Map<String, SessionToken> keyspaceBasedTokens = new ConcurrentHashMap<>();

  /**
   * Stores all sessions that are table based tokens (when migrated all data within these tables are
   * transferred)
   */
  private final Map<String, SessionToken> tableBasedTokens = new ConcurrentHashMap<>();

  // TODO: Load sessions in from session files
  private PathStoreSessionManager() {}

  /**
   * This function is used to gather a token with a keyspace granularity. If the token doesn't
   * already exist a fresh token will be generated
   *
   * @param sessionName session name of keyspace session
   * @return existing session token or newly generated session token
   */
  public SessionToken getKeyspaceToken(final String sessionName) {
    return getOrGenerateToken(this.keyspaceBasedTokens, sessionName, SessionType.KEYSPACE);
  }

  /**
   * This function is used to gather a token with a table granularity. If the token doesn't already
   * exist a fresh token will be generated
   *
   * @param sessionName session name of table session
   * @return existing session token or newly generated session token
   */
  public SessionToken getTableToken(final String sessionName) {
    return getOrGenerateToken(this.tableBasedTokens, sessionName, SessionType.TABLE);
  }

  /**
   * This function is used to dump all session tokens to a file. This should be used when you plan
   * to kill a client and move it to a new destination node
   */
  public void dumpToFile() {
    // TODO:
  }

  /**
   * This function is used to get or generated a token for a specific session storage map.
   *
   * @param storage session storage map from sessionName -> Session Token
   * @param sessionName name of session to get or generate
   * @param sessionType type of session (if needed to be generated)
   * @return previously existing session or freshly generated session
   */
  private static SessionToken getOrGenerateToken(
      final Map<String, SessionToken> storage,
      final String sessionName,
      final SessionType sessionType) {

    SessionToken token = storage.get(sessionName);

    if (token == null) {
      SessionToken temp =
          new SessionToken(PathStoreProperties.getInstance().NodeID, sessionName, sessionType);
      storage.put(sessionName, temp);
      token = temp;
    }

    return token;
  }
}
