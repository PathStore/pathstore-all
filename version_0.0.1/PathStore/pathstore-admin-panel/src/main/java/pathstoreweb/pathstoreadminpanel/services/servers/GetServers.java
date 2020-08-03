package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.GetServersFormatter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/** Getter service to query all servers stored in the database */
public class GetServers implements IService {

  /** @return {@link GetServersFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new GetServersFormatter(this.getServers()).format();
  }

  /**
   * Select all servers from table and parse them into a list of {@link Server} object
   *
   * @return list of servers
   */
  private List<Server> getServers() {

    LinkedList<Server> listOfServers = new LinkedList<>();

    Session session = PathStoreCluster.getSuperUserInstance().connect();
    Select queryAllServers =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

    for (Row row : session.execute(queryAllServers))
      listOfServers.push(
          new Server(
              UUID.fromString(row.getString(Constants.SERVERS_COLUMNS.SERVER_UUID)),
              row.getString(Constants.SERVERS_COLUMNS.IP),
              row.getString(Constants.SERVERS_COLUMNS.USERNAME),
              row.getString(Constants.SERVERS_COLUMNS.PASSWORD),
              row.getInt(Constants.SERVERS_COLUMNS.SSH_PORT),
              row.getInt(Constants.SERVERS_COLUMNS.RMI_PORT),
              row.getString(Constants.SERVERS_COLUMNS.NAME)));

    return listOfServers;
  }
}
