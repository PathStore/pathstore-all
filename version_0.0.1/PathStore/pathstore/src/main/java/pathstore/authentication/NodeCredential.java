package pathstore.authentication;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/** This credential is used to denote a credential for a pathstore node to use. */
@EqualsAndHashCode(callSuper = true)
public class NodeCredential extends Credential<Integer> {
  /**
   * @param searchable node id where the credential belongs to
   * @param username username
   * @param password password
   */
  public NodeCredential(
      final @NonNull Integer searchable,
      final @NonNull String username,
      final @NonNull String password) {
    super(searchable, username, password);
  }
}
