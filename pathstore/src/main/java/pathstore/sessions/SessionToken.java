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

import com.google.protobuf.ProtocolStringList;
import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstore.grpc.pathStoreProto;
import pathstore.system.PathStorePushServer;
import pathstore.system.network.NetworkImpl;
import pathstore.util.SchemaInfo;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pathstore.util.SchemaInfo.Table;

/**
 * This class is used to denote a session token. Tokens will solely be managed by {@link
 * PathStoreSessionManager} and the user should not concern themselves with this class other then
 * the json dump that the session manager provides for migration of terminated clients
 */
public class SessionToken implements Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = -2763727974755711499L;

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
   * This is only used to generate a session token from a grpc session token
   *
   * @param sessionUUID {@link #sessionUUID}
   * @param sourceNode {@link #sourceNode}
   * @param sessionName {@link #sessionName}
   * @param sessionType {@link #sessionType}
   * @param data {@link #data}
   */
  private SessionToken(
      final String sessionUUID,
      final int sourceNode,
      final String sessionName,
      final String sessionType,
      final ProtocolStringList data) {
    this.sessionUUID = UUID.fromString(sessionUUID);
    this.sourceNode = sourceNode;
    this.sessionName = sessionName;
    this.sessionType = SessionType.valueOf(sessionType);
    this.data = NetworkImpl.GRPCRepeatedToCollection(data, Collectors.toSet());
    this.hasBeenValidated = true;
  }

  /**
   * @param grpcSessionToken grpc session token
   * @return session token from data
   */
  public static SessionToken fromGRPCSessionTokenObject(
      final pathStoreProto.SessionToken grpcSessionToken) {
    return new SessionToken(
        grpcSessionToken.getSessionUUID(),
        grpcSessionToken.getSourceNode(),
        grpcSessionToken.getSessionName(),
        grpcSessionToken.getSessionType(),
        grpcSessionToken.getDataList());
  }

  /** @return grpc session token object from local data */
  public pathStoreProto.SessionToken toGRPCSessionToken() {
    return pathStoreProto
        .SessionToken
        .newBuilder()
        .setSessionUUID(this.sessionUUID.toString())
        .setSourceNode(this.sourceNode)
        .setSessionName(this.sessionName)
        .setSessionType(this.sessionType.toString())
        .addAllData(this.data)
        .build();
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
   * This function is used to build a stream of table objects from the data set.
   *
   * <p>This gets used during the migration process.
   *
   * @return stream of tables determine by data set and session type
   */
  public Stream<Table> stream() {
    switch (this.sessionType) {
      case KEYSPACE:
        return this.data.stream()
            .map(keyspace -> SchemaInfo.getInstance().getTablesFromKeyspace(keyspace))
            .flatMap(Collection::stream)
            .filter(PathStorePushServer.filterOutViewAndLocal);
      case TABLE:
        return this.data.stream()
            .map(
                entry -> {
                  int locationOfPeriod = entry.indexOf('.');
                  return SchemaInfo.getInstance()
                      .getTableFromKeyspaceAndTableName(
                          entry.substring(0, locationOfPeriod),
                          entry.substring(locationOfPeriod + 1));
                })
            .filter(PathStorePushServer.filterOutViewAndLocal);
      default:
        throw new RuntimeException("Session Type is not either keyspace or table");
    }
  }

  /**
   * @return json export of all data within this session, this is used for storage of session in a
   *     central file for termination of child but migration is still possible when child comes
   *     alive on a new destination node
   */
  public String exportToJson() {
    JSONObject json = new JSONObject();

    json.put(Constants.SESSION_TOKEN.SESSION_UUID, this.sessionUUID.toString());

    json.put(Constants.SESSION_TOKEN.SOURCE_NODE, this.sourceNode);

    json.put(Constants.SESSION_TOKEN.SESSION_NAME, this.sessionName);

    json.put(Constants.SESSION_TOKEN.SESSION_TYPE, this.sessionType.toString());

    JSONArray data = new JSONArray();

    this.data.forEach(data::put);

    json.put(Constants.SESSION_TOKEN.DATA, data);

    return json.toString();
  }

  /**
   * @param sessionTokenString session Token json string
   * @return generated session token object or null if all keys aren't present
   */
  public static SessionToken buildFromJsonString(final String sessionTokenString) {
    JSONObject sessionObject = new JSONObject(sessionTokenString);

    return sessionObject.has(Constants.SESSION_TOKEN.SESSION_UUID)
            && sessionObject.has(Constants.SESSION_TOKEN.SOURCE_NODE)
            && sessionObject.has(Constants.SESSION_TOKEN.SESSION_NAME)
            && sessionObject.has(Constants.SESSION_TOKEN.SESSION_TYPE)
            && sessionObject.has(Constants.SESSION_TOKEN.DATA)
        ? new SessionToken(
            UUID.fromString(sessionObject.getString(Constants.SESSION_TOKEN.SESSION_UUID)),
            sessionObject.getInt(Constants.SESSION_TOKEN.SOURCE_NODE),
            sessionObject.getString(Constants.SESSION_TOKEN.SESSION_NAME),
            SessionType.valueOf(sessionObject.getString(Constants.SESSION_TOKEN.SESSION_TYPE)),
            new HashSet<>(
                sessionObject.getJSONArray(Constants.SESSION_TOKEN.DATA).toList().stream()
                    .map(Objects::toString)
                    .collect(Collectors.toSet())))
        : null;
  }
}
