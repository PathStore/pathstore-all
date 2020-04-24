package pathstoreweb.pathstoreadminpanel.services.servers.formatter;

import org.json.JSONArray;
import org.json.JSONObject;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;
import pathstoreweb.pathstoreadminpanel.services.servers.Server;

import java.util.List;

public class GetServersFormatter implements IFormatter {

  private final List<Server> listOfServers;

  public GetServersFormatter(final List<Server> listOfServers) {
    this.listOfServers = listOfServers;
  }

  @Override
  public String format() {

    JSONArray jsonArray = new JSONArray();

    for (Server server : this.listOfServers) {
      JSONObject object = new JSONObject();
      object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, server.serverUUID.toString());
      object.put(Constants.SERVERS_COLUMNS.IP, server.ip);
      object.put(Constants.SERVERS_COLUMNS.USERNAME, server.username);
      object.put(Constants.SERVERS_COLUMNS.PASSWORD, server.password);

      jsonArray.put(object);
    }

    return jsonArray.toString();
  }
}
