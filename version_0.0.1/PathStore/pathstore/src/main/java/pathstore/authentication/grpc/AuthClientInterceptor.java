package pathstore.authentication.grpc;

import io.grpc.*;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

/**
 * This class is used as the auth interceptor for the grpc client. While the message is being build
 * at the end we add all the authentication information required so the expected server can
 */
public abstract class AuthClientInterceptor implements ClientInterceptor {

  /** Logger for class */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(AuthServerInterceptor.class);

  /**
   * This function is to be implemented by the child implementor to denote how to set the header
   * information for outgoing requests to the server.
   *
   * @param header header to modify
   */
  public abstract void setHeader(final Metadata header);

  /**
   * Intercept ClientCall creation by the next Channel.
   *
   * <p>The usage of this interceptor is to add valid grpc authentication information for the local
   * node or parent node. We will append the proper information to each request as authentication is
   * at the granularity of the request instead of the handshake level.
   *
   * <p>TODO: Add see blocks to denote where the authentication information for the client comes
   * from.
   *
   * <p>TODO: Define structure for {@link pathstore.client.PathStoreServerClient} interface that
   * will be used to pass authentication information in on creation of the interceptor
   *
   * @param methodDescriptor what endpoint will be called.
   * @param callOptions how the call is being processed
   * @param channel channel of said request
   * @param <ReqT> req payload type
   * @param <RespT> expected payload type
   * @return return the next call in the channel, in this case the request is always forwarded to
   *     the next call in the DAG.
   */
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      final MethodDescriptor<ReqT, RespT> methodDescriptor,
      final CallOptions callOptions,
      final Channel channel) {
    return new BackendForwardingClientCall<ReqT, RespT>(
        methodDescriptor, channel.newCall(methodDescriptor, callOptions)) {

      /**
       * Logs outgoing message at finest
       *
       * @param message message to be sent to the server.
       */
      @Override
      public void sendMessage(final ReqT message) {
        logger.finest(
            String.format(
                "Send message: Method: %s Response: {%s}",
                this.methodDescriptor.getFullMethodName(), message.toString().replace("\n", "")));
        super.sendMessage(message);
      }

      /**
       * Note from docs:
       *
       * <p>Start a call, using responseListener for processing response messages. It must be called
       * prior to any other method on this class, except for ClientCall.cancel(java.lang.String,
       * java.lang.Throwable) which may be called at any time.
       *
       * <p>Since Metadata is not thread-safe, the caller must not access (read or write) headers
       * after this point.
       *
       * <p>This function is used to build the headers object with primary_key, username and
       * password. See {@link Keys} to see the definition for each key
       *
       * @param responseListener receives response messages
       * @param header headers that will be passed to the server to encapsulate authentication
       *     information
       */
      @Override
      public void start(final Listener<RespT> responseListener, final Metadata header) {
        setHeader(header);

        BackendListener<RespT> backendListener =
            new BackendListener<>(this.methodDescriptor.getFullMethodName(), responseListener);
        super.start(backendListener, header);
      }
    };
  }

  /**
   * Wrapper for the listener started in {@link #interceptCall(MethodDescriptor, CallOptions,
   * Channel)}. This allows for further debugging on the message that gets sent out
   *
   * @param <RespT> Response type of payload from server
   */
  private static class BackendListener<RespT> extends ClientCall.Listener<RespT> {

    /** Endpoint that was called */
    private final String methodName;

    /**
     * This is the listener passed from {@link
     * BackendForwardingClientCall#start(ClientCall.Listener, Metadata)}
     */
    private final ClientCall.Listener<RespT> responseListener;

    /**
     * @param methodName {@link #methodName}
     * @param responseListener {@link #responseListener}
     */
    protected BackendListener(
        final String methodName, final ClientCall.Listener<RespT> responseListener) {
      this.methodName = methodName;
      this.responseListener = responseListener;
    }

    /**
     * Note from doc:
     *
     * <p>A response message has been received. May be called zero or more times depending on
     * whether the call response is empty, a single message or a stream of messages.
     *
     * <p>This function logs outgoing payloads at finest
     *
     * @param message outgoing payload
     */
    @Override
    public void onMessage(final RespT message) {
      logger.finest(
          String.format(
              "On message: Method: %s Response: {%s}",
              this.methodName, message.toString().replace("\n", "")));
      this.responseListener.onMessage(message);
    }

    /**
     * The response headers have been received. Headers always precede messages. Since Metadata is
     * not thread-safe, the caller must not access (read or write) headers after this point.
     *
     * @param headers containing metadata sent by the server at the start of the response.
     */
    @Override
    public void onHeaders(final Metadata headers) {
      this.responseListener.onHeaders(headers);
    }

    /**
     * The ClientCall has been closed. Any additional calls to the ClientCall will not be processed
     * by the server. No further receiving will occur and no further notifications will be made.
     * Since Metadata is not thread-safe, the caller must not access (read or write) trailers after
     * this point.
     *
     * <p>If status returns false for Status.isOk(), then the call failed. An additional block of
     * trailer metadata may be received at the end of the call from the server. An empty Metadata
     * object is passed if no trailers are received.
     *
     * @param status the result of the remote call
     * @param trailers metadata provided at call completion
     */
    @Override
    public void onClose(final Status status, final Metadata trailers) {
      this.responseListener.onClose(status, trailers);
    }

    /**
     * This indicates that the ClientCall may now be capable of sending additional messages (via
     * ClientCall.sendMessage(ReqT)) without requiring excessive buffering internally. This event is
     * just a suggestion and the application is free to ignore it, however doing so may result in
     * excessive buffering within the ClientCall. Because there is a processing delay to deliver
     * this notification, it is possible for concurrent writes to cause isReady() == false within
     * this callback. Handle "spurious" notifications by checking isReady()'s current value instead
     * of assuming it is now true. If isReady() == false the normal expectations apply, so there
     * would be another onReady() callback.
     *
     * <p>If the type of a call is either MethodDescriptor.MethodType.UNARY or
     * MethodDescriptor.MethodType.SERVER_STREAMING, this callback may not be fired. Calls that send
     * exactly one message should not await this callback.
     */
    @Override
    public void onReady() {
      this.responseListener.onReady();
    }
  }

  /**
   * This class represents a forwarding call in the DAG.
   *
   * <p>This is a wrapper for the authentication payload construction
   *
   * @param <ReqT> request payload type
   * @param <RespT> response payload type
   */
  private static class BackendForwardingClientCall<ReqT, RespT>
      extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    /** Endpoint that is being requested */
    protected final MethodDescriptor<ReqT, RespT> methodDescriptor;

    /**
     * @param methodDescriptor {@link #methodDescriptor}
     * @param delegate outgoing client call
     */
    protected BackendForwardingClientCall(
        final MethodDescriptor<ReqT, RespT> methodDescriptor,
        final ClientCall<ReqT, RespT> delegate) {
      super(delegate);
      this.methodDescriptor = methodDescriptor;
    }
  }
}
