package pathstore.client;

import com.datastax.driver.core.Cluster;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONObject;
import pathstore.authentication.credentials.ClientCredential;
import pathstore.authentication.grpc.PathStoreClientInterceptor;
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

  /**
   * Local saved instance of this class. Passes application name and master password from the
   * properties file
   *
   * @see PathStoreProperties#applicationName
   * @see PathStoreProperties#applicationMasterPassword
   */
  @Getter(lazy = true)
  private static final PathStoreClientAuthenticatedCluster instance =
      initInstance(
          PathStoreProperties.getInstance().applicationName,
          PathStoreProperties.getInstance().applicationMasterPassword);

  /**
   * This function is used to initialize the connection to the local node. You can call this
   * function more than once and it won't re-call the local node for initialization
   *
   * @param applicationName the application name you're trying to connect with
   * @param masterPassword the master password associated with your application
   * @return a connection instance if valid credentials are passed.
   * @see #instance
   */
  private static PathStoreClientAuthenticatedCluster initInstance(
      @NonNull final String applicationName, @NonNull final String masterPassword) {

    System.out.println("Calling");

    Pair<Optional<String>, Optional<SchemaInfo>> response =
        PathStoreServerClient.getInstance()
            .registerApplicationClient(applicationName, masterPassword);

    System.out.println("called");

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
          return new PathStoreClientAuthenticatedCluster(
              new ClientCredential(
                  applicationName,
                  responseObject.getString(Constants.REGISTER_APPLICATION.USERNAME),
                  responseObject.getString(Constants.REGISTER_APPLICATION.PASSWORD),
                  false)); // idk if this is going to relevant
        } else
          throw new RuntimeException(
              "Schema info fetched is not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
      } else
        throw new RuntimeException(responseObject.getString(Constants.REGISTER_APPLICATION.REASON));
    } else
      throw new RuntimeException(
          "Credentials fetched are not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
  }

  /** Cluster connection using client credentials */
  private final Cluster cluster;

  /** PathStoreSession created using cluster */
  private final PathStoreSession session;

  /**
   * @param clientCredential client credential passed from the local node that is used to
   *     communicate via cassandra and GRPC
   */
  private PathStoreClientAuthenticatedCluster(final ClientCredential clientCredential) {
    this.cluster =
        ClusterCache.createCluster(
            PathStoreProperties.getInstance().CassandraIP,
            PathStoreProperties.getInstance().CassandraPort,
            clientCredential.getUsername(),
            clientCredential.getPassword());

    this.session = new PathStoreSession(this.cluster);

    // As of now the client is considered connected and properly ready to communicate with the local
    // node
    PathStoreClientInterceptor.getInstance().setCredential(clientCredential);

    // All operations to perform after connection is complete
    PathStoreProperties.getInstance().NodeID = PathStoreServerClient.getInstance().getLocalNodeId();
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
  }
}
