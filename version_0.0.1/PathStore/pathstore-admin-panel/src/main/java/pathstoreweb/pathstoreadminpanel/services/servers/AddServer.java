package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.AddServerFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload;

/**
 * This service is used to insert a record in the database to denote the creation of a new server
 */
public class AddServer implements IService {

  /**
   * Valid payload
   *
   * @see AddServerPayload
   * @see pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerConnectionTest
   * @see pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerUnique
   */
  private final AddServerPayload payload;

  /** @param payload {@link #payload} */
  public AddServer(final AddServerPayload payload) {
    this.payload = payload;
  }

  /**
   * Insert the server into the database and create a success response
   *
   * @return response of the uuid and ip {@link AddServerFormatter}
   */
  @Override
  public ResponseEntity<String> response() {
    this.writeServerRecord();

    return new AddServerFormatter(this.payload.server.serverUUID, this.payload.ip).format();
  }

  /** Write insert up to the table */
  private void writeServerRecord() {

    Session session = PathStoreCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(Constants.SERVERS_COLUMNS.SERVER_UUID, this.payload.server.serverUUID.toString())
            .value(Constants.SERVERS_COLUMNS.IP, this.payload.server.ip)
            .value(Constants.SERVERS_COLUMNS.USERNAME, this.payload.server.username)
            .value(Constants.SERVERS_COLUMNS.PASSWORD, this.payload.server.password)
            .value(Constants.SERVERS_COLUMNS.NAME, this.payload.server.name);

    session.execute(insert);
  }
}
