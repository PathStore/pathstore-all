package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialCache;

/**
 * This command is used to remove credentials from the local node's pathstore_applications.local_auth table during
 * un-deployment of a child node
 */
public class RemoveLocalCredential implements ICommand {
  /** Node id of child that is being undeployed */
  private final int nodeId;

  /** @param nodeId {@link #nodeId} */
  public RemoveLocalCredential(final int nodeId) {
    this.nodeId = nodeId;
  }

  /** Remove the data from the cache and the table */
  @Override
  public void execute() {
    CredentialCache.getNodes().remove(this.nodeId);
  }

  /** @return command inform message */
  @Override
  public String toString() {
    return String.format("Removing local credential with nodeid %d", this.nodeId);
  }
}
