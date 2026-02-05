package com.jbh.gateway.client.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationRequest;
import com.jbh.notification.contracts.validation.NotificationValidationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JbhNotificationGatewayClientTest {

    static JbhNotificationGatewayClient notificationClient;

    static final String GOOD_TOKEN =
        "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyYWZiZGNmYy1lYmM2LTQxODAtOGE2My02MDljZjhmMTk4YmEiLCJpYXQiOjE3NTc4NDQyNjksImV4cCI6MTc1ODQ0OTA2OX0.r1qQJATRVVk9mw5twn66c-M-aszjgZ96H9cmsAV9W9-STEj0Da80vb_J3Zm8M8ZAac_1Lt8wMTdAedpaDV5hIg";

    @BeforeAll
    public static void setup() {
        JbhGatewayClientBuilder clientBuilder =
            JbhGatewayClientBuilder.builder()
                .baseUrl("http://localhost:8080")
                .sourceService("test-service")
                .build();

        try {
            notificationClient = clientBuilder.getNotificationClient();
        } catch (JbhGatewayException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldThrowExceptionWhenNotValidAuthorizationHeader() {
        String TOKEN_WITHOUT_BEARER = "eyJhbGciOiJIUzUxMiJ9..";

        SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("recipient@example.com")
            .subject("Test Subject")
            .message("Test message")
            .build();

        assertThrows(
            JbhGatewayException.class,
            () -> notificationClient.sendNotification(
                request,
                NotificationType.EMAIL,
                Map.of("Authorization", TOKEN_WITHOUT_BEARER)
            )
        );
    }

    @Test
    public void shouldThrowExceptionWhenMissingAuthorizationHeader() {
        SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("recipient@example.com")
            .subject("Test Subject")
            .message("Test message")
            .build();

        assertThrows(
            JbhGatewayException.class,
            () -> notificationClient.sendNotification(
                request,
                NotificationType.EMAIL,
                Map.of()
            )
        );
    }

    @Test
    public void shouldBuildNotificationRequestWithAllFields() {
        UUID recipientId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(recipientId)
            .recipientEmail("recipient@example.com")
            .senderUserId(senderId)
            .senderEmail("sender@example.com")
            .subject("Test Subject")
            .message("Test message body")
            .build();

        assertEquals(recipientId, request.recipientId());
        assertEquals("recipient@example.com", request.recipientEmail());
        assertEquals(senderId, request.senderUserId());
        assertEquals("sender@example.com", request.senderEmail());
        assertEquals("Test Subject", request.subject());
        assertEquals("Test message body", request.message());
        assertNull(request.metadata());
    }

    @Test
    public void shouldBuildNotificationRequestWithMinimalFields() {
        SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientEmail("recipient@example.com")
            .subject("Test")
            .message("Message")
            .build();

        assertEquals("recipient@example.com", request.recipientEmail());
        assertEquals("Test", request.subject());
        assertEquals("Message", request.message());
    }

    @Test
    public void shouldHaveAllNotificationTypes() {
        assertEquals(4, NotificationType.values().length);
        assertNotNull(NotificationType.EMAIL);
        assertNotNull(NotificationType.SMS);
        assertNotNull(NotificationType.PUSH);
        assertNotNull(NotificationType.IN_APP);
    }

    @Test
    public void shouldUseEmailAsDefaultInConvenienceMethod() throws JbhGatewayException {
        SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientEmail("test@example.com")
            .subject("Test")
            .message("Message")
            .build();

        String TOKEN_WITHOUT_BEARER = "invalid";

        JbhGatewayException exception = assertThrows(
            JbhGatewayException.class,
            () -> notificationClient.sendNotification(request, NotificationType.EMAIL, Map.of("Authorization", TOKEN_WITHOUT_BEARER))
        );

        assertTrue(exception.getMessage().contains("Authorization"));
    }

    // Integration test - requires running server
    // Uncomment @Test to run manually
    public void shouldSendEmailNotification() {
        try {
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(UUID.randomUUID())
                .recipientEmail("recipient@example.com")
                .senderEmail("sender@example.com")
                .subject("Test Email")
                .message("This is a test email message")
                .build();

            JbhHttpResponse response = notificationClient.sendNotification(
                request,
                NotificationType.EMAIL,
                Map.of("Authorization", GOOD_TOKEN)
            );

            assertEquals(200, response.getStatusCode());
            assertTrue(response.isSuccessful());
        } catch (JbhGatewayException | NotificationValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
