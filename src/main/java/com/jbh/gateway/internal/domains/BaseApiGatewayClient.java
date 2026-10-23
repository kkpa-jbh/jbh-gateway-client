package com.jbh.gateway.internal.domains;

import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.REQ_SOURCE_HEADER;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.JbhGatewayInternalClientImpl;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.adapter.NativeHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.validators.JbhHttpValidator;
import com.jbh.gateway.internal.core.validators.JbhHttpValidatorImpl;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class BaseApiGatewayClient {
  protected final JbhHttpValidator validator;
  private final JbhGatewayInternalClientImpl jbhGatewayClient;
  private final JbhGatewayClientBuilder gatewayConfig;
  private static final String JBH_API_GATEWAY_PREFIX = "/jbh-api";

  // Error codes for requests that got no HTTP response. See README "Exception Handling".
  public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
  public static final String TIMEOUT = "TIMEOUT";
  public static final String IO_ERROR = "IO_ERROR";
  public static final String INTERRUPTED = "INTERRUPTED";

  protected Map<String,String> callerConfigHeaders = new HashMap<>();

  public BaseApiGatewayClient(JbhGatewayClientBuilder gatewayConfig) throws JbhGatewayException {
    this(gatewayConfig, null);
  }

  /**
   * @param gatewayConfig the caller configuration
   * @param httpClientAdapter the transport. Tests pass their own. {@code null} uses {@link
   *     NativeHttpClientAdapter}.
   */
  public BaseApiGatewayClient(
      JbhGatewayClientBuilder gatewayConfig, JbhHttpClientAdapter httpClientAdapter)
      throws JbhGatewayException {
    this.gatewayConfig = gatewayConfig;
    this.validator = new JbhHttpValidatorImpl();
    JbhHttpClientConfig httpClientConfig = JbhHttpClientConfig.defaultConfig();
    if (this.gatewayConfig.getConnectTimeout() != null) {
      httpClientConfig.setConnectTimeout(this.gatewayConfig.getConnectTimeout());
    }
    if (this.gatewayConfig.getRequestTimeout() != null) {
      httpClientConfig.setRequestTimeout(this.gatewayConfig.getRequestTimeout());
    }

    if (this.gatewayConfig.getSourceService() != null) {
      Map<String, String> customHeaders = httpClientConfig.getCustomHeaders();
      if (customHeaders == null) {
        customHeaders = new HashMap<>();
      }
      customHeaders.put(REQ_SOURCE_HEADER, this.gatewayConfig.getSourceService());
      httpClientConfig.setCustomHeaders(customHeaders);
    }

    this.callerConfigHeaders = httpClientConfig.getCustomHeaders();

    validator.vaildateClientSourceHeader(JbhHttpHeaders.fromMap(callerConfigHeaders));

    JbhHttpClientAdapter adapter =
        httpClientAdapter != null ? httpClientAdapter : new NativeHttpClientAdapter(httpClientConfig);
    this.jbhGatewayClient =
        new JbhGatewayInternalClientImpl(getGatewayHostUrl(), httpClientConfig, adapter);
  }

  protected String getGatewayHostUrl() {
    return gatewayConfig.getBaseUrl();
  }

  private String normalizePath(String path) {
    if (path == null) {
      return null;
    }

    // Ensure path starts with /
    return path.startsWith("/") ? path : "/" + path;
  }

  protected JbhHttpResponse executeGet(String path, JbhHttpHeaders clientHttpHeaders)
      throws JbhGatewayException {
    String fullPath = JBH_API_GATEWAY_PREFIX + normalizePath(path);
    return callGateway("GET", fullPath, () -> jbhGatewayClient.get(fullPath, clientHttpHeaders));
  }

  /** GET that also reads a JSON body of a 2xx response as {@code responseType}. */
  protected <T> JbhHttpResponse executeGet(
      String path, JbhHttpHeaders clientHttpHeaders, Class<T> responseType)
      throws JbhGatewayException {
    String fullPath = JBH_API_GATEWAY_PREFIX + normalizePath(path);
    return callGateway(
        "GET", fullPath, () -> jbhGatewayClient.get(fullPath, clientHttpHeaders, responseType));
  }

  protected JbhHttpResponse executePost(String path, String jsonBody, JbhHttpHeaders clientHttpHeaders)
      throws JbhGatewayException {
    String fullPath = JBH_API_GATEWAY_PREFIX + normalizePath(path);
    return callGateway(
        "POST", fullPath, () -> jbhGatewayClient.post(fullPath, jsonBody, clientHttpHeaders));
  }

  protected JbhHttpResponse executePut(String path, String jsonBody, JbhHttpHeaders clientHttpHeaders)
      throws JbhGatewayException {
    String fullPath = JBH_API_GATEWAY_PREFIX + normalizePath(path);
    return callGateway(
        "PUT", fullPath, () -> jbhGatewayClient.put(fullPath, jsonBody, clientHttpHeaders));
  }

  protected JbhHttpResponse executeDelete(String path, JbhHttpHeaders clientHttpHeaders)
      throws JbhGatewayException {
    String fullPath = JBH_API_GATEWAY_PREFIX + normalizePath(path);
    return callGateway("DELETE", fullPath, () -> jbhGatewayClient.delete(fullPath, clientHttpHeaders));
  }

  /** One call to the gateway. It throws the network error of the adapter as it is. */
  @FunctionalInterface
  private interface GatewayCall {
    JbhHttpResponse send() throws IOException, InterruptedException;
  }

  /** Runs one call. A request with no HTTP response becomes a {@link JbhGatewayException}. */
  private JbhHttpResponse callGateway(String method, String path, GatewayCall call)
      throws JbhGatewayException {
    try {
      return call.send();
    } catch (IOException | InterruptedException e) {
      throw toGatewayException(method + " " + path, e);
    }
  }

  /** The single place that maps network errors to error codes. */
  static JbhGatewayException toGatewayException(String request, Exception error) {
    if (error instanceof InterruptedException) {
      Thread.currentThread().interrupt();
      return new JbhGatewayException("Request was interrupted: " + request, INTERRUPTED, null, error);
    }
    if (error instanceof ConnectException) {
      return new JbhGatewayException("Connection failed: " + request, CONNECTION_FAILED, null, error);
    }
    if (error instanceof HttpTimeoutException) {
      return new JbhGatewayException("Request timed out: " + request, TIMEOUT, null, error);
    }
    return new JbhGatewayException("IO error: " + request, IO_ERROR, null, error);
  }
}
