package pathstore.system.deployment.commands;

import lombok.RequiredArgsConstructor;
import pathstore.authentication.CredentialCache;
import pathstore.authentication.credentials.Credential;
import pathstore.authentication.credentials.NodeCredential;

/**
 * This command is used to write child daemon credential information to the local node. This is used
 * to connect to the child node on un-deployment to force push all dirty data.
 *
 * @implNote This class uses {@link CredentialCache} as when this gets written it also gets stored
 *     in memory for more convenient access when used in the future
 */
@RequiredArgsConstructor
public class WriteChildCredentialsToCassandra implements ICommand {

  /** Child credential */
  private final NodeCredential childCredential;

  /** Calls {@link CredentialCache#add(Credential)} */
  @Override
  public void execute() {
    CredentialCache.getNodes().add(this.childCredential);
  }

  /** @return informs user what is occurring */
  @Override
  public String toString() {
    return "Writing child credentials to local cassandra";
  }
}
