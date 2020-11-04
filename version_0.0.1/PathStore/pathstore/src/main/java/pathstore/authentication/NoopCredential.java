package pathstore.authentication;

import lombok.NonNull;

/**
 * This credential class is solely used for comparisons between another credential that was pulled
 * from the cache.
 */
public class NoopCredential extends Credential<Boolean> {
  /**
   * @param username username to compare
   * @param password password to compare
   */
  public NoopCredential(@NonNull final String username, @NonNull final String password) {
    super(true, username, password);
  }
}
