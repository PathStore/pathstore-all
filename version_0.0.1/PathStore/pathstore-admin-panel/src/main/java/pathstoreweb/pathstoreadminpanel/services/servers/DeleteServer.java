package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.DeleteServerPayload;

/**
 * Delete server service. This service if executed will always return HTTP 200 as the only
 * constraints are the validity of the input data
 *
 * @see DeleteServerPayload
 */
public class DeleteServer implements IService {

  /** Valid user submitted payload */
  private final DeleteServerPayload deleteServerPayload;

  /** @param deleteServerPayload {@link #deleteServerPayload} */
  public DeleteServer(final DeleteServerPayload deleteServerPayload) {
    this.deleteServerPayload = deleteServerPayload;
  }

  /**
   * Deletes the server record
   *
   * @return ok
   */
  @Override
  public ResponseEntity<String> response() {

    this.deleteServer();

    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Simply deletes the server record from the table based on what uuid was passed */
  private void deleteServer() {

    Session session = PathStoreCluster.getInstance().connect();

    Delete delete = QueryBuilder.delete().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);
    delete.where(
        QueryBuilder.eq(
            Constants.SERVERS_COLUMNS.SERVER_UUID, this.deleteServerPayload.serverUUID.toString()));

    session.execute(delete);
  }
}
