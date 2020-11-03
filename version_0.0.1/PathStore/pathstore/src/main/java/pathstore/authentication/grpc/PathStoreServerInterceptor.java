package pathstore.authentication.grpc;

import io.grpc.Metadata;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.Credential;

/**
 * This AuthClientInterceptor is specifically used to deal with the case when the grpc client is a
 * pathstore {@link pathstore.common.Role#SERVER}. As {@link pathstore.client.PathStoreServerClient}
 * is the wrapper for the grpc connection it needs to be able to handle the case of having an
 * instance of the credentials available all the time.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathStoreServerInterceptor extends AuthClientInterceptor {
  /** instance of the class */
  private static PathStoreServerInterceptor instance;

  /**
   * @param daemonCredentials daemon credentials of the parent node
   * @return Server Interceptor instance for that node
   * @see pathstore.client.PathStoreServerClient
   */
  public static synchronized PathStoreServerInterceptor getInstance(
      final Credential<Integer> daemonCredentials) {
    if (instance == null) instance = new PathStoreServerInterceptor(daemonCredentials);
    return instance;
  }

  /** Credential from caller */
  private final Credential<Integer> credential;

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.PRIMARY_KEY, String.valueOf(this.credential.primaryKey));
    header.put(Keys.USERNAME, this.credential.username);
    header.put(Keys.PASSWORD, this.credential.password);
  }
}
