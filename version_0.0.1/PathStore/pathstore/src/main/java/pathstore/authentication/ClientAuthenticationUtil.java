package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.schemaFSM.ProccessStatus;

/**
 * This util function encompasses all functions required to register and unregister a temporary
 * client application account.
 *
 * <p>See the wiki on client authentication for how this entire process works (with examples).
 */
public class ClientAuthenticationUtil {

  /**
   * This function is used to ensure that the application name passed is loaded on the local node.
   *
   * @param applicationName name of application provided by user
   * @return true if loaded else false
   */
  public static boolean isApplicationNotLoaded(final String applicationName) {
    Session pathStoreSession = PathStoreCluster.getDaemonInstance().connect();

    Select queryNodeSchemasTable =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    queryNodeSchemasTable.where(
        QueryBuilder.eq(
            Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID));

    for (Row row : pathStoreSession.execute(queryNodeSchemasTable))
      if (row.getString(Constants.NODE_SCHEMAS_COLUMNS.KEYSPACE_NAME).equals(applicationName))
        if (ProccessStatus.valueOf(row.getString(Constants.NODE_SCHEMAS_COLUMNS.PROCESS_STATUS))
            == ProccessStatus.INSTALLED) return false;

    return true;
  }

  /**
   * This function is used to determine if a user passed applicationName and password match the
   * master application combo defined in {@link Constants#APPLICATION_CREDENTIALS}
   *
   * @param applicationName application name to check
   * @param password password to match
   * @return true if the combo matches else false.
   */
  public static boolean isComboInvalid(final String applicationName, final String password) {

    Session pathStoreSession = PathStoreCluster.getDaemonInstance().connect();

    Select queryCombo =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_CREDENTIALS);
    queryCombo.where(
        QueryBuilder.eq(Constants.APPLICATION_CREDENTIALS_COLUMNS.KEYSPACE_NAME, applicationName));

    for (Row row : pathStoreSession.execute(queryCombo))
      if (row.getString(Constants.APPLICATION_CREDENTIALS_COLUMNS.PASSWORD).equals(password))
        return false;

    return true;
  }

  /**
   * This function is used to create a temporary role on cassandra for a given application. It will
   * also store this information in the local client_auth table for later reference.
   *
   * @param applicationName application name to grant permission on
   * @param clientUsername role username (random generated string)
   * @param clientPassword role password (random generated string)
   */
  public static void createClientAccount(
      final String applicationName, final String clientUsername, final String clientPassword) {
    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    AuthenticationUtil.createRole(superUserSession, clientUsername, false, true, clientPassword);
    AuthenticationUtil.grantAccessToKeyspace(superUserSession, applicationName, clientUsername);

    superUserSession.execute(
        QueryBuilder.insertInto("local_keyspace", "client_auth")
            .value("keyspace_name", applicationName)
            .value("username", clientUsername)
            .value("password", clientPassword));
  }

  /**
   * This function is used to validate {@link
   * pathstore.system.PathStoreServerImplRMI#unRegisterApplication(String, String, String)}
   * parameters. As if they pass an invalid tuple there is nothing more to do.
   *
   * @param applicationName application name the combo is associated with
   * @param clientUsername username given when registered
   * @param clientPassword password given when registered
   * @return false if the tuple is valid, else true.
   */
  public static boolean isClientAccountInvalidInLocalTable(
      final String applicationName, final String clientUsername, final String clientPassword) {
    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    for (Row row :
        superUserSession.execute(QueryBuilder.select().all().from("local_keyspace", "client_auth")))
      if (row.getString("keyspace_name").equals(applicationName)
          && row.getString("username").equals(clientUsername)
          && row.getString("password").equals(clientPassword)) return false;

    return true;
  }

  /**
   * This function is used to drop a client account from the local database. It also removes it from
   * the client_auth table
   *
   * @param applicationName application name
   * @param clientUsername username given when registered
   * @param clientPassword password given when registered
   */
  public static void deleteClientAccount(
      final String applicationName, final String clientUsername, final String clientPassword) {
    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    superUserSession.execute(
        QueryBuilder.delete()
            .from("local_keyspace", "client_auth")
            .where(QueryBuilder.eq("keyspace_name", applicationName))
            .and(QueryBuilder.eq("username", clientUsername))
            .and(QueryBuilder.eq("password", clientPassword)));

    AuthenticationUtil.dropRole(superUserSession, clientUsername);
  }
}
