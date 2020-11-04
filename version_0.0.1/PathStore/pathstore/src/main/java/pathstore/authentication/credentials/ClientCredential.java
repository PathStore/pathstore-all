package pathstore.authentication.credentials;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * This credential is used specifically for the client credentials. This will be used to denote a
 * credential that is solely used for pathstore client.
 *
 * <p>TODO: Add super user functionality
 */
@EqualsAndHashCode(callSuper = true)
public class ClientCredential extends Credential<String> {
  /**
   * @param searchable application name
   * @param username username
   * @param password password
   */
  public ClientCredential(
      final @NonNull String searchable,
      final @NonNull String username,
      final @NonNull String password) {
    super(searchable, username, password);
  }
}
