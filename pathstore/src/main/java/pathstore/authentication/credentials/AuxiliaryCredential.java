package pathstore.authentication.credentials;

import lombok.NonNull;

/**
 * TODO: Denote all auxiliary credentials here
 *
 * <p>This class is used to denote auxiliary credentials.
 *
 * <p>They are:
 *
 * <p>Network Administrator account
 */
public class AuxiliaryCredential extends Credential<String> {
  /**
   * @param name name of the credential i.e. Network Administrator
   * @param username username
   * @param password password
   */
  public AuxiliaryCredential(
      final @NonNull String name, final @NonNull String username, final @NonNull String password) {
    super(name, username, password);
  }
}
