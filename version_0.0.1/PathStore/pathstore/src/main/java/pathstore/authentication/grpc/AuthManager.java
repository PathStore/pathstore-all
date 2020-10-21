package pathstore.authentication.grpc;

/**
 * This class is used for the registration, storage and comparison of credentials related to a grpc
 * server.
 *
 * <p>TODO: Add credentials registration
 *
 * <p>TODO: Should this be a factory?
 */
public class AuthManager {
  /**
   * @param endpoint endpoint called
   * @param primaryKey primary key given
   * @param username username given
   * @param password and password given
   * @return true if authenticated false if not
   */
  public boolean isAuthenticated(
      final String endpoint,
      final String primaryKey,
      final String username,
      final String password) {
    if (endpoint == null || primaryKey == null || username == null || password == null)
      return false;

    return primaryKey.equals("primary_key")
        && username.equals("username")
        && password.equals("password");
  }
}
