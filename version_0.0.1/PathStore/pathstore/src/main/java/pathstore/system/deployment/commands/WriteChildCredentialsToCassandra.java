package pathstore.system.deployment.commands;

import pathstore.authentication.CredentialInfo;

public class WriteChildCredentialsToCassandra implements ICommand {

  private final int childNodeId;

  private final String username;

  private final String password;

  public WriteChildCredentialsToCassandra(
      final int childNodeId, final String username, final String password) {
    this.childNodeId = childNodeId;
    this.username = username;
    this.password = password;
  }

  @Override
  public void execute() {
    CredentialInfo.getInstance().add(this.childNodeId, this.username, this.password);
  }

  @Override
  public String toString() {
    return "Writing child credentials to local cassandra";
  }
}
