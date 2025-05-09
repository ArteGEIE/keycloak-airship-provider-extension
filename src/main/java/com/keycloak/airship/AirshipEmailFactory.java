package com.keycloak.airship;

import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AirshipEmailFactory implements EmailSenderProviderFactory {

    public static final String PROVIDER_ID = "keycloak-airship-provider";

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new AirshipEmailProvider(session);
    }

    @Override
    public void init(Config.Scope scope) {
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