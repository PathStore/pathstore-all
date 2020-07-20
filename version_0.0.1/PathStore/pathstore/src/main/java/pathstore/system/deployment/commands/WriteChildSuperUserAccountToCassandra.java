package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialInfo;

public class WriteChildSuperUserAccountToCassandra implements ICommand {

  private final int childNodeId;

  private final String username;

  private final String password;

  public WriteChildSuperUserAccountToCassandra(final int childNodeId, final String username, final String password) {
    this.childNodeId = childNodeId;
    this.username = username;
    this.password = password;
  }

  @Override
  public void execute() {
    CredentialInfo.getInstance().add(this.childNodeId, this.username, this.password);
  }
}
