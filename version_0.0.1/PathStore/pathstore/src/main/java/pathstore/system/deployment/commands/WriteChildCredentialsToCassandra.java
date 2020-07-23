package pathstore.system.deployment.commands;

import pathstore.authentication.AuthenticationUtil;
import pathstore.authentication.CredentialInfo;

/**
 * This command is used to write child daemon credential information to the local node. This is used
 * to connect to the child node on un-deployment to force push all dirty data.
 *
 * @implNote This class uses {@link CredentialInfo} as when this gets written it also gets stored in
 *     memory for more convenient access when used in the future
 */
public class WriteChildCredentialsToCassandra implements ICommand {

  /** Child node Id */
  private final int childNodeId;

  /** Child daemon username {@link pathstore.common.Constants#PATHSTORE_DAEMON_USERNAME} */
  private final String username;

  /**
   * Randomly generated password
   *
   * @see AuthenticationUtil#generateAlphaNumericPassword()
   */
  private final String password;

  /**
   * @param childNodeId {@link #childNodeId}
   * @param username {@link #username}
   * @param password {@link #password}
   */
  public WriteChildCredentialsToCassandra(
      final int childNodeId, final String username, final String password) {
    this.childNodeId = childNodeId;
    this.username = username;
    this.password = password;
  }

  /** Calls {@link CredentialInfo#add(int, String, String)} */
  @Override
  public void execute() {
    CredentialInfo.getInstance().add(this.childNodeId, this.username, this.password);
  }

  /** @return informs user what is occurring */
  @Override
  public String toString() {
    return "Writing child credentials to local cassandra";
  }
}
