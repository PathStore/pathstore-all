package pathstore.client;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.json.JSONObject;
import pathstore.common.PathStoreProperties;
import pathstore.util.ClusterCache;

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
   * @throws Exception If there is an issues retrieving the response from the local node, or the
   *     credentials provided are invalid.
   * @apiNote Ideally you should only call this function at the start of your application, then you
   *     can use {@link #getInstance()} to retrieve this instance in other classes.
   */
  public static synchronized PathStoreClientAuthenticatedCluster initInstance(
      final String applicationName, final String masterPassword) throws Exception {
    if (instance == null) {
      Optional<String> response =
          PathStoreServerClient.getInstance().registerApplication(applicationName, masterPassword);

      if (response.isPresent()) {
        JSONObject responseObject = new JSONObject(response.get());

        if (responseObject.getString("status").equals("valid"))
          instance =
              new PathStoreClientAuthenticatedCluster(
                  applicationName,
                  responseObject.getString("username"),
                  responseObject.getString("password"));
        else throw new Exception("Login Credentials are invalid");
      } else throw new Exception("Response is not present");
    }
    return instance;
  }

  /**
   * This function is used to retrieve the local instance of this class if {@link
   * #initInstance(String, String)} has already been called and successfully works.
   *
   * @return instance if present
   * @throws Exception thrown if {@link #initInstance(String, String)} hasn't been called yet
   */
  public static synchronized PathStoreClientAuthenticatedCluster getInstance() throws Exception {
    if (instance != null) return instance;

    throw new Exception(
        "Instance is not yet initialized you must call PathStoreClientAuthenticatedCluster#initInstance first");
  }

  /** Application name used */
  private final String applicationName;

  /** Username provided on registration */
  private final String clientUsername;

  /** Password provided on registration */
  private final String clientPassword;

  /** Cluster connection using client credentials */
  private final Cluster cluster;

  /** PathStoreSession created using cluster */
  private final Session session;

  /**
   * @param applicationName {@link #applicationName}
   * @param clientUsername {@link #clientUsername}
   * @param clientPassword {@link #clientPassword}
   */
  private PathStoreClientAuthenticatedCluster(
      final String applicationName, final String clientUsername, final String clientPassword) {

    System.out.println(
        String.format("Connecting with credentials %s %s", clientUsername, clientPassword));

    this.applicationName = applicationName;
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
  public Session connect() {
    return this.session;
  }

  /**
   * This function is used to close the connection with the local node. It will close the session,
   * cluster
   */
  public void close() {
    this.session.close();
    this.cluster.close();

    PathStoreServerClient.getInstance()
        .unRegisterApplication(this.applicationName, this.clientUsername, this.clientPassword);
  }
}
