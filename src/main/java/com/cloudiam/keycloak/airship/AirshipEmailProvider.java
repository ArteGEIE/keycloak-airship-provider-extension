package com.cloudiam.keycloak.airship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public class AirshipEmailProvider implements EmailSenderProvider {

    private static final Logger LOGGER = Logger.getLogger(AirshipEmailFactory.class);
    private final String apiUrl;
    private final String airShipDomain;
    private final String accessToken;
    private final String appKey;
    private final String defaultSender;
    private final String airshipHeader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeycloakSession session;

    public AirshipEmailProvider(KeycloakSession session, String apiUrl, String airShipDomain, String accessToken, String appKey, String defaultSender, String airshipHeader) {
        this.apiUrl = apiUrl;
        this.airShipDomain = airShipDomain;
        this.accessToken = accessToken;
        this.appKey = appKey;
        this.defaultSender = defaultSender;
        this.airshipHeader = airshipHeader;
        this.session = session;
    }
    @Override
    public void send(Map<String, String> config, String address, String subject, String textBody, String htmlBody) throws EmailException {
        try {
            LOGGER.info("******** START AIRSHIP EMAIL SENDING ********");

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

            // Create categories node
            ArrayNode categories = objectMapper.createArrayNode();
            categories.add("keycloak");

            email.put("subject", subject);
            email.put("message_type", "transactional");

            // Set sender information
            String senderName = config.getOrDefault("senderName", "Keycloak");

            // Ensure valid sender email
            String senderEmail = defaultSender;
            config.getOrDefault("from", senderEmail);
            config.getOrDefault("replyTo", senderEmail);

            email.put("sender_name", senderName);
            email.put("sender_address", senderEmail);
            email.put("reply_to", senderEmail);

            // Set email content
            if (htmlBody != null && !htmlBody.isEmpty()) {
                email.put("html_body", htmlBody);
            }

            if (textBody != null && !textBody.isEmpty()) {
                email.put("plaintext_body", textBody);
            }

            notification.set("email", email);
            payload.set("notification", notification);

            // Create campaigns node and add categories
            ObjectNode campaigns = objectMapper.createObjectNode();
            campaigns.set("categories", categories);
            payload.set("campaigns", campaigns);

            // Convert to JSON and send
            String json = objectMapper.writeValueAsString(payload);
            LOGGER.tracef("Request payload: %s", json);

            String fullUrl = airShipDomain + apiUrl;
            LOGGER.debugf("Sending HTTP request to %s", fullUrl);

            // Build request with proper headers
            SimpleHttp simpleHttp = SimpleHttp.doPost(fullUrl, session)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/" + airshipHeader + "; version=3")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-UA-App-Key", appKey)
                    .json(payload);

            SimpleHttp.Response response = simpleHttp.asResponse();


            if (response.getStatus() >= 400) {
                LOGGER.errorf("Failed to send email. Response code: %s, body: %s", response.getStatus(), response.asString());
                throw new EmailException("Airship API responded with error: " + response.getStatus() + " - " + response.asString());
            }

            LOGGER.debugf("******** AIRSHIP EMAIL SENT SUCCESSFULLY to %s. ********", address);
        } catch (Exception e) {
            LOGGER.error("Failed to send email via Airship", e);
            throw new EmailException("Failed to send email via Airship", e);
        }
    }

    @Override
    public void close() {
        // Do nothing
    }
}
