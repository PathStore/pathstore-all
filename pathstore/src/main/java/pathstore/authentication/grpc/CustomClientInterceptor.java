package pathstore.authentication.grpc;

import io.grpc.Metadata;
import lombok.RequiredArgsConstructor;
import pathstore.authentication.credentials.Credential;

/**
 * This class is specifically used for {@link
 * pathstore.client.PathStoreServerClient#getCustom(String, int, Credential)}
 *
 * @param <CredentialT> credential token type
 */
@RequiredArgsConstructor
public class CustomClientInterceptor<CredentialT extends Credential<?>>
    extends AuthClientInterceptor {

  /** Credential provided */
  private final CredentialT credential;

  /** @param header header to modify {@link #credential} */
  @Override
  public void setHeader(final Metadata header) {
    header.put(Keys.USERNAME, this.credential.getUsername());
    header.put(Keys.PASSWORD, this.credential.getPassword());
  }
}
