package com.cloudiam.keycloak.airship;

import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirshipEmailFactory implements EmailSenderProviderFactory {

    public static final String PROVIDER_ID = "default";
    private final Logger logger = LoggerFactory.getLogger(AirshipEmailFactory.class);
    private String apiEndpoint;
    private String airShipDomain;
    private String accessToken;
    private String appKey;
    private String defaultSender;
    private String airshipHeader;

    @Override
    public void init(Config.Scope config) {
        logger.info("******** INITIALIZING AIRSHIP EMAIL SENDER PROVIDER ********");

        this.apiEndpoint = System.getenv("AIRSHIP_ENDPOINT");
        this.accessToken = System.getenv("AIRSHIP_ACCESS_TOKEN");
        this.appKey = System.getenv("AIRSHIP_APP_KEY");
        this.defaultSender = System.getenv("AIRSHIP_EMAIL_SENDER");
        this.airshipHeader = System.getenv("AIRSHIP_HEADER").isEmpty() ? "vnd.urbanairship+json" : System.getenv("AIRSHIP_HEADER");
        this.airShipDomain = System.getenv("AIRSHIP_DOMAIN").isEmpty() ? "https://go.airship.eu" : System.getenv("AIRSHIP_DOMAIN");

        // Log configuration
        logger.info("Airship Email Configuration:");
        logger.info("API URL: {}", this.apiEndpoint);
        logger.info("Domain: {}", airShipDomain);
        logger.info( accessToken != null ? "Access Token: [CONFIGURED]" : "Access Token: [MISSING]");
        logger.info(appKey != null ? "App Key: [CONFIGURED]" : "App Key: [MISSING]");
        logger.info("Default Sender: {}", defaultSender);
        logger.info(airshipHeader != null ? "Header: [CONFIGURED]" : "Header: [MISSING]");

        if (apiEndpoint == null || accessToken == null || appKey == null || defaultSender == null) {
            throw new IllegalStateException("Missing required Airship environment variables.");
        }

        logger.info("******** AIRSHIP EMAIL SENDER PROVIDER INITIALIZING SUCCESSFULLY ********");
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new AirshipEmailProvider(apiEndpoint, airShipDomain, accessToken, appKey, defaultSender, airshipHeader);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
        logger.info("******** CLOSING AIRSHIP EMAIL SENDER PROVIDER ********");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
