package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import pathstore.client.PathStoreCluster;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.GetServersFormatter;

import java.util.LinkedList;
import java.util.List;

/** Getter service to query all servers stored in the database */
public class GetServers implements IService {

  /** @return {@link GetServersFormatter#format()} */
  @Override
  public String response() {
    return new GetServersFormatter(this.getServers()).format();
  }

  /**
   * Select all servers from table and parse them into a list of {@link Server} object
   *
   * @return list of servers
   */
  private List<Server> getServers() {

    LinkedList<Server> listOfServers = new LinkedList<>();

    Session session = PathStoreCluster.getInstance().connect();
    Select queryAllServers =
        QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS);

    for (Row row : session.execute(queryAllServers))
      listOfServers.push(
          new Server(
              row.getString(Constants.SERVERS_COLUMNS.SERVER_UUID),
              row.getString(Constants.SERVERS_COLUMNS.IP),
              row.getString(Constants.SERVERS_COLUMNS.USERNAME),
              row.getString(Constants.SERVERS_COLUMNS.PASSWORD)));

    return listOfServers;
  }
}
