package pathstoreweb.pathstoreadminpanel.services.servers.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pathstore.common.Constants;
import pathstore.common.tables.ServerEntry;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.GetServers;

import java.util.List;

/** This formatter is used to format the response of querying all available servers */
public class GetServersFormatter implements IFormatter {

  /** List of servers stored in {@link Constants#SERVERS} and gathered from {@link GetServers} */
  private final List<ServerEntry> listOfServers;

  /** @param listOfServers {@link #listOfServers} */
  public GetServersFormatter(final List<ServerEntry> listOfServers) {
    this.listOfServers = listOfServers;
  }

  /** @return list of all servers queried in json format */
  @Override
  public ResponseEntity<String> format() {

    JSONArray jsonArray = new JSONArray();

    for (ServerEntry server : this.listOfServers) {
      JSONObject object = new JSONObject();

      object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, server.serverUUID.toString());
      object.put(Constants.SERVERS_COLUMNS.IP, server.ip);
      object.put(Constants.SERVERS_COLUMNS.USERNAME, server.username);
      object.put(Constants.SERVERS_COLUMNS.AUTH_TYPE, server.authType.toString());
      object.put(Constants.SERVERS_COLUMNS.SSH_PORT, server.sshPort);
      object.put(Constants.SERVERS_COLUMNS.RMI_PORT, server.rmiPort);
      object.put(Constants.SERVERS_COLUMNS.NAME, server.name);

      jsonArray.put(object);
    }

    return new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
  }
}
