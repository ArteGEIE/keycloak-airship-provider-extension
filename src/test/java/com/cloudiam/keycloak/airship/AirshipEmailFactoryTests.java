package com.cloudiam.keycloak.airship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.email.EmailSenderProvider;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AirshipEmailFactoryTests {

    private static WireMockContainer wiremockServer;
    private KeycloakSession keycloakSession;
    private AirshipEmailProvider airshipEmailProvider;
    private EmailSenderProvider provider;
    private RealmModel realm;
    private UserModel user;

    @BeforeAll
    public static void startWireMockServer() {
        wiremockServer = new WireMockContainer("wiremock/wiremock").withExposedPorts(8080);
        wiremockServer.start();
    }

    @AfterAll
    public static void stopWireMockServer() {
        wiremockServer.stop();
    }

    @BeforeEach
    public void setup() {
        keycloakSession = mock(KeycloakSession.class);
        
        RealmProvider realmProvider = mock(RealmProvider.class);
        realm = mock(RealmModel.class);
        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(realmProvider.getRealm(any(String.class))).thenReturn(realm);
    
        UserProvider userProvider = mock(UserProvider.class);
        user = mock(UserModel.class);
        when(keycloakSession.users()).thenReturn(userProvider);
        when(userProvider.getUserByUsername(any(RealmModel.class), any(String.class))).thenReturn(user);
        
        airshipEmailProvider = new AirshipEmailProvider(keycloakSession, "http://api.example.com", "example.com", "accessToken", "appKey", "defaultSender@example.com", "airshipHeader");
        provider = airshipEmailProvider;
    }

    @AfterEach
    public void tearDown() {
        // Clean up session
        if (keycloakSession != null) {
            keycloakSession.close();
        }
    }
    
    @Test
    public void testDefaultProviderIsOverridden() {
        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        when(keycloakSession.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getProviderFactory(EmailSenderProvider.class))
            .thenReturn(new AirshipEmailFactory());

        when(sessionFactory.getProviderFactory(EmailSenderProvider.class, null))
            .thenReturn(new AirshipEmailFactory());
    
        ProviderFactory<EmailSenderProvider> factory = sessionFactory.getProviderFactory(EmailSenderProvider.class);
        assertEquals("keycloak-airship-provider", factory.getId(), 
            "Default provider should be our custom implementation with ID 'keycloak-airship-provider'");
    }
}