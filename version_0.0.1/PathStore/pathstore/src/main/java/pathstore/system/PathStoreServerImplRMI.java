package pathstore.system;

import org.json.JSONObject;
import pathstore.authentication.AuthenticationUtil;
import pathstore.authentication.ClientAuthenticationUtil;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * @implNote We assume that forever every distinct {@link #registerApplication(String, String)}
 *     there is a corresponding {@link #unRegisterApplication(String, String, String)}
 */
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

  @Override // child calls this (maybe client or child node)
  public String addUserCommandEntry(
      String user, String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException, PathMigrateAlreadyGoneException {

    // System.out.println("In addUserCommandEntry " + user + ":"+ keyspace + ":" + table + " " +
    // clauses);

    try {
      QueryCache.getInstance().updateDeviceCommandCache(user, keyspace, table, clauses, limit);
    } catch (Exception e) {
      if (e instanceof PathMigrateAlreadyGoneException)
        throw new RemoteException("PathMigrateAlreadyGoneException");
      else throw new RemoteException(e.getMessage());
    }

    return "server says hello! in user command entry";
  }

  public String addQueryEntry(String keyspace, String table, byte[] clauses, int limit)
      throws RemoteException {
    logger.info("In addQueryEntry " + keyspace + ":" + table + " " + clauses);

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
    logger.info(
        "In createQueryDelta "
            + keyspace
            + ":"
            + table
            + " "
            + clauses
            + " pts:"
            + parentTimestamp.timestamp()
            + " "
            + nodeID);

    try {
      return QueryCache.getInstance()
          .createDelta(keyspace, table, clauses, parentTimestamp, nodeID, limit);
    } catch (ClassNotFoundException | IOException e) {
      throw new RemoteException(e.getMessage());
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
  @Override
  public String registerApplication(final String applicationName, final String password) {
    logger.info(
        String.format("Register application credentials for application %s", applicationName));

    if (ClientAuthenticationUtil.isApplicationNotLoaded(applicationName)) {
      logger.info(
          String.format(
              "Registration of application credentials for application %s has failed as the provided application name is not loaded on the give node",
              applicationName));
      return new JSONObject().put("status", "invalid").toString();
    }

    if (ClientAuthenticationUtil.isComboInvalid(applicationName, password)) {
      logger.info(
          String.format(
              "Registration of application credentials for application %s has failed as the provided credentials do not match the master application credentials",
              applicationName));
      return new JSONObject().put("status", "invalid").toString();
    }

    String clientUsername, clientPassword;

    clientUsername = AuthenticationUtil.generateAlphaNumericPassword().toLowerCase();
    clientPassword = AuthenticationUtil.generateAlphaNumericPassword();

    ClientAuthenticationUtil.createClientAccount(applicationName, clientUsername, clientPassword);

    return new JSONObject()
        .put("status", "valid")
        .put("username", clientUsername)
        .put("password", clientPassword)
        .toString();
  }

  /**
   * This function is used to unregister a client application
   *
   * @param applicationName application name
   * @param clientUsername clientUsername generated from register call
   * @param clientPassword clientPassword generated from register call
   * @return status: {invalid, valid}
   * @implNote Assumes that {@link #registerApplication(String, String)} was called with the same
   *     parameters before this function was called.
   * @see pathstore.client.PathStoreClientAuthenticatedCluster
   */
  @Override
  public String unRegisterApplication(
      final String applicationName, final String clientUsername, final String clientPassword) {

    logger.info(
        String.format("Unregistering application credential for application %s", applicationName));

    if (ClientAuthenticationUtil.isClientAccountInvalidInLocalTable(
        applicationName, clientUsername, clientPassword)) {
      logger.info(
          String.format(
              "Unregistering of application credentials for application %s has failed as the provided credentials do not match any registered credentials",
              applicationName));
      return new JSONObject().put("status", "invalid").toString();
    }

    ClientAuthenticationUtil.deleteClientAccount(applicationName, clientUsername, clientPassword);

    return new JSONObject().put("status", "valid").toString();
  }
}
