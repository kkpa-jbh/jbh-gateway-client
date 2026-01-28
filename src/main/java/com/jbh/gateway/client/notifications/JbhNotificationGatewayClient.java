package com.jbh.gateway.client.notifications;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
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
        SendNotificationRequest request,
        NotificationType type,
        Map<String, String> headers
    ) throws JbhGatewayException;

    /**
     * Sends an EMAIL notification (default type).
     *
     * @param request the notification request
     * @param headers HTTP headers including Authorization
     * @return JbhHttpResponse with the result
     * @throws JbhGatewayException if the request fails
     */
    default JbhHttpResponse sendEmailNotification(
        SendNotificationRequest request,
        Map<String, String> headers
    ) throws JbhGatewayException {
        return sendNotification(request, NotificationType.EMAIL, headers);
    }
}
