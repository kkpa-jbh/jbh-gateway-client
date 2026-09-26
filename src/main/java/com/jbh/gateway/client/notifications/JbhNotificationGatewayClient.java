package com.jbh.gateway.client.notifications;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationCommand;
import com.jbh.notification.contracts.validation.NotificationValidationException;
import java.util.Map;

public interface JbhNotificationGatewayClient {

    /**
     * Sends a notification using the specified type.
     *
     * @param request the notification request containing recipient, subject, message, etc.
     * @param type the notification type (EMAIL, SMS, PUSH, IN_APP)
     * @param headers HTTP headers including Authorization
     * @return JbhHttpResponse with the result
     * @throws JbhGatewayException if the request fails
     */
    JbhHttpResponse sendNotification(
        SendNotificationCommand request,
        NotificationType type,
        Map<String, String> headers
    ) throws JbhGatewayException, NotificationValidationException;

}
