package com.jbh.gateway.internal.domains.notifications;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.notifications.JbhNotificationGatewayClient;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.mappers.JacksonJsonUtil;
import com.jbh.gateway.internal.domains.BaseApiGatewayClient;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationRequest;
import com.jbh.notification.contracts.validation.NotificationValidationException;
import com.jbh.notification.contracts.validation.NotificationValidator;
import java.util.Map;

public class JbhNotificationGatewayClientImpl extends BaseApiGatewayClient
    implements JbhNotificationGatewayClient {

  private static final String NOTIFICATIONS_PATH = "/notifications/v1";

  public JbhNotificationGatewayClientImpl(JbhGatewayClientBuilder hostConfig)
      throws JbhGatewayException {
    super(hostConfig);
  }

  @Override
  public JbhHttpResponse sendNotification(
      SendNotificationRequest request, NotificationType type, Map<String, String> clientHttpHeaders)
      throws JbhGatewayException, NotificationValidationException {
    JbhHttpHeaders headers = JbhHttpHeaders.fromMap(clientHttpHeaders);
    validator.validateAuthorizationHeader(headers);

    NotificationValidator.validate(type, request);

    String path = NOTIFICATIONS_PATH + "?type=" + type.name();
    String jsonBody;
    try {
      jsonBody = JacksonJsonUtil.toJson(request);
    } catch (Exception e) {
      throw new JbhGatewayException(
          "Failed to serialize notification request", "SERIALIZATION_ERROR", null, e);
    }

    return executePost(path, jsonBody, headers);
  }
}
