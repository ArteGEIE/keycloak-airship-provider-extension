package com.cloudiam.keycloak.airship;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmProvider;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import org.apache.http.impl.client.CloseableHttpClient;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AirshipEmailProviderTests {
    private static final Network SHARED_NETWORK = Network.newNetwork();
    private static final String TEST_API_URL = "/api/email";
    private static final String TEST_ACCESS_TOKEN = "test-token";
    private static final String TEST_APP_KEY = "test-app-key";
    private static final String TEST_DEFAULT_SENDER = "noreply@example.com";
    private static final String TEST_AIRSHIP_HEADER = "vnd.urbanairship+json";
    private AirshipEmailProvider provider;
    
    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private RealmProvider realmProvider;

    @Mock
    private KeycloakContext keycloakContext;
    
    @Mock
    private CloseableHttpClient httpClient;

    static Keycloak keycloakAdminClient;

    @Container
    private final static WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin"));

    @Container
    private final static KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:latest")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("keycloak")
            .withExposedPorts(8080)
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withEnv("KC_HTTP_ENABLED", "true")
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_PROXY", "edge")
            .withEnv("KC_HTTP_PORT", "8080")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
            .waitingFor(Wait.forHttp("/realms/master"));
    
    @AfterAll
    static void cleanup() {
            if (keycloakAdminClient != null) {
                keycloakAdminClient.close();
            }

            if (container != null && container.isRunning()) {
                container.stop();
            }

            if (wiremockServer != null && wiremockServer.isRunning()) {
                wiremockServer.stop();
            }
    }

    @BeforeAll
    static void startContainer() throws Exception {
        wiremockServer.start();
        WireMock.configureFor(wiremockServer.getHost(), wiremockServer.getPort());

        container.start();
        
        String authServerUrl = container.getAuthServerUrl();
        RestAssured.baseURI = authServerUrl;
        RestAssured.useRelaxedHTTPSValidation();
        
        initializeKeycloakAdminClient(authServerUrl);
    }

    @BeforeEach
    void setUp() {
        WireMock.reset();

        when(keycloakSession.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        when(httpClientProvider.getHttpClient()).thenReturn(httpClient);


        when(keycloakSession.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
        when(keycloakSession.getProvider(RealmProvider.class)).thenReturn(realmProvider);
        when(keycloakSession.getContext()).thenReturn(keycloakContext);

        provider = new AirshipEmailProvider(
            keycloakSession,
            TEST_API_URL,
            wiremockServer.getBaseUrl(),
            TEST_ACCESS_TOKEN,
            TEST_APP_KEY,
            TEST_DEFAULT_SENDER,
            TEST_AIRSHIP_HEADER
        );
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(keycloakSession, httpClientProvider, realmProvider, keycloakContext, httpClient);
    }

    @Test
    void sendEmailSuccessfully() throws Exception {
                String recipient = "user@example.com";
                String subject = "Test Subject";
                String textBody = "Test email body";
                String htmlBody = "<p>Test email body</p>";
                
                Map<String, String> config = new HashMap<>();
                config.put("from", TEST_DEFAULT_SENDER);
                config.put("senderName", "Test Sender");
                config.put("replyTo", TEST_DEFAULT_SENDER);
                
                SimpleHttp.Response mockResponse = mock(SimpleHttp.Response.class);
                when(mockResponse.getStatus()).thenReturn(200);
                
                SimpleHttp simpleHttp = mock(SimpleHttp.class);
                when(simpleHttp.header(anyString(), anyString())).thenReturn(simpleHttp);
                when(simpleHttp.json(any())).thenReturn(simpleHttp);
                when(simpleHttp.asResponse()).thenReturn(mockResponse);

                try (MockedStatic<SimpleHttp> mockedStatic = Mockito.mockStatic(SimpleHttp.class)) {
                        mockedStatic.when(() -> SimpleHttp.doPost(anyString(), eq(keycloakSession)))
                        .thenReturn(simpleHttp);

                        provider.send(config, recipient, subject, textBody, htmlBody);
                        
                        mockedStatic.verify(() -> SimpleHttp.doPost(
                        wiremockServer.getBaseUrl() + TEST_API_URL,
                        keycloakSession
                        ));
                        
                        verify(simpleHttp).header("Content-Type", "application/json");
                        verify(simpleHttp).header("Accept", "application/" + TEST_AIRSHIP_HEADER + "; version=3");
                        verify(simpleHttp).header("Authorization", "Bearer " + TEST_ACCESS_TOKEN);
                        verify(simpleHttp).header("X-UA-App-Key", TEST_APP_KEY);
                        
                        ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
                        verify(simpleHttp).json(jsonCaptor.capture());

                        // Verify the JSON payload
                        ObjectNode payload = (ObjectNode) jsonCaptor.getValue();
                        assertEquals("user@example.com", payload.path("audience").path("create_and_send").get(0).path("ua_address").asText());
                        assertEquals("email", payload.path("device_types").get(0).asText());
                        assertEquals("Test Subject", payload.path("notification").path("email").path("subject").asText());
                        assertEquals("<p>Test email body</p>", payload.path("notification").path("email").path("html_body").asText());
                        assertEquals("Test Sender", payload.path("notification").path("email").path("sender_name").asText());
                        assertEquals(TEST_DEFAULT_SENDER, payload.path("notification").path("email").path("sender_address").asText());
                        assertEquals(TEST_DEFAULT_SENDER, payload.path("notification").path("email").path("reply_to").asText());
                        assertEquals("keycloak", payload.path("campaigns").path("categories").get(0).asText());
                        
                        verify(mockResponse).getStatus();
                }
        }


    private static void initializeKeycloakAdminClient(String authServerUrl) throws Exception {
        int maxRetries = 5;
        int retryDelaySeconds = 5;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try (Keycloak tempClient = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(KeycloakContainer.MASTER_REALM)
                    .clientId(KeycloakContainer.ADMIN_CLI_CLIENT)
                    .username(container.getAdminUsername())
                    .password(container.getAdminPassword())
                    .build()) {

                // Test the connection
                tempClient.realms().findAll();
                keycloakAdminClient = tempClient;
                return;
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    System.err.printf("Failed to connect to Keycloak (attempt %d/%d). Retrying in %d seconds...%n",
                            i + 1, maxRetries, retryDelaySeconds);
                    Thread.sleep(retryDelaySeconds * 1000);
                }
            }
        }

        throw new RuntimeException("Failed to initialize Keycloak admin client after " + maxRetries + " attempts", lastException);
    }
}