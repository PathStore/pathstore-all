package pathstore.authentication;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.NonNull;
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
  public static boolean isApplicationNotLoaded(@NonNull final String applicationName) {
    if (applicationName.equals(Constants.PATHSTORE_APPLICATIONS)) return false;

    PathStoreSession pathStoreSession = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

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
   * This function is used to get the application credential record from the database based on an
   * application name and password provided from the user
   *
   * @param applicationName application name
   * @param password password
   * @return optional of an application credential object
   */
  public static Optional<ApplicationCredential> getApplicationCredentialRow(
      @NonNull final String applicationName, @NonNull final String password) {
    PathStoreSession pathStoreSession = PathStorePrivilegedCluster.getDaemonInstance().psConnect();

    Select queryCombo =
        QueryBuilder.select()
            .all()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_CREDENTIALS);
    queryCombo.where(
        QueryBuilder.eq(Constants.APPLICATION_CREDENTIALS_COLUMNS.KEYSPACE_NAME, applicationName));

    for (Row row : pathStoreSession.execute(queryCombo))
      if (row.getString(Constants.APPLICATION_CREDENTIALS_COLUMNS.PASSWORD).equals(password))
        return Optional.of(
            new ApplicationCredential(
                row.getString(Constants.APPLICATION_CREDENTIALS_COLUMNS.KEYSPACE_NAME),
                row.getString(Constants.APPLICATION_CREDENTIALS_COLUMNS.PASSWORD),
                row.getBool(Constants.APPLICATION_CREDENTIALS_COLUMNS.IS_SUPER_USER)));
    return Optional.empty();
  }
}
