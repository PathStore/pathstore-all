package pathstore.client;

import com.datastax.driver.core.Cluster;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;
import pathstore.util.ClusterCache;
import pathstore.util.Pair;
import pathstore.util.SchemaInfo;

import java.util.Optional;

/**
 * This class is the front facing class users will use to connect to their local pathstore node with
 * their application name and the associated master password.
 */
public class PathStoreClientAuthenticatedCluster {

  /** Logger */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(PathStoreClientAuthenticatedCluster.class);

  /** Local saved instance of this class. */
  private static PathStoreClientAuthenticatedCluster instance = null;

  /**
   * This function is used to initialize the connection to the local node. You can call this
   * function more than once and it won't re-call the local node for initialization
   *
   * @param applicationName the application name you're trying to connect with
   * @param masterPassword the master password associated with your application
   * @return a connection instance if valid credentials are passed.
   * @apiNote Ideally you should only call this function at the start of your application, then you
   *     can use {@link #getInstance()} to retrieve this instance in other classes.
   */
  private static PathStoreClientAuthenticatedCluster initInstance(
      final String applicationName, final String masterPassword) {
    if (instance == null) {
      Pair<Optional<String>, Optional<SchemaInfo>> response =
          PathStoreServerClient.getInstance()
              .registerApplicationClient(applicationName, masterPassword);

      Optional<String> credentialsOptional = response.t1;
      Optional<SchemaInfo> schemaInfoOptional = response.t2;

      if (credentialsOptional.isPresent()) {
        String credentials = credentialsOptional.get();

        JSONObject responseObject = new JSONObject(credentials);
        if (responseObject
            .getEnum(
                Constants.REGISTER_APPLICATION.STATUS_STATES.class,
                Constants.REGISTER_APPLICATION.STATUS)
            .equals(Constants.REGISTER_APPLICATION.STATUS_STATES.VALID)) {
          if (schemaInfoOptional.isPresent()) {
            SchemaInfo schemaInfo = schemaInfoOptional.get();
            SchemaInfo.setInstance(schemaInfo);
            PathStoreProperties.getInstance().NodeID =
                PathStoreServerClient.getInstance().getLocalNodeId();
            instance =
                new PathStoreClientAuthenticatedCluster(
                    responseObject.getString(Constants.REGISTER_APPLICATION.USERNAME),
                    responseObject.getString(Constants.REGISTER_APPLICATION.PASSWORD));
          } else
            throw new RuntimeException(
                "Schema info fetched is not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
        } else
          throw new RuntimeException(
              responseObject.getString(Constants.REGISTER_APPLICATION.REASON));
      } else
        throw new RuntimeException(
            "Credentials fetched are not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
    }
    return instance;
  }

  /**
   * This function is used to retrieve the local instance of this class. If it is not present one
   * will attempted to be created using the client authentication details present in pathstore
   * properties. If those aren't present a runtime error will be thrown to inform the user that some
   * key piece of information is missing
   *
   * @return instance if it is present or if creation was successful
   */
  public static synchronized PathStoreClientAuthenticatedCluster getInstance() {
    if (instance != null) return instance;

    PathStoreProperties.getInstance().verifyClientAuthenticationDetails();

    PathStoreProperties.getInstance().verifyCassandraConnectionDetails();

    return initInstance(
        PathStoreProperties.getInstance().applicationName,
        PathStoreProperties.getInstance().applicationMasterPassword);
  }

  /** Username provided on registration */
  private final String clientUsername;

  /** Password provided on registration */
  private final String clientPassword;

  /** Cluster connection using client credentials */
  private final Cluster cluster;

  /** PathStoreSession created using cluster */
  private final PathStoreSession session;

  /**
   * @param clientUsername {@link #clientUsername}
   * @param clientPassword {@link #clientPassword}
   */
  private PathStoreClientAuthenticatedCluster(
      final String clientUsername, final String clientPassword) {

    this.clientUsername = clientUsername;
    this.clientPassword = clientPassword;

    this.cluster =
        ClusterCache.createCluster(
            PathStoreProperties.getInstance().CassandraIP,
            PathStoreProperties.getInstance().CassandraPort,
            this.clientUsername,
            this.clientPassword);

    this.session = new PathStoreSession(this.cluster);
  }

  /** @return local node db session */
  public PathStoreSession connect() {
    return this.session;
  }

  /** Close session and cluster */
  public void close() throws InterruptedException {
    PathStoreServerClient.getInstance().shutdown();
    logger.debug("Shutdown grpc connection to local node");
    this.session.close();
    logger.debug("Closed cassandra session");
    this.cluster.close();
    logger.debug("Closed cassandra cluster connection");
    instance = null;
  }
}
