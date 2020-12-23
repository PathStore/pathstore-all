package pathstoreweb.pathstoreadminpanel.services.servers;

import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.http.ResponseEntity;
import pathstore.client.PathStoreClientAuthenticatedCluster;
import pathstore.common.Constants;
import pathstore.common.tables.ServerEntry;
import pathstoreweb.pathstoreadminpanel.services.IService;
import pathstoreweb.pathstoreadminpanel.services.servers.formatter.GetServersFormatter;

import java.util.List;
import java.util.stream.Collectors;

/** Getter service to query all servers stored in the database */
public class GetServers implements IService {

  /** @return {@link GetServersFormatter#format()} */
  @Override
  public ResponseEntity<String> response() {
    return new GetServersFormatter(this.getServers()).format();
  }

  /**
   * Select all servers from table and parse them into a list of {@link ServerEntry} object
   *
   * @return list of servers
   */
  private List<ServerEntry> getServers() {
    return PathStoreClientAuthenticatedCluster.getInstance().connect()
        .execute(
            QueryBuilder.select().all().from(Constants.PATHSTORE_APPLICATIONS, Constants.SERVERS))
        .stream()
        .map(ServerEntry::fromRow)
        .collect(Collectors.toList());
  }
}
