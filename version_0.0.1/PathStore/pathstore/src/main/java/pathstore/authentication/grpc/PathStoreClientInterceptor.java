package pathstore.authentication.grpc;

import io.grpc.Metadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pathstore.authentication.credentials.ClientCredential;

/**
 * This AuthClientInterceptor is specifically used to deal with the case when the grpc client is a
 * pathstore {@link pathstore.common.Role#CLIENT}. As {@link pathstore.client.PathStoreServerClient}
 * is the wrapper for the grpc connection it needs to be able to handle the case of having to make a
 * grpc call to the local node before they know the credentials. This instance covers that case.
 * However it assumes that the credentials will have a primary key of a type string.
 *
 * @see PathStoreServerInterceptor for when the grpc client is a pathstore {@link
 *     pathstore.common.Role#SERVER}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathStoreClientInterceptor extends AuthClientInterceptor {

  /** instance of client interceptor */
  @Getter(lazy = true)
  private static final PathStoreClientInterceptor instance = new PathStoreClientInterceptor();

  /**
   * Credentials to set in the header. Default values are noop.
   *
   * @see pathstore.client.PathStoreClientAuthenticatedCluster for invokation of setter
   */
  @Setter private ClientCredential credential = new ClientCredential("noop", "noop", "noop");

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.PRIMARY_KEY, this.credential.getSearchable());
    header.put(Keys.USERNAME, this.credential.getUsername());
    header.put(Keys.PASSWORD, this.credential.getPassword());
  }
}
