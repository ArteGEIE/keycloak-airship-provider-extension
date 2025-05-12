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
    private String defaultSender;
    private String airshipHeader;

    @Override
    public void init(Config.Scope config) {
        LOGGER.info("******** INITIALIZING AIRSHIP EMAIL SENDER PROVIDER ********");

        this.apiEndpoint = System.getenv("AIRSHIP_ENDPOINT");
        this.accessToken = System.getenv("AIRSHIP_ACCESS_TOKEN");
        this.appKey = System.getenv("AIRSHIP_APP_KEY");
        this.defaultSender = System.getenv("AIRSHIP_EMAIL_SENDER");
        this.airshipHeader = System.getenv("AIRSHIP_HEADER").isEmpty() ? "vnd.urbanairship+json" : System.getenv("AIRSHIP_HEADER");
        this.airShipDomain = System.getenv("AIRSHIP_DOMAIN").isEmpty() ? "https://go.airship.eu" : System.getenv("AIRSHIP_DOMAIN");

        // Log configuration
        LOGGER.info("Airship Email Configuration:");
        LOGGER.infof("API URL: %s", this.apiEndpoint);
        LOGGER.infof("Domain: %s", airShipDomain);
        LOGGER.info(accessToken != null ? "Access Token: [CONFIGURED]" : "Access Token: [MISSING]");
        LOGGER.info(appKey != null ? "App Key: [CONFIGURED]" : "App Key: [MISSING]");
        LOGGER.infof("Default Sender: %s", defaultSender);
        LOGGER.info(airshipHeader != null ? "Header: [CONFIGURED]" : "Header: [MISSING]");

        if (apiEndpoint == null || accessToken == null || appKey == null || defaultSender == null) {
            throw new IllegalStateException("Missing required Airship environment variables.");
        }

        LOGGER.info("******** AIRSHIP EMAIL SENDER PROVIDER INITIALIZING SUCCESSFULLY ********");
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new AirshipEmailProvider(session, apiEndpoint, airShipDomain, accessToken, appKey, defaultSender, airshipHeader);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
        LOGGER.info("******** CLOSING AIRSHIP EMAIL SENDER PROVIDER ********");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
