package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.UpdateServerPayload;

/** Simple service to update a server record */
public class UpdateServer implements IService {

  /** Data for the server record */
  private final UpdateServerPayload payload;

  /** @param payload {@link #payload} */
  public UpdateServer(final UpdateServerPayload payload) {
    this.payload = payload;
  }

  /**
   * Update server record and return 200 OK
   *
   * @return 200 OK
   */
  @Override
  public ResponseEntity<String> response() {

    this.updateServer();

    return new ResponseEntity<>(new JSONObject().toString(), HttpStatus.OK);
  }

  /** Set all rows with values from website as there is no need to make a read and then a select */
  private void updateServer() {

    Session session = PathStoreCluster.getInstance().connect();

    Update update = QueryBuilder.update(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

    update
        .where(
            QueryBuilder.eq(
                Constants.SERVERS_COLUMNS.SERVER_UUID, this.payload.server.serverUUID.toString()))
        .with(QueryBuilder.set(Constants.SERVERS_COLUMNS.USERNAME, this.payload.server.username))
        .and(QueryBuilder.set(Constants.SERVERS_COLUMNS.PASSWORD, this.payload.server.password))
        .and(QueryBuilder.set(Constants.SERVERS_COLUMNS.SSH_PORT, this.payload.server.sshPort))
        .and(QueryBuilder.set(Constants.SERVERS_COLUMNS.RMI_PORT, this.payload.server.rmiPort))
        .and(QueryBuilder.set(Constants.SERVERS_COLUMNS.NAME, this.payload.server.name));

    session.execute(update);
  }
}
