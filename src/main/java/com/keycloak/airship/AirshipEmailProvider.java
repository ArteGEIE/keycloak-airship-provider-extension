package com.keycloak.airship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class AirshipEmailProvider implements EmailSenderProvider {

    private Logger logger = LoggerFactory.getLogger(AirshipEmailProvider.class);
    private final String apiUrl;
    private final String airShipDomain;
    private final String accessToken;
    private final String appKey;
    private final String defaultSender;
    private final String airshipHeader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public AirshipEmailProvider(String apiUrl, String airShipDomain, String accessToken, String appKey, String defaultSender, String airshipHeader) {
        this.apiUrl = apiUrl;
        this.airShipDomain = airShipDomain;
        this.accessToken = accessToken;
        this.appKey = appKey;
        this.defaultSender = defaultSender;
        this.airshipHeader = airshipHeader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
        try {
            logger.info("******** START AIRSHIP EMAIL SENDING ********");

            // Create the payload according to Airship API format
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode audience = objectMapper.createObjectNode();
            ObjectNode recipient = objectMapper.createObjectNode();
            ArrayNode createAndSend = objectMapper.createArrayNode();

            recipient.put("ua_address", address);
            createAndSend.add(recipient);
            audience.set("create_and_send", createAndSend);
            payload.set("audience", audience);

            // Set device_types to email
            ArrayNode deviceTypes = objectMapper.createArrayNode();
            deviceTypes.add("email");
            payload.set("device_types", deviceTypes);

            // Create notification content
            ObjectNode notification = objectMapper.createObjectNode();
            ObjectNode email = objectMapper.createObjectNode();

            email.put("subject", subject);
            email.put("message_type", "transactional");

            // Set sender information
            String senderName = config.getOrDefault("senderName", "Keycloak");

            // Ensure valid sender email
            String senderEmail = defaultSender;
            if (config.containsKey("from") && config.get("from") != null && !config.get("from").isEmpty()) {
                senderEmail = config.get("from");
            }
            if (senderEmail == null || senderEmail.isEmpty()) {
                senderEmail = "no-reply@example.com";
                logger.warn("No sender email found, using default: {}", senderEmail);
            }

            // Ensure valid reply-to address
            String replyTo = senderEmail; // Default to sender email
            if (config.containsKey("replyTo") && config.get("replyTo") != null && !config.get("replyTo").isEmpty()) {
                replyTo = config.get("replyTo");
            }

            email.put("sender_name", senderName);
            email.put("sender_address", senderEmail);
            email.put("reply_to", replyTo);

            // Set email content
            if (htmlBody != null && !htmlBody.isEmpty()) {
                email.put("html_body", htmlBody);
            }

            if (textBody != null && !textBody.isEmpty()) {
                email.put("plaintext_body", textBody);
            }

            notification.set("email", email);
            payload.set("notification", notification);

            // Convert to JSON and send
            String json = objectMapper.writeValueAsString(payload);
            logger.debug("Request payload: {}", json);

            String fullUrl = airShipDomain + apiUrl;
            logger.info("Sending HTTP request to {}", fullUrl);

            // Build request with proper headers
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/" + airshipHeader + "; version=3")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-UA-App-Key", appKey);

            // Finalize and send the request
            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.error("Failed to send email. Response code: {}, body: {}", response.statusCode(), response.body());
                throw new EmailException("Airship API responded with error: " + response.statusCode() + " - " + response.body());
            }

            logger.info("******** AIRSHIP EMAIL SENT SUCCESSFULLY to {}. ********", address);
        } catch (Exception e) {
            logger.error("Failed to send email via Airship", e);
            throw new EmailException("Failed to send email via Airship", e);
        }
    }

    @Override
    public void close() {

    }
}