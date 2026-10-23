package com.jbh.gateway.internal.domains;

import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.REQ_SOURCE_HEADER;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.internal.JbhGatewayInternalClientImpl;
import com.jbh.gateway.internal.JbhGatewayInternalClient;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.AdapterType;
import com.jbh.gateway.internal.core.http.exception.HttpClientException;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.core.validators.JbhHttpValidator;
import com.jbh.gateway.internal.core.validators.JbhHttpValidatorImpl;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BaseApiGatewayClient {
  protected final JbhHttpValidator validator;
  private final JbhGatewayInternalClient jbhGatewayClient;
  private final JbhGatewayClientBuilder gatewayConfig;
  private static final String JBH_API_GATEWAY_PREFIX = "/jbh-api";

  // Error codes for requests that got no HTTP response. See README "Exception Handling".
  public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
  public static final String TIMEOUT = "TIMEOUT";
  public static final String IO_ERROR = "IO_ERROR";
  public static final String INTERRUPTED = "INTERRUPTED";
  public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";

  protected Map<String,String> callerConfigHeaders = new HashMap<>();

  public BaseApiGatewayClient(JbhGatewayClientBuilder gatewayConfig) throws JbhGatewayException {
    this(gatewayConfig, new JbhHttpValidatorImpl());
  }

  public BaseApiGatewayClient(JbhGatewayClientBuilder gatewayConfig, JbhHttpValidator validator)
      throws JbhGatewayException {
    this.gatewayConfig = gatewayConfig;
    this.validator = validator;
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

    this.jbhGatewayClient =
        new JbhGatewayInternalClientImpl(getGatewayHostUrl(), AdapterType.NATIVE, httpClientConfig);
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
        "GET", fullPath, () -> jbhGatewayClient.getForObject(fullPath, clientHttpHeaders, responseType));
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

  /** Runs one call. A request with no HTTP response becomes a {@link JbhGatewayException}. */
  private JbhHttpResponse callGateway(String method, String path, Supplier<JbhHttpResponse> call)
      throws JbhGatewayException {
    try {
      return call.get();
    } catch (HttpClientException e) {
      throw toGatewayException(method + " " + path, e.getCause() != null ? e.getCause() : e);
    }
  }

  /** The single place that maps network errors to error codes. */
  static JbhGatewayException toGatewayException(String request, Throwable error) {
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
    if (error instanceof IOException) {
      return new JbhGatewayException("IO error: " + request, IO_ERROR, null, error);
    }
    return new JbhGatewayException("Request failed: " + request, UNKNOWN_ERROR, null, error);
  }
}
