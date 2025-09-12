package com.jbh.gateway.internal.domains;

import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.REQ_SOURCE_HEADER;

import com.jbh.gateway.client.JbhGatewayClient;
import com.jbh.gateway.internal.JbhGatewayClientImpl;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.AdapterType;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpResponse;
import com.jbh.gateway.internal.core.validators.JbhHttpValidator;
import com.jbh.gateway.internal.core.validators.JbhHttpValidatorImpl;
import java.util.HashMap;
import java.util.Map;

public class BaseApiGatewayClient {
  protected final JbhHttpValidator validator;
  private final com.jbh.gateway.internal.JbhGatewayClient jbhGatewayClient;
  private final JbhGatewayClient gatewayConfig;
  private static final String JBH_APIGATEWAY_PREFIX = "/jbh-api";

  public BaseApiGatewayClient(JbhGatewayClient gatewayConfig) {
    this(gatewayConfig, new JbhHttpValidatorImpl());
  }

  public BaseApiGatewayClient(JbhGatewayClient gatewayConfig, JbhHttpValidator validator) {
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

    this.jbhGatewayClient =
        new JbhGatewayClientImpl(getGatewayHostUrl(), AdapterType.NATIVE, httpClientConfig);
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

  protected JbhHttpResponse getCall(String path, JbhHttpHeaders headers) {
    return jbhGatewayClient.get(JBH_APIGATEWAY_PREFIX + normalizePath(path), headers);
  }
}
