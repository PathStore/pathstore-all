/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package pathstore.authentication.grpc;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

/**
 * This class is used as the auth interceptor for the grpc server. The onMessage function gets
 * called whenever a new message comes in. This is where we will verify that the proper
 * authentication is present within the meta data.
 *
 * @see AuthManager for the logic behind the construction of who is capable of accessing which
 *     endpoints
 */
@RequiredArgsConstructor
public class AuthServerInterceptor implements ServerInterceptor {
  /** Logger to log each message incoming and out going at finest */
  private static final PathStoreLogger logger =
      PathStoreLoggerFactory.getLogger(AuthServerInterceptor.class);

  /** Auth manager instance */
  private final AuthManager authManager;

  /**
   * This function is called every time a message is coming into the grpc server.
   *
   * <p>The logic is first convert the incoming call to a custom {@link GRPCServerCall} so we can
   * monitor its activity, then start the next call using the newly created custom call and pass the
   * meta data. Then create a {@link GRPCForwardingServerCallListener} and when the message is
   * received it will verify that the authentication information is valid
   *
   * @param serverCall reference to the actual call going into the network, this allows you to close
   *     the call if the user is un-authenticated
   * @param metadata metadata provided from the client. This will contain the authentication
   *     information
   * @param next allows you to forward the current call to the next step.
   * @param <ReqT> type of the incoming request
   * @param <RespT> type of the outgoing request
   * @return a {@link GRPCForwardingServerCallListener} either to the service as normal or closes
   *     the service as the payload is improperly authenticated
   */
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> serverCall,
      final Metadata metadata,
      final ServerCallHandler<ReqT, RespT> next) {

    GRPCServerCall<ReqT, RespT> grpcServerCall = new GRPCServerCall<>(serverCall);

    ServerCall.Listener<ReqT> listener = next.startCall(grpcServerCall, metadata);

    return new GRPCForwardingServerCallListener<ReqT>(listener) {
      @Override
      public void onMessage(final ReqT message) {
        logger.finest(
            String.format(
                "On Message Method: %s, Message: {%s}",
                grpcServerCall.getMethodDescriptor().getFullMethodName(),
                message.toString().replace("\n", "")));

        String username = metadata.get(Keys.USERNAME);
        String password = metadata.get(Keys.PASSWORD);

        if (!authManager.isAuthenticated(
            grpcServerCall.getMethodDescriptor().getFullMethodName(), username, password)) {
          serverCall.close(Status.UNAUTHENTICATED, new Metadata());
        } else super.onMessage(message);
      }
    };
  }

  /**
   * This class is used to take a server call and wrap it so that we can modify the behaviour of the
   * request that gets sent out after the authentication phase. This is for two reasons, a) we can
   * print out what the services response message is and b) override the close function as we call
   * close on incoming server calls if the payload is un-authenticated
   *
   * @param <ReqT> request type of incoming payload
   * @param <RespT> request type of outgoing payload
   */
  private static class GRPCServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT> {

    /**
     * Server call from the {@link ServerInterceptor#interceptCall(ServerCall, Metadata,
     * ServerCallHandler)} function
     */
    private final ServerCall<ReqT, RespT> serverCall;

    /** @param serverCall {@link #serverCall} */
    GRPCServerCall(final ServerCall<ReqT, RespT> serverCall) {
      this.serverCall = serverCall;
    }

    /**
     * Requests up to the given number of messages from the call to be delivered to
     * ServerCall.Listener.onMessage(Object). Once numMessages have been delivered no further
     * request messages will be delivered until more messages are requested by calling this method
     * again. Servers use this mechanism to provide back-pressure to the client for flow-control.
     *
     * <p>This method is safe to call from multiple threads without external synchronization.
     *
     * @param numMessages the requested number of messages to be delivered to the listener
     */
    @Override
    public void request(final int numMessages) {
      this.serverCall.request(numMessages);
    }

    /**
     * Send response header metadata prior to sending a response message. This method may only be
     * called once and cannot be called after calls to sendMessage(RespT) or close(io.grpc.Status,
     * io.grpc.Metadata). Since Metadata is not thread-safe, the caller must not access (read or
     * write) headers after this point.
     *
     * @param metadata metadata to send prior to any response body
     */
    @Override
    public void sendHeaders(final Metadata metadata) {
      this.serverCall.sendHeaders(metadata);
    }

    /**
     * Send a response message. Messages are the primary form of communication associated with RPCs.
     * Multiple response messages may exist for streaming calls.
     *
     * @param respT response message
     */
    @Override
    public void sendMessage(final RespT respT) {
      logger.finest(
          String.format(
              "Send message: Method: %s Response: {%s}",
              this.serverCall.getMethodDescriptor().getFullMethodName(),
              respT.toString().replace("\n", "")));

      this.serverCall.sendMessage(respT);
    }

    /**
     * Close the call with the provided status. No further sending or receiving will occur. If
     * Status.isOk() is false, then the call is said to have failed. If no errors or cancellations
     * are known to have occurred, then a ServerCall.Listener.onComplete() notification should be
     * expected, independent of status. Otherwise ServerCall.Listener.onCancel() has been or will be
     * called.
     *
     * <p>Since Metadata is not thread-safe, the caller must not access (read or write) trailers
     * after this point.
     *
     * @param status status to close on
     * @param metadata metadata to send if status is ok
     */
    @Override
    public void close(final Status status, final Metadata metadata) {
      if (this.serverCall.isReady()) this.serverCall.close(status, metadata);
    }

    /**
     * @return Returns true when the call is cancelled and the server is encouraged to abort
     *     processing to * save resources, since the client will not be processing any further
     *     methods. Cancellations * can be caused by timeouts, explicit cancel by client, network
     *     errors, and similar. This * method may safely be called concurrently from multiple
     *     threads.
     */
    @Override
    public boolean isCancelled() {
      return this.serverCall.isCancelled();
    }

    /** @return The MethodDescriptor for the call. */
    @Override
    public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
      return this.serverCall.getMethodDescriptor();
    }
  }

  /**
   * Forwarding Server Call for the interceptor to be able to build a custom call to use.
   *
   * @param <ReqT> Request type incoming
   */
  private static class GRPCForwardingServerCallListener<ReqT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    /** @param delegate Listener generated for next step in the call */
    protected GRPCForwardingServerCallListener(final ServerCall.Listener<ReqT> delegate) {
      super(delegate);
    }
  }
}
