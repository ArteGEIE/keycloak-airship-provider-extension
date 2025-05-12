package com.keycloak.airship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.Config;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AirshipEmailFactoryTest {

    @Mock
    private KeycloakSession mockSession;
    
    @Mock
    private Config.Scope mockScope;
    
    private AirshipEmailFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AirshipEmailFactory();
        injectEnvironmentVariables();
    }

    private void injectEnvironmentVariables() throws Exception {
        try {
            factory.init(mockScope);
        } catch (Exception e) {
            // Expected to fail because env vars aren't set
        }

        String TEST_API_URL = "https://test-airship-api.com";
        injectField("apiUrl", TEST_API_URL);
        String TEST_ACCESS_TOKEN = "test-access-token";
        injectField("accessToken", TEST_ACCESS_TOKEN);
        String TEST_APP_KEY = "test-app-key";
        injectField("appKey", TEST_APP_KEY);
        String TEST_DEFAULT_SENDER = "test@sender.com";
        injectField("defaultSender", TEST_DEFAULT_SENDER);
        String TEST_AIRSHIP_HEADER = "vnd.urbanairship+json";
        injectField("airshipHeader", TEST_AIRSHIP_HEADER);
    }
    
    private void injectField(String fieldName, String value) throws Exception {
        Field field = AirshipEmailFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(factory, value);
    }

    @Test
    void shouldReturnCorrectProviderId() {
        assertEquals("keycloak-airship-provider", factory.getId());
    }
    
    @Test
    void shouldCreateAirshipEmailProvider() {
        EmailSenderProvider provider = factory.create(mockSession);

        assertNotNull(provider);
        assertThat(provider).isInstanceOf(AirshipEmailProvider.class);
    }
} 