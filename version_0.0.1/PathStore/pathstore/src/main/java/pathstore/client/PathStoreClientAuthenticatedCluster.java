package pathstore.client;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONObject;
import pathstore.authentication.credentials.ClientCredential;
import pathstore.authentication.credentials.DeploymentCredential;
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
 * TODO: Client side needs to be aware if the account if a super user. If it is allow it to create a
 * privileged session
 *
 * <p>This class is the front facing class users will use to connect to their local pathstore node
 * with their application name and the associated master password.
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
        if (schemaInfoOptional
            .isPresent()) { // check to ensure that the schema info object is present
          SchemaInfo schemaInfo = schemaInfoOptional.get();
          SchemaInfo.setInstance(schemaInfo);
          return new PathStoreClientAuthenticatedCluster(
              new ClientCredential(
                  applicationName,
                  responseObject.getString(Constants.REGISTER_APPLICATION.USERNAME),
                  responseObject.getString(Constants.REGISTER_APPLICATION.PASSWORD),
                  responseObject.getBoolean(Constants.REGISTER_APPLICATION.IS_SUPER_USER)));
        } else
          throw new RuntimeException(
              "Schema info fetched is not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
      } else
        throw new RuntimeException(responseObject.getString(Constants.REGISTER_APPLICATION.REASON));
    } else
      throw new RuntimeException(
          "Credentials fetched are not present, this is a server error. Please ensure that you don't have version mismatches between the server and the client. Also ensure that you're running a stable version of the code base as with development versions you should expect that some functions don't work as expected. If you're a developer this is thrown on the grpc endpoint registerApplicationClient");
  }

  /** Credential used to connect */
  private final ClientCredential credential;

  /** Cluster connection using client credentials */
  private final Cluster cluster;

  /** Raw session */
  private final Session rawSession;

  /** PathStoreSession created using cluster */
  private final PathStoreSession psSession;

  /**
   * @param clientCredential client credential passed from the local node that is used to
   *     communicate via cassandra and GRPC
   */
  private PathStoreClientAuthenticatedCluster(final ClientCredential clientCredential) {
    this.credential = clientCredential;
    this.cluster =
        ClusterCache.createCluster(
            new DeploymentCredential(
                clientCredential.getUsername(),
                clientCredential.getPassword(),
                PathStoreProperties.getInstance().CassandraIP,
                PathStoreProperties.getInstance().CassandraPort));

    this.rawSession = this.cluster.connect();

    this.psSession = new PathStoreSession(this.rawSession);

    // As of now the client is considered connected and properly ready to communicate with the local
    // node
    PathStoreClientInterceptor.getInstance().setCredential(clientCredential);

    // All operations to perform after connection is complete
    PathStoreProperties.getInstance().NodeID = PathStoreServerClient.getInstance().getLocalNodeId();

    if (this.credential.isSuperUser()) SchemaInfo.getInstance().setSession(this.rawSession);
  }

  /** @return local node db session */
  public PathStoreSession connect() {
    return this.psSession;
  }

  /** @return raw session iff the user is a super user */
  public Session connectRaw() {
    if (!this.credential.isSuperUser())
      throw new RuntimeException("Only super user clients can use the raw session");
    return this.rawSession;
  }

  /** Close session and cluster */
  public void close() throws InterruptedException {
    PathStoreServerClient.getInstance().shutdown();
    logger.debug("Shutdown grpc connection to local node");
    this.rawSession.close();
    logger.debug("Closed cassandra session");
    this.cluster.close();
    logger.debug("Closed cassandra cluster connection");
  }
}
