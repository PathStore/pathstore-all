package pathstore.system.network;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.json.JSONObject;
import pathstore.authentication.ApplicationCredential;
import pathstore.authentication.CassandraAuthenticationUtil;
import pathstore.authentication.ClientAuthenticationUtil;
import pathstore.authentication.credentials.ClientCredential;
import pathstore.authentication.CredentialCache;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreServerClient;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.QueryCache;
import pathstore.common.tables.DeploymentEntry;
import pathstore.common.tables.DeploymentProcessStatus;
import pathstore.common.tables.ServerEntry;
import pathstore.sessions.SessionToken;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.PathStorePushServer;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is class is the implementation of {@link UnAuthenticatedServiceImpl}. That class is purely a
 * transportation wrapper
 */
public class NetworkImpl {
  /** Instance of class */
  private static NetworkImpl instance = null;

  /** @return instance of NetworkImpl */
  public static synchronized NetworkImpl getInstance() {
    if (instance == null) instance = new NetworkImpl();
    return instance;
  }

  /** Private Default Constructor */
  private NetworkImpl() {}

  /** Logger */
  private final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(NetworkImpl.class);

  /**
   * Update the parent / local node cache
   *
   * @param keyspace keyspace of the entry
   * @param table table of the entry
   * @param clauses clauses for the entry
   * @param limit limit on rows
   * @return test string
   */
  public String updateCache(
      final String keyspace, final String table, final byte[] clauses, final int limit) {

    try {
      QueryCache.getInstance().updateCache(keyspace, table, clauses, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }

    return "server says hello!";
  }

  /**
   * This function is used to create a delta for an entry on a parent node.
   *
   * @param keyspace keyspace of entry
   * @param table table of entry
   * @param clauses clauses of entry
   * @param parentTimestamp timestamp of entry, so only greater than is pulled
   * @param nodeID node id of caller (to not add rows pushed by the child)
   * @param limit limit on rows
   * @return delta uuid if rows were added, else null
   */
  public UUID createQueryDelta(
      final String keyspace,
      final String table,
      final byte[] clauses,
      final UUID parentTimestamp,
      final int nodeID,
      final int limit) {
    try {
      return QueryCache.getInstance()
          .createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param applicationName application name to register client for
   * @param password application password. This must be valid in comparison to the password given on
   *     application registration or after altering
   * @return JSON String up to 3 params, status, username, password. Response must check status
   *     before username and password as those fields may not exist
   * @see pathstore.client.PathStoreClientAuthenticatedCluster
   */
  @SneakyThrows
  public String registerApplicationClient(
      @NonNull final String applicationName, @NonNull final String password) {
    if (ClientAuthenticationUtil.isApplicationNotLoaded(applicationName)) {
      String errorResponse =
          String.format(
              "Registration of application credentials for application %s has failed as the provided application name is not loaded on the give node",
              applicationName);
      return new JSONObject()
          .put(
              Constants.REGISTER_APPLICATION.STATUS,
              Constants.REGISTER_APPLICATION.STATUS_STATES.INVALID)
          .put(Constants.REGISTER_APPLICATION.REASON, errorResponse)
          .toString();
    }

    Optional<ApplicationCredential> optionalApplicationCredential =
        ClientAuthenticationUtil.getApplicationCredentialRow(applicationName, password);

    if (!optionalApplicationCredential.isPresent()) {
      String errorResponse =
          String.format(
              "Registration of application credentials for application %s has failed as the provided credentials do not match the master application credentials",
              applicationName);
      return new JSONObject()
          .put(
              Constants.REGISTER_APPLICATION.STATUS,
              Constants.REGISTER_APPLICATION.STATUS_STATES.INVALID)
          .put(Constants.REGISTER_APPLICATION.REASON, errorResponse)
          .toString();
    }

    ApplicationCredential applicationCredential = optionalApplicationCredential.get();
    ClientCredential credential = CredentialCache.getClients().getCredential(applicationName);

    if (credential
        == null) { // Create a new client account if an account for that application doesn't already
      // exist
      String clientUsername =
          CassandraAuthenticationUtil.generateAlphaNumericPassword().toLowerCase();
      String clientPassword = CassandraAuthenticationUtil.generateAlphaNumericPassword();

      credential =
          new ClientCredential(
              applicationName, clientUsername, clientPassword, applicationCredential.isSuperUser());

      CredentialCache.getClients().add(credential);
    }

    return new JSONObject()
        .put(
            Constants.REGISTER_APPLICATION.STATUS,
            Constants.REGISTER_APPLICATION.STATUS_STATES.VALID)
        .put(Constants.REGISTER_APPLICATION.USERNAME, credential.getUsername())
        .put(Constants.REGISTER_APPLICATION.PASSWORD, credential.getPassword())
        .toString();
  }

  /**
   * This function is used by {@link PathStoreClientAuthenticatedCluster#getInstance()} schema info
   * for the client node.
   *
   * @param keyspace application associated with the client
   * @return schemainfo solely on that application
   * @see SchemaInfo#getSchemaPartition(String)
   */
  @SneakyThrows
  public SchemaInfo getSchemaInfo(final String keyspace) {
    return SchemaInfo.getInstance().getSchemaPartition(keyspace);
  }

  /**
   * This function is used to validate a session.
   *
   * <p>There are two cases that can occur within this function assuming the json string passed is a
   * valid session token.
   *
   * <p>1): The session provided originated from this node, thus we ensure its validity but no
   * migration occurs
   *
   * <p>2): The session provided originated from another node, we ensure its validity and if valid
   * we perform a data migration.
   *
   * <p>Migration Logic:
   *
   * <p>Let sessionToken.sourceNode = n_s and the current node id = n_d
   *
   * <p>1): Calculate LCA (Lowest common ancestor) which is defined as n_a
   *
   * <p>2): Force push all of K or T of the session from n_s to n_a
   *
   * <p>3): Force pull all of K or T of the session from n_a to n_d
   *
   * <p>Migration is then complete
   *
   * @param sessionToken from client
   * @return true if the session is valid, false if not valid
   */
  public boolean validateSession(final SessionToken sessionToken) {

    if (sessionToken != null) {

      // validity check of source node id is actually a source node
      Select selectNodeId =
          QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT);

      selectNodeId
          .where(QueryBuilder.eq(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID, sessionToken.sourceNode))
          .and(
              QueryBuilder.eq(
                  Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
                  DeploymentProcessStatus.DEPLOYED.toString()));

      selectNodeId.allowFiltering();

      if (PathStoreCluster.getDaemonInstance().connect().execute(selectNodeId).empty())
        return false;

      // validity check of data
      switch (sessionToken.sessionType) {
        case KEYSPACE:
          if (!SchemaInfo.getInstance().getLoadedKeyspaces().containsAll(sessionToken.getData()))
            return false;
          break;
        case TABLE:
          if (!SchemaInfo.getInstance().getLoadedKeyspaces().stream()
              .map(keyspace -> SchemaInfo.getInstance().getTablesFromKeyspace(keyspace))
              .flatMap(Collection::stream)
              .map(table -> String.format("%s.%s", table.keyspace_name, table.table_name))
              .collect(Collectors.toList())
              .containsAll(sessionToken.getData())) return false;
          break;
      }

      // perform migration
      if (sessionToken.sourceNode != PathStoreProperties.getInstance().NodeID) {
        // calculate LCA between sourceNode and NodeId to get N_A
        int lca = this.lca(sessionToken.sourceNode, PathStoreProperties.getInstance().NodeID);

        logger.info(
            String.format(
                "LCA of (%d, %d) is %d",
                sessionToken.sourceNode, PathStoreProperties.getInstance().NodeID, lca));

        Select querySourceNodeAddress =
            QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

        Optional<Row> deploymentRow =
            PathStoreCluster.getDaemonInstance().connect().execute(selectNodeId).stream()
                .findFirst();

        if (!deploymentRow.isPresent())
          throw new RuntimeException("Could not get deployment row for source node");

        querySourceNodeAddress.where(
            QueryBuilder.eq(
                Constants.SERVERS_COLUMNS.SERVER_UUID,
                deploymentRow.get().getString(Constants.DEPLOYMENT_COLUMNS.SERVER_UUID)));

        Optional<Row> optionalServerRow =
            PathStoreCluster.getDaemonInstance().connect().execute(querySourceNodeAddress).stream()
                .findFirst();

        if (!optionalServerRow.isPresent())
          throw new RuntimeException("Could not get server row for source node");

        Row serverRow = optionalServerRow.get();

        ServerEntry serverEntry = ServerEntry.fromRow(serverRow);

        PathStoreServerClient sourceNode =
            PathStoreServerClient.getCustom(serverEntry.ip, serverEntry.grpcPort);

        // force push all of K or T of session from sourceNode to lca if the sourceNode isn't the
        // lca
        if (sessionToken.sourceNode != lca) sourceNode.forcePush(sessionToken, lca);

        // force synchronize all of K or T of session from destination node to lca if the
        // destination node isn't the lca
        if (PathStoreProperties.getInstance().NodeID != lca)
          this.forceSynchronize(sessionToken, lca);
      }
      return true;
    }
    return false;
  }

  /**
   * This function is used to force push all data to the parent node recursively up to a node.
   *
   * <p>This is only used to migrate a session from one node to another.
   *
   * <p>This will recursively push data from n_s to lca. Once lca is hit, it will not push anymore
   * as the data is where it needs to be
   *
   * @param sessionToken session token to migrate
   * @param lca lca calculation from {@link #lca(int, int)} between n_s and n_d
   */
  public void forcePush(final SessionToken sessionToken, final int lca) {
    int nodeId = PathStoreProperties.getInstance().NodeID;
    if (nodeId != lca) {
      logger.info(
          String.format(
              "Performing a force push for session data on node %d with session name %s with lca of %d",
              nodeId, sessionToken.sessionName, lca));
      PathStorePushServer.push(
          sessionToken.stream().collect(Collectors.toList()),
          PathStorePrivilegedCluster.getDaemonInstance().connect(),
          PathStorePrivilegedCluster.getParentInstance().connect(),
          SchemaInfo.getInstance(),
          nodeId);

      PathStoreServerClient.getInstance().forcePush(sessionToken, lca);
    } else {
      logger.info(
          String.format(
              "LCA has been hit with nodeid %d and session name %s",
              lca, sessionToken.sessionName));
    }
  }

  /**
   * This function is used to force synchronize all caches between n_d and n_a
   *
   * @param sessionToken session token to migrate
   * @param lca lca of migration
   * @see SessionToken#stream()
   * @implNote The reason why we only process ready entries is because the only way an entry can be
   *     not ready is during its creation process. If a non-ready entry is non-covered, it will have
   *     to go to its parent, so if we wait for it to be complete we will pull double the amount of
   *     data. Thus we should only be processing entries that are ready which implies they have a
   *     parentTimestamp which means all fetchDelta calls will only be deltas and not entire
   *     datasets. Which in turn will speed up session migration time.
   */
  public void forceSynchronize(final SessionToken sessionToken, final int lca) {
    int nodeId = PathStoreProperties.getInstance().NodeID;
    if (nodeId != lca) {

      logger.info(String.format("Still haven't hit lca of %d", lca));

      PathStoreServerClient.getInstance().forceSynchronize(sessionToken, lca);

      logger.info(
          String.format("Starting synchronization of all session data for node id %d", lca));

      sessionToken.stream()
          .map(table -> QueryCache.getInstance().getEntries(table))
          .flatMap(Collection::stream)
          .filter(
              queryCacheEntry ->
                  queryCacheEntry.getIsCovered() == null
                      && queryCacheEntry
                          .isReady()) // only non-covered and ready entries, see implNote
          .forEach(
              nonCoveredQueryCacheEntry -> {
                logger.info(
                    String.format(
                        "Synchronizing entry on table %s.%s with clauses %s",
                        nonCoveredQueryCacheEntry.keyspace,
                        nonCoveredQueryCacheEntry.table,
                        nonCoveredQueryCacheEntry.clauses));
                QueryCache.getInstance().fetchDelta(nonCoveredQueryCacheEntry);
              });

    } else {
      logger.info(String.format("Hit lca of %d, not going any further", lca));
    }
  }

  /**
   * This function is used to calculate the lowest common ancestor between the sourceNode and the
   * destinationNode.
   *
   * <p>The time complexity for this function is O(h) where h is the height of the tree.
   *
   * <p>The Logic for this function is as follow:
   *
   * <p>Step 1) Build a map from child -> parent node.
   *
   * <p>Step 2)
   *
   * <p>Iterate from sourceNode to the root node and add all the nodes along the path to a marked
   * set.
   *
   * <p>Case 1: destinationNode is an ancestor of the source node, return the destination node as
   * the LCA.
   *
   * <p>Case 2: sourceNode and destinationNode are apart of two separate sub-trees.
   *
   * <p>Once the sourceNode traverses to the root node, then traverse up the tree starting from the
   * destination node. The first node hit is the LCA
   *
   * <p>Case 3: sourceNode is an ancestor of the destinationNode.
   *
   * <p>Since the sourceNode is traversed first, it will traverse to the root. Then the destination
   * will traverse up until it hits the sourceNode and that node will be the first marked node and
   * thus the sourceNode will return.
   *
   * @param sourceNode where the data originated
   * @param destinationNode where the data is going
   * @return lca.
   * @implNote the return will always be valid as the sourceNode and destinationNode to exist within
   *     the tree structure.
   */
  private int lca(final int sourceNode, final int destinationNode) {

    Select allDeployedNodes =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.DEPLOYMENT)
            .allowFiltering();
    allDeployedNodes.where(
        QueryBuilder.eq(
            Constants.DEPLOYMENT_COLUMNS.PROCESS_STATUS,
            DeploymentProcessStatus.DEPLOYED.toString()));

    // build child -> parent set from db.
    Map<Integer, Integer> childToParentMap =
        PathStoreCluster.getDaemonInstance().connect().execute(allDeployedNodes).stream()
            .map(DeploymentEntry::fromRow)
            .collect(Collectors.toMap(entry -> entry.newNodeId, entry -> entry.parentNodeId));

    Set<Integer> visitedSet = new HashSet<>();

    // traverse from the sourceNode to the root node and mark each node including the sourceNode as
    // visited
    for (int current = sourceNode; current != -1; current = childToParentMap.get(current)) {
      visitedSet.add(current);

      if (current == destinationNode) return current;
    }

    // traverse from the destinationNode up to the first visited node, the first met visited node
    // will be the lca.
    int current = destinationNode;
    while (!visitedSet.contains(current)) current = childToParentMap.get(current);

    return current;
  }

  /**
   * This function is used to gather the node of the local node for node_source in a session token.
   * Only called once by {@link PathStoreProperties#getInstance()}
   *
   * @return local node id
   */
  public int getLocalNodeId() {
    return PathStoreProperties.getInstance().NodeID;
  }
}
