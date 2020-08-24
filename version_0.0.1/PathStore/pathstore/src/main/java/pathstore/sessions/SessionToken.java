package pathstore.sessions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to denote a session token. Tokens will solely be managed by {@link
 * PathStoreSessionManager} and the user should not concern themselves with this class other then
 * the json dump that the session manager provides for migration of terminated clients
 */
public class SessionToken {

  /** Random uuid for session */
  public final UUID sessionUUID;

  /** Where the session was originally. */
  public final int sourceNode;

  /** Name of session */
  public final String sessionName;

  /**
   * What granularity the session is
   *
   * @see SessionType
   */
  public final SessionType sessionType;

  /**
   * What data needs to be transferred (list of tables or keyspace)
   *
   * @see PathStoreSessionManager
   */
  private final Set<String> data;

  /**
   * @param sourceNode {@link #sourceNode}
   * @param sessionName {@link #sessionName}
   * @param sessionType {@link #sessionType}
   */
  protected SessionToken(
      final int sourceNode, final String sessionName, final SessionType sessionType) {
    this.sessionUUID = UUID.randomUUID();
    this.sourceNode = sourceNode;
    this.sessionName = sessionName;
    this.sessionType = sessionType;
    this.data = new HashSet<>();
  }

  /**
   * This constructor is used for building a session token object from a json string
   *
   * @param sessionUUID session uuid
   * @param sourceNode where the data originated from
   * @param sessionName session Name
   * @param sessionType session Type
   * @param data data associated with session
   */
  private SessionToken(
      final UUID sessionUUID,
      final int sourceNode,
      final String sessionName,
      final SessionType sessionType,
      final Set<String> data) {
    this.sessionUUID = sessionUUID;
    this.sourceNode = sourceNode;
    this.sessionName = sessionName;
    this.sessionType = sessionType;
    this.data = data;
  }

  /**
   * @param entry entry to add to data list
   * @apiNote Assumes validity of either a keyspace.table name or a keyspace name
   */
  public void addEntry(final String entry) {
    this.data.add(entry);
  }

  /**
   * @return json export of all data within this session, this is used for storage of session in a
   *     central file for termination of child but migration is still possible when child comes
   *     alive on a new destination node
   */
  public String exportToJson() {
    JSONObject json = new JSONObject();

    json.put("sessionUUID", this.sessionUUID.toString());

    json.put("sourceNode", this.sourceNode);

    json.put("sessionName", this.sessionName);

    json.put("sessionType", this.sessionType.toString());

    JSONArray data = new JSONArray();

    this.data.forEach(data::put);

    json.put("data", data);

    return json.toString();
  }

  /**
   * @param sessionTokenString session Token json string
   * @return generated session token object
   * @apiNote No validity check is performed on the string. Please ensure the file you're reading
   *     from was produced by {@link PathStoreSessionManager#close()}
   */
  protected static SessionToken buildFromJsonString(final String sessionTokenString) {
    JSONObject sessionObject = new JSONObject(sessionTokenString);

    return new SessionToken(
        UUID.fromString(sessionObject.getString("sessionUUID")),
        sessionObject.getInt("sourceNode"),
        sessionObject.getString("sessionName"),
        SessionType.valueOf(sessionObject.getString("sessionType")),
        new HashSet<>(
            sessionObject.getJSONArray("data").toList().stream()
                .map(Objects::toString)
                .collect(Collectors.toSet())));
  }
}
