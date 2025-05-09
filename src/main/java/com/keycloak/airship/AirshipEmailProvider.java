package com.keycloak.airship;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AirshipEmailProvider implements EmailSenderProvider {

    private static final Logger logger = LoggerFactory.getLogger(AirshipEmailProvider.class);

    private final String airshipApiKey;
    private final String senderEmail;
    private final String airShipEndpointUrl;
    private final ObjectMapper objectMapper;

    public AirshipEmailProvider(KeycloakSession session) {

        // Load environment variables for Airship configuration
        this.airshipApiKey = System.getenv("AIRSHIP_API_KEY");
        this.senderEmail = System.getenv("SENDER_EMAIL");
        this.airShipEndpointUrl = System.getenv("AIRSHIP_ENDPOINT_URL");

        if (airshipApiKey == null || airshipApiKey.isEmpty()) {
            logger.error("AIRSHIP_API_KEY environment variable is not set");
        }

        if (senderEmail == null || senderEmail.isEmpty()) {
            logger.error("SENDER_EMAIL environment variable is not set");
        }

        if(airShipEndpointUrl == null || airShipEndpointUrl.isEmpty()) {
            logger.error("AIRSHIP_ENDPOINT_URL environment variable is not set");
        }

        this.objectMapper = new ObjectMapper();
    }


    @Override
    public void close() {

    }

    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
        logger.debug("Sending email via Airship to: {}", address);

        try {
            validateEmail(address);
            Map<String, Object> payload = buildAirshipPayload(address, subject, textBody, htmlBody);
            sendToAirship(payload);

            logger.debug("Email sent successfully via Airship");
        } catch (Exception e) {
            logger.error("Failed to send email via Airship", e);
            throw new EmailException("Failed to send email via Airship", e);
        }
    }

    private void validateEmail(String email) throws AddressException {
        new InternetAddress(email).validate();
    }

    private Map<String, Object> buildAirshipPayload(String toAddress, String subject, String textBody, String htmlBody) {
        Map<String, Object> payload = new HashMap<>();

        // Basic email details
        payload.put("to", toAddress);
        payload.put("from", senderEmail);
        payload.put("subject", subject);

        // Add content
        if (htmlBody != null && !htmlBody.isEmpty()) {
            payload.put("html_body", htmlBody);
        }

        if (textBody != null && !textBody.isEmpty()) {
            payload.put("text_body", textBody);
        }

        return payload;
    }

    private void sendToAirship(Map<String, Object> payload) throws IOException {
        if (airshipApiKey == null || airshipApiKey.isEmpty()) {
            throw new IOException("Airship API key is not configured");
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .version(java.net.http.HttpClient.Version.HTTP_2)
                    .build();

            // Convert payload to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(this.airShipEndpointUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + airshipApiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                logger.error("Failed to send email via Airship. Status code: {}", statusCode);
                logger.error("Response body: {}", response.body());
                throw new IOException("Failed to send email via Airship. Status code: " + statusCode);
            }

            logger.info("Email sent successfully via Airship API. Status code: {}", statusCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending email via Airship", e);
        }
    }
}