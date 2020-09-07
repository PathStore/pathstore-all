package pathstore.client;

import com.datastax.driver.core.Cluster;
import org.json.JSONObject;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;
import pathstore.util.SchemaInfo;

import java.util.Optional;

/**
 * This class is the front facing class users will use to connect to their local pathstore node with
 * their application name and the associated master password.
 *
 * <p>TODO: Add custom exceptions
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
          PathStoreServerClient.getInstance().registerApplication(applicationName, masterPassword);

      if (response.isPresent()) {
        JSONObject responseObject = new JSONObject(response.get());
        if (responseObject.getString("status").equals("valid")) {
          SchemaInfo schemaInfo =
              PathStoreServerClient.getInstance().getSchemaInfo(applicationName);

          if (schemaInfo == null)
            throw new RuntimeException("Could not get schema info from local node");

          SchemaInfo.setInstance(schemaInfo);
          instance =
              new PathStoreClientAuthenticatedCluster(
                  responseObject.getString("username"), responseObject.getString("password"));
        } else throw new RuntimeException("Login Credentials are invalid");
      } else throw new RuntimeException("Response is not present");
    }
    return instance;
  }

  /**
   * This function is used to retrieve the local instance of this class if {@link
   * #initInstance(String, String)} has already been called and successfully works.
   *
   * @return instance if present
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
