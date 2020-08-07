package pathstore.system;

import org.json.JSONObject;
import pathstore.authentication.AuthenticationUtil;
import pathstore.authentication.ClientAuthenticationUtil;
import pathstore.authentication.Credential;
import pathstore.common.PathStoreServer;
import pathstore.common.QueryCache;
import pathstore.exception.PathMigrateAlreadyGoneException;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.UUID;

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
}
