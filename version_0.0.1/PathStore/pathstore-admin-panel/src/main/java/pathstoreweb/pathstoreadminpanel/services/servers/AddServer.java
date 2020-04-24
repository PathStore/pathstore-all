package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.AddServerFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.payload.AddServerPayload;

public class AddServer implements IService {

  private final AddServerPayload payload;

  public AddServer(final AddServerPayload payload) {
    this.payload = payload;
  }

  @Override
  public String response() {
    this.writeServerRecord();

    return new AddServerFormatter(this.payload.server.serverUUID, this.payload.ip).format();
  }

  private void writeServerRecord() {

    Session session = PathStoreCluster.getInstance().connect();

    Insert insert =
        QueryBuilder.insertInto(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS)
            .value(Constants.SERVERS_COLUMNS.SERVER_UUID, this.payload.server.serverUUID)
            .value(Constants.SERVERS_COLUMNS.IP, this.payload.server.ip)
            .value(Constants.SERVERS_COLUMNS.USERNAME, this.payload.server.username)
            .value(Constants.SERVERS_COLUMNS.PASSWORD, this.payload.server.password);

    session.execute(insert);
  }
}
