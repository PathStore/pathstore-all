package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialInfo;

public class RemoveLocalCredential implements ICommand {
  private final int nodeId;

  public RemoveLocalCredential(final int nodeId) {
    this.nodeId = nodeId;
  }

  @Override
  public void execute() {
    CredentialInfo.getInstance().remove(this.nodeId);
  }

  @Override
  public String toString() {
    return String.format("Removing local credential with nodeid %d", this.nodeId);
  }
}
