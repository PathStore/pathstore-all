package pathstoreweb.pathstoreadminpanel.services.servers.formatter;

import org.json.JSONObject;
import pathstore.common.Constants;
import pathstoreweb.pathstoreadminpanel.services.IFormatter;

import java.util.UUID;

/** This formatter is used to format the response of adding a server */
public class AddServerFormatter implements IFormatter {

  /** Server UUID that was added */
  private final UUID serverUUID;

  /** Ip of server that was added */
  private final String ip;

  /**
   * @param serverUUID {@link #serverUUID}
   * @param ip {@link #ip}
   */
  public AddServerFormatter(final UUID serverUUID, final String ip) {
    this.serverUUID = serverUUID;
    this.ip = ip;
  }

  /** @return {server_uuid: {@link #serverUUID}, ip: {@link #ip}} */
  @Override
  public String format() {

    JSONObject object = new JSONObject();

    object.put(Constants.SERVERS_COLUMNS.SERVER_UUID, this.serverUUID.toString());
    object.put(Constants.SERVERS_COLUMNS.IP, this.ip);

    return object.toString();
  }
}
