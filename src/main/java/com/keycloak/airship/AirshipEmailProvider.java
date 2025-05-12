package com.keycloak.airship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class AirshipEmailProvider implements EmailSenderProvider {

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
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode audience = objectMapper.createObjectNode();

            audience.put("email", address);
            payload.set("audience", audience);

            ObjectNode sender = objectMapper.createObjectNode();
            sender.put("email_address", config.getOrDefault("from", defaultSender));
            payload.set("sender", sender);

            payload.put("subject", subject);
            payload.put("plain", textBody);
            payload.put("html", htmlBody);

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(airShipDomain + apiUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    //.header("X-UA-Appkey", appKey)
                    .header("Accept", "application/" + airshipHeader + "; version=3;")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new EmailException("Airship API responded with error: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            throw new EmailException("Failed to send email via Airship", e);
        }
    }

    @Override
    public void close() {

    }
}