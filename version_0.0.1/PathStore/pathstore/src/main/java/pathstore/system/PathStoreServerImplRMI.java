package pathstore.system;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.json.JSONObject;
import pathstore.authentication.AuthenticationUtil;
import pathstore.authentication.ClientAuthenticationUtil;
import pathstore.authentication.Credential;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.sessions.SessionToken;
import pathstore.system.deployment.deploymentFSM.DeploymentProcessStatus;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.SchemaInfo;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class PathStoreServerImplRMI implements PathStoreServer {
  private final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreServerImplRMI.class);

  /** Static instance so that it doesn't get gc'd */
  private static PathStoreServerImplRMI instance;

  /** @return instance of rmi server */
  public static PathStoreServerImplRMI getInstance() {
    if (instance == null) instance = new PathStoreServerImplRMI();
    return instance;
  }

  public String addQueryEntry(String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException {
    long d = System.nanoTime();
    try {
      QueryCache.getInstance().updateCache(keyspace, table, clauses, limit);
      //			System.out.println("^^^^^^^^^^^^^^^^ time to reply took: " + Timer.getTime(d));

    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }

    return "server says hello!";
  }

  @Override
  public UUID createQueryDelta(
      String keyspace, String table, byte[] clauses, UUID parentTimestamp, int nodeID, int limit)
      throws RemoteException {
    try {
      return QueryCache.getInstance()
          .createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
    }
  }

  /**
   * TODO: State reason why status is invalid
   *
   * @param applicationName application name to register client for
   * @param password application password. This must be valid in comparison to the password given on
   *     application registration or after altering
   * @return JSON String up to 3 params, status, username, password. Response must check status
   *     before username and password as those fields may not exist
   * @see pathstore.client.PathStoreClientAuthenticatedCluster
   */
  @Override
  public String registerApplication(final String applicationName, final String password) {
    logger.info(
        String.format("Register application credentials for application %s", applicationName));

    if (ClientAuthenticationUtil.isComboInvalid(applicationName, password)) {
      logger.info(
          String.format(
              "Registration of application credentials for application %s has failed as the provided credentials do not match the master application credentials",
              applicationName));
      return new JSONObject().put("status", "invalid").toString();
    }

    if (ClientAuthenticationUtil.isApplicationNotLoaded(applicationName)) {
      logger.info(
          String.format(
              "Registration of application credentials for application %s has failed as the provided application name is not loaded on the give node",
              applicationName));
      return new JSONObject().put("status", "invalid").toString();
    }

    Optional<Credential> optionalExistingCredential =
        ClientAuthenticationUtil.getExistingClientAccount(applicationName);

    String clientUsername, clientPassword;

    if (optionalExistingCredential.isPresent()) {
      Credential existingCredential = optionalExistingCredential.get();

      clientUsername = existingCredential.username;
      clientPassword = existingCredential.password;
    } else {
      clientUsername = AuthenticationUtil.generateAlphaNumericPassword().toLowerCase();
      clientPassword = AuthenticationUtil.generateAlphaNumericPassword();

      ClientAuthenticationUtil.createClientAccount(applicationName, clientUsername, clientPassword);
    }

    return new JSONObject()
        .put("status", "valid")
        .put("username", clientUsername)
        .put("password", clientPassword)
        .toString();
  }

  // TODO: Make it so that you can get a portion of the schema info (only for an application)
  @Override
  public SchemaInfo getSchemaInfo() {
    return SchemaInfo.getInstance();
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
   * @param sessionJsonString session json passed from {@link
   *     pathstore.client.PathStoreSession#execute(Statement, SessionToken)}
   * @return true if the session is valid, false if not valid
   */
  @Override
  public boolean validateSession(final String sessionJsonString) {

    SessionToken sessionToken = SessionToken.buildFromJsonString(sessionJsonString);

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

        // force push all of K or T of session from sourceNode to N_A

        // force pull all of K or T of session from N_A to NodeID

        return true;
      }

      return true;
    }

    return false;
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
            .collect(
                Collectors.toMap(
                    row -> row.getInt(Constants.DEPLOYMENT_COLUMNS.NEW_NODE_ID),
                    row -> row.getInt(Constants.DEPLOYMENT_COLUMNS.PARENT_NODE_ID)));

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
   * Only called once by {@link pathstore.sessions.PathStoreSessionManager#init(String)}
   *
   * @return local node id
   */
  @Override
  public int getLocalNodeId() {
    return PathStoreProperties.getInstance().NodeID;
  }
}
