package com.jbh.gateway.client.notifications;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
    UUID recipientId,
    String recipientEmail,
    UUID senderUserId,
    String senderEmail,
    String subject,
    String message,
    String emailTemplate,
    Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID recipientId;
        private String recipientEmail;
        private UUID senderUserId;
        private String senderEmail;
        private String subject;
        private String message;
        private String emailTemplate;
        private Map<String, Object> metadata;

        public Builder recipientId(UUID recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder recipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public Builder senderUserId(UUID senderUserId) {
            this.senderUserId = senderUserId;
            return this;
        }

        public Builder senderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder emailTemplate(String emailTemplate) {
            this.emailTemplate = emailTemplate;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SendNotificationRequest build() {
            return new SendNotificationRequest(
                recipientId,
                recipientEmail,
                senderUserId,
                senderEmail,
                subject,
                message,
                emailTemplate,
                metadata
            );
        }
    }
}
