package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreSession;
import pathstore.common.Constants;
import pathstore.common.PathStoreProperties;
import pathstore.common.tables.NodeSchemaEntry;
import pathstore.common.tables.NodeSchemaProcessStatus;
import pathstore.system.PathStorePrivilegedCluster;

import java.util.Optional;

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
    PathStoreSession pathStoreSession = PathStoreCluster.getDaemonInstance().connect();

    Select queryNodeSchemasTable =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.NODE_SCHEMAS);

    queryNodeSchemasTable.where(
        QueryBuilder.eq(
            Constants.NODE_SCHEMAS_COLUMNS.NODE_ID, PathStoreProperties.getInstance().NodeID));

    for (Row row : pathStoreSession.execute(queryNodeSchemasTable)) {
      NodeSchemaEntry entry = NodeSchemaEntry.fromRow(row);
      if (entry.keyspaceName.equals(applicationName))
        if (entry.nodeSchemaProcessStatus == NodeSchemaProcessStatus.INSTALLED) return false;
    }

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
   * This function is used to get an exiting client account from the client auth table, if it exists
   *
   * @param applicationName application name given
   * @return optional credential. If it doesn't exist returns empty
   */
  public static Optional<Credential> getExistingClientAccount(final String applicationName) {
    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    for (Row row :
        superUserSession.execute(
            QueryBuilder.select()
                .all()
                .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
                .where(
                    QueryBuilder.eq(
                        Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, applicationName))))
      return Optional.of(
          new Credential(
              -1,
              row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME),
              row.getString(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD)));

    return Optional.empty();
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
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, applicationName)
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.USERNAME, clientUsername)
            .value(Constants.LOCAL_CLIENT_AUTH_COLUMNS.PASSWORD, clientPassword));
  }

  /**
   * This function is used to drop a client account from the local database. It also removes it from
   * the client_auth table
   *
   * @param applicationName application name
   * @return true if a deletion occurred else false
   */
  public static boolean deleteClientAccount(final String applicationName) {
    Session superUserSession = PathStorePrivilegedCluster.getSuperUserInstance().connect();

    Optional<Credential> optionalCredential = getExistingClientAccount(applicationName);

    if (optionalCredential.isPresent()) {
      superUserSession.execute(
          QueryBuilder.delete()
              .from(Constants.PATHSTORE_APPLICATIONS, Constants.LOCAL_CLIENT_AUTH)
              .where(
                  QueryBuilder.eq(
                      Constants.LOCAL_CLIENT_AUTH_COLUMNS.KEYSPACE_NAME, applicationName)));

      AuthenticationUtil.dropRole(superUserSession, optionalCredential.get().username);
    }

    return optionalCredential.isPresent();
  }
}
