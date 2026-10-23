package com.jbh.gateway.internal.core.http;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import java.io.IOException;

/**
 * Sends one HTTP request and returns the response. This is the transport seam: domain clients
 * depend on it, so tests can pass their own implementation.
 *
 * <p>Network errors are thrown as they are. {@code BaseApiGatewayClient} maps them to {@code
 * JbhGatewayException}.
 */
@FunctionalInterface
public interface JbhHttpClientAdapter {

  /**
   * Execute an HTTP request synchronously.
   *
   * @param request the HTTP request to execute
   * @return the HTTP response, for any status code
   * @throws IOException when no HTTP response was received
   * @throws InterruptedException when the thread was interrupted
   */
  JbhHttpResponse execute(JbhHttpRequest request) throws IOException, InterruptedException;
}
