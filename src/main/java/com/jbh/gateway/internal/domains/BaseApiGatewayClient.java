package com.jbh.gateway.internal.domains;

import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.REQ_SOURCE_HEADER;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.internal.JbhGatewayInternalClientImpl;
import com.jbh.gateway.internal.JbhGatewayInternalClient;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.AdapterType;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.core.validators.JbhHttpValidator;
import com.jbh.gateway.internal.core.validators.JbhHttpValidatorImpl;
import java.util.HashMap;
import java.util.Map;

public class BaseApiGatewayClient {
  protected final JbhHttpValidator validator;
  private final JbhGatewayInternalClient jbhGatewayClient;
  private final JbhGatewayClientBuilder gatewayConfig;
  private static final String JBH_API_GATEWAY_PREFIX = "/jbh-api";

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

  protected JbhHttpResponse executeGet(String path, JbhHttpHeaders clientHttpHeaders) {
    return jbhGatewayClient.get(JBH_API_GATEWAY_PREFIX + normalizePath(path), clientHttpHeaders);
  }

  protected JbhHttpResponse executePost(String path, String jsonBody, JbhHttpHeaders clientHttpHeaders) {
    return jbhGatewayClient.post(JBH_API_GATEWAY_PREFIX + normalizePath(path), jsonBody, clientHttpHeaders);
  }

  protected JbhHttpResponse executePut(String path, String jsonBody, JbhHttpHeaders clientHttpHeaders) {
    return jbhGatewayClient.put(JBH_API_GATEWAY_PREFIX + normalizePath(path), jsonBody, clientHttpHeaders);
  }

  protected JbhHttpResponse executeDelete(String path, JbhHttpHeaders clientHttpHeaders) {
    return jbhGatewayClient.delete(JBH_API_GATEWAY_PREFIX + normalizePath(path), clientHttpHeaders);
  }
}
