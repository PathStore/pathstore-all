package pathstore.client;

import com.datastax.driver.core.Cluster;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;
import pathstore.util.SchemaInfo;

import java.util.Optional;

/**
 * This class is the front facing class users will use to connect to their local pathstore node with
 * their application name and the associated master password.
 */
public class PathStoreClientAuthenticatedCluster {

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
      Optional<String> response =
          PathStoreServerClient.getInstance().registerApplicationClient(applicationName, masterPassword);

      if (response.isPresent()) {
        JSONObject responseObject = new JSONObject(response.get());
        if (responseObject
            .getEnum(
                Constants.REGISTER_APPLICATION.STATUS_STATES.class,
                Constants.REGISTER_APPLICATION.STATUS)
            .equals(Constants.REGISTER_APPLICATION.STATUS_STATES.VALID)) {
          SchemaInfo schemaInfo =
              PathStoreServerClient.getInstance().getSchemaInfo(applicationName);

          if (schemaInfo == null)
            throw new RuntimeException("Could not get schema info from local node");

          SchemaInfo.setInstance(schemaInfo);
          instance =
              new PathStoreClientAuthenticatedCluster(
                  responseObject.getString(Constants.REGISTER_APPLICATION.USERNAME),
                  responseObject.getString(Constants.REGISTER_APPLICATION.PASSWORD));
        } else
          throw new RuntimeException(
              responseObject.getString(Constants.REGISTER_APPLICATION.REASON));
      } else
        throw new RuntimeException(
            "Response is not present, most likely a network connectivity issue has occurred");
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
  public void close() {
    this.session.close();
    this.cluster.close();
  }
}
