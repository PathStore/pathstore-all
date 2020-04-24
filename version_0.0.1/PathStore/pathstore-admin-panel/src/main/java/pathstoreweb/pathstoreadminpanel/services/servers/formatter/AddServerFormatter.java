package pathstoreweb.pathstoreadminpanel.services.servers.formatter;

import org.json.JSONObject;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.UUID;

public class AddServerFormatter implements IFormatter {

  private final UUID serverUUID;

  private final String ip;

  public AddServerFormatter(final UUID serverUUID, final String ip) {
    this.serverUUID = serverUUID;
    this.ip = ip;
  }

  @Override
  public String format() {

    JSONObject object = new JSONObject();

    object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, this.serverUUID);
    object.put(Constants.SERVERS_COLUMNS.IP, this.ip);

    return object.toString();
  }
}
