package pathstoreweb.pathstoreadminpanel.services.servers.payload;

import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerDetached;
import pathstoreweb.pathstoreadminpanel.services.servers.validator.ServerUUIDExistence;

import java.util.UUID;

/** Simple delete server payload for the delete operation */
public final class DeleteServerPayload {

  /** Server UUID passed by user to remove */
  @ServerUUIDExistence(message = "You must pass a valid server UUID in order to delete")
  @ServerDetached(message = "You cannot delete a server that is already linked to a pathstore node")
  public final UUID serverUUID;

  /** @param serverUUID {@link #serverUUID} */
  public DeleteServerPayload(final String serverUUID) {
    this.serverUUID = UUID.fromString(serverUUID);
  }
}
