package pathstore.authentication.credentials;

import lombok.NonNull;

/**
 * This credential class is solely used for comparisons between another credential that was pulled
 * from the cache.
 */
public class NopCredential extends Credential<Boolean> {
  /**
   * @param username username to compare
   * @param password password to compare
   */
  public NopCredential(@NonNull final String username, @NonNull final String password) {
    super(true, username, password);
  }
}
