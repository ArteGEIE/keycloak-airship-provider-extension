package com.keycloak.airship;

import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.logging.Logger;

public class AirshipEmailFactory implements EmailSenderProviderFactory {

    public static final String PROVIDER_ID = "keycloak-airship-provider";
    private Logger logger = Logger.getLogger(AirshipEmailFactory.class.getName());
    private String apiUrl;
    private String airShipDomain;
    private String accessToken;
    private String appKey;
    private String defaultSender;
    private String airshipHeader;

    @Override
    public void init(Config.Scope config) {
        this.apiUrl = System.getenv("AIRSHIP_API_URL");
        this.airShipDomain = System.getenv("AIRSHIP_DOMAIN");
        this.accessToken = System.getenv("AIRSHIP_ACCESS_TOKEN");
        this.appKey = System.getenv("AIRSHIP_APP_KEY");
        this.defaultSender = System.getenv("AIRSHIP_EMAIL_SENDER");
        this.airshipHeader = System.getenv("AIRSHIP_HEADER");

        if (apiUrl == null || accessToken == null || appKey == null || defaultSender == null || airshipHeader == null || airShipDomain == null) {
            throw new IllegalStateException("Missing required Airship environment variables.");
        }

        logger.info("Airship Email Factory initialized successfully.");
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new AirshipEmailProvider(apiUrl, airShipDomain, accessToken, appKey, defaultSender, airshipHeader);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}