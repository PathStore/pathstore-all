package pathstoreweb.pathstoreadminpanel.services.applicationmanagement.payload;

import java.util.Set;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator.ApplicationNameExists;
import pathstoreweb.pathstoreadminpanel.services.applicationmanagement.validator.NodesExist;

/**
 * Payload for when a user wants to remove an application or install an application on a list of
 * nodes
 */
public final class UpdateApplicationStatePayload {

  /**
   * Name of application to perform operation on.
   *
   * <p>Ensures that the application is actually a valid application
   */
  @ApplicationNameExists(message = "The application name you passed does not exist")
  public final String applicationName;

  /**
   * List of nodes to perform the operation on
   *
   * <p>Ensures the nodes are valid nodes within the topology
   */
  @NodesExist(
      message =
          "One or more of the nodes you passed does not exist within the network topology or you didn't pass any nodes")
  public final Set<Integer> node;

  public UpdateApplicationStatePayload(final String applicationName, final Set<Integer> node) {
    this.applicationName = applicationName;
    this.node = node;
  }
}
