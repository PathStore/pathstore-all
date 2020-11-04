package pathstore.authentication.grpc;

import io.grpc.Metadata;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * Keys for headers generated from {@link AuthClientInterceptor} and parsed by {@link
 * AuthServerInterceptor}
 *
 * <p>TODO: Add to constants file
 */
public class Keys {
  /** username header key */
  public static final Metadata.Key<String> USERNAME =
      Metadata.Key.of("username", ASCII_STRING_MARSHALLER);

  /** password header key */
  public static final Metadata.Key<String> PASSWORD =
      Metadata.Key.of("password", ASCII_STRING_MARSHALLER);
}
