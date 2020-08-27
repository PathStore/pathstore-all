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
  public int sourceNode;

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
   * This boolean is used only in memory to denote if the stoken has been check pointed with the
   * local node at least once to confirm it does not need to be migrated
   */
  private boolean hasBeenValidated;

  /**
   * hasBeenValidated is set to true as this constructor is called when generated a fresh session
   * token for the given node.
   *
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
    this.hasBeenValidated = true;
  }

  /**
   * This constructor is used for building a session token object from a json string
   *
   * <p>hasBeenValidated is set to false as the session token was loaded in via a json string
   * implying it was loaded in from a file thus we can not guarantee the given session token was
   * generated on this node and potentially needs migration
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
    this.hasBeenValidated = false;
  }

  /**
   * @param entry entry to add to data list
   * @implNote Validity check is done during migration, not during insertion.
   */
  public void addEntry(final String entry) {
    this.data.add(entry);
  }

  /**
   * Used for validity check to determine if all inserted data is valid
   *
   * @return {@link #data}
   */
  public Collection<String> getData() {
    return this.data;
  }

  /** @return if this token has been validated or not */
  public boolean hasBeenValidated() {
    return this.hasBeenValidated;
  }

  /**
   * This function is used to update the validity of a session. It is also required to pass a new
   * source node id. This may be the original source node already existing, but if the source node
   * is a new node, as in the session has been migrated the source node must be updated for future
   * migrations
   *
   * @param newSourceNode source node to update to.
   */
  public void isValidated(final int newSourceNode) {
    this.hasBeenValidated = true;
    this.sourceNode = newSourceNode;
    System.out.println(String.format("Validated session token with name %s", this.sessionName));
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
   * @return generated session token object or null if all keys aren't present
   */
  public static SessionToken buildFromJsonString(final String sessionTokenString) {
    JSONObject sessionObject = new JSONObject(sessionTokenString);

    return sessionObject.has("sessionUUID")
            && sessionObject.has("sourceNode")
            && sessionObject.has("sessionName")
            && sessionObject.has("sessionType")
            && sessionObject.has("data")
        ? new SessionToken(
            UUID.fromString(sessionObject.getString("sessionUUID")),
            sessionObject.getInt("sourceNode"),
            sessionObject.getString("sessionName"),
            SessionType.valueOf(sessionObject.getString("sessionType")),
            new HashSet<>(
                sessionObject.getJSONArray("data").toList().stream()
                    .map(Objects::toString)
                    .collect(Collectors.toSet())))
        : null;
  }
}
