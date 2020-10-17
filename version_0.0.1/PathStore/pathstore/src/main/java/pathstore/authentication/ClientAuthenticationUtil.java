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
}
