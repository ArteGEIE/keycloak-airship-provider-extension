package com.cloudiam.keycloak.airship;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AirshipEmailFactory implements EmailSenderProviderFactory {

    public static final String PROVIDER_ID = "keycloak-airship-provider";
    private static final Logger LOGGER = Logger.getLogger(AirshipEmailFactory.class);
    private String apiEndpoint;
    private String airShipDomain;
    private String accessToken;
    private String appKey;
    private String airshipHeader;
    private String senderEmail;
    private String senderName;
    private String replyTo;

    private static final String DEFAULT_AIRSHIP_HEADER = "vnd.urbanairship+json";
    private static final String DEFAULT_AIRSHIP_DOMAIN = "https://go.airship.eu";

    @Override
    public void init(Config.Scope config) {
        LOGGER.info("******** INITIALIZING AIRSHIP EMAIL SENDER PROVIDER ********");

        // Load required environment variables
        this.apiEndpoint = getEnvOrThrow("AIRSHIP_ENDPOINT");
        this.accessToken = getEnvOrThrow("AIRSHIP_ACCESS_TOKEN");
        this.appKey = getEnvOrThrow("AIRSHIP_APP_KEY");
        this.airshipHeader = getEnvOrDefault("AIRSHIP_HEADER", DEFAULT_AIRSHIP_HEADER);
        this.airShipDomain = getEnvOrDefault("AIRSHIP_DOMAIN", DEFAULT_AIRSHIP_DOMAIN);

        LOGGER.info("Airship Email Configuration:");
        LOGGER.infof("API URL: %s", apiEndpoint);
        LOGGER.infof("Domain: %s", airShipDomain);
        LOGGER.info(accessToken != null ? "Access Token: [CONFIGURED]" : "Access Token: [MISSING]");
        LOGGER.info(appKey != null ? "App Key: [CONFIGURED]" : "App Key: [MISSING]");
        LOGGER.info(airshipHeader != null ? "Header: [CONFIGURED]" : "Header: [MISSING]");

        if (apiEndpoint.isEmpty() || accessToken.isEmpty() || appKey.isEmpty()) {
            throw new IllegalStateException("Missing required Airship environment variables.");
        }

        LOGGER.info("******** AIRSHIP EMAIL SENDER PROVIDER INITIALIZING SUCCESSFULLY ********");
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        this.senderEmail = getEnvOrDefault("AIRSHIP_EMAIL_SENDER", getFromEmail(session));
        this.senderName = getEnvOrDefault("AIRSHIP_EMAIL_SENDER_NAME", getFromDisplay(session));
        this.replyTo = getEnvOrDefault("AIRSHIP_EMAIL_REPLY_TO", getReplyToDisplay(session) + " <" + getReplyTo(session) + ">");

        LOGGER.info("Email Configuration:");
        LOGGER.infof("Sender Email: %s", senderEmail);
        LOGGER.infof("Sender Name: %s", senderName);
        LOGGER.infof("Reply To: %s", replyTo);

        return new AirshipEmailProvider(session, apiEndpoint, airShipDomain, accessToken, appKey, airshipHeader, senderEmail, senderName, replyTo);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
        LOGGER.debug("******** CLOSING AIRSHIP EMAIL SENDER PROVIDER ********");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    // Make sure it overrides default email provider
    @Override
    public int order() {
        return 100;
    }

    private String getFromEmail(KeycloakSession session) {
        return session.getContext().getRealm().getSmtpConfig().get("from");
    }

    private String getFromDisplay(KeycloakSession session) {
        return session.getContext().getRealm().getSmtpConfig().get("fromDisplayName");
    }

    private String getReplyTo(KeycloakSession session) {
        return session.getContext().getRealm().getSmtpConfig().get("replyTo");
    }

    private String getReplyToDisplay(KeycloakSession session) {
        return session.getContext().getRealm().getSmtpConfig().get("replyToDisplayName");
    }

    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        if (value == null || value.isEmpty()) {
            LOGGER.warnf("Environment variable %s not set, using default value: %s", envVar, defaultValue);
            return defaultValue;
        }
        return value;
    }

    private String getEnvOrThrow(String envVar) {
        String value = System.getenv(envVar);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + envVar);
        }
        return value;
    }
}
