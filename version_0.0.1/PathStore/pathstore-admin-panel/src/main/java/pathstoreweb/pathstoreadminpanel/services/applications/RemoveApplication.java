package pathstoreweb.pathstoreadminpanel.services.applications;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.applications.payload.RemoveApplicationPayload;

/**
 * This service will remove an application from the application list iff it is not deployed on any
 * nodes
 */
public class RemoveApplication implements IService {

  /**
   * Validated payload
   *
   * @see RemoveApplicationPayload
   */
  private final RemoveApplicationPayload payload;

  /** @param payload {@link #payload} */
  public RemoveApplication(final RemoveApplicationPayload payload) {
    this.payload = payload;
  }

  /** Remove the application from the network and return 200 OK */
  @Override
  public ResponseEntity<String> response() {
    removeApplication();
    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Delete the given application record from the table */
  private void removeApplication() {
    Session session = PathStoreCluster.getSuperUserInstance().connect();

    Delete removeApp = QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.APPS);
    removeApp.where(
        QueryBuilder.eq(Constants.APPS_COLUMNS.KEYSPACE_NAME, this.payload.applicationName));

    session.execute(removeApp);

    Delete removeMasterPassword =
        QueryBuilder.delete()
            .from(Constants.PATHSTORE_APPLICATIONS, Constants.APPLICATION_CREDENTIALS);
    removeMasterPassword.where(
        QueryBuilder.eq(
            Constants.APPLICATION_CREDENTIALS_COLUMNS.KEYSPACE_NAME, this.payload.applicationName));

    session.execute(removeMasterPassword);
  }
}
