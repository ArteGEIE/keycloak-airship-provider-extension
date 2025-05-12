package com.cloudiam.keycloak.airship;

import com.github.tomakehurst.wiremock.client.WireMock;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AirshipEmailProviderTests {
    private static final Network SHARED_NETWORK = Network.newNetwork();
    private static String adminAccessToken;
    private static String baseUrl;

    @Container
    static WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("api");

    static KeycloakContainer container;

    static Keycloak keycloakAdminClient;

    @BeforeAll
    static void startContainer() {
        wiremockServer.start();
        baseUrl = container.getAuthServerUrl() + "/admin/realms/test-realm";
        container = new KeycloakContainer("quay.io/keycloak/keycloak:25.0.6")
                .withAdminUsername("admin")
                .withExposedPorts(8080, 8443)
                .withAdminPassword("password")
                .withDefaultProviderClasses()
                .withNetwork(SHARED_NETWORK)
                .withEnv("API_URL", "http://api:8080")
                .withEnv("KC_HTTP_ENABLED", "true")
                .withEnv("KC_HOSTNAME_STRICT", "false")
                .withEnv("KC_LOG_LEVEL", "DEBUG")
                .withCustomCommand("start-dev")
                .withEnv("KC_DB", "dev-mem")
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "password")
                .waitingFor(
                        Wait.forHttp("/health")
                                .forPort(8080)
                                .withStartupTimeout(Duration.ofMinutes(3))
                )
                .withLogConsumer(outputFrame -> {
                    System.out.println("Keycloak Log: " +
                            outputFrame.getUtf8String().trim());
                });
        container.start();

        RestAssured.baseURI = container.getAuthServerUrl();
        WireMock.configureFor(wiremockServer.getHost(), wiremockServer.getPort());

        keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(container.getAuthServerUrl())
                .realm(KeycloakContainer.MASTER_REALM)
                .clientId(KeycloakContainer.ADMIN_CLI_CLIENT)
                .username(container.getAdminUsername())
                .password(container.getAdminPassword())
                .build();

        // Initialize admin token
        adminAccessToken = obtainAdminAccessToken();

        try {
            keycloakAdminClient.realm("test-realm").toRepresentation();
        } catch (Exception e) {
            var realm = new RealmRepresentation();
            realm.setRealm("test-realm");
            realm.setEnabled(true);
            keycloakAdminClient.realms().create(realm);
        }
    }

    private static String obtainAdminAccessToken() {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "password");
        formParams.put("client_id", "admin-cli");
        formParams.put("username", "admin");
        formParams.put("password", "admin");

        return given()
                .contentType(ContentType.URLENC)
                .formParams(formParams)
                .when()
                .post(container.getAuthServerUrl() + "/realms/master/protocol/openid-connect/token")
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");
    }


    @AfterAll
    static void stopContainer() {
        container.stop();
        wiremockServer.stop();
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        final var realm = keycloakAdminClient.realm("test-realm");
        realm.remove();
    }

    @Test
    void testEmailVerificationEndpoint() {
        WireMock wireMock = new WireMock(
                wiremockServer.getHost(),
                wiremockServer.getMappedPort(8080)
        );

        wireMock.register(WireMock.post(WireMock.urlPathEqualTo("/airship/send-email"))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withHeader("Authorization", WireMock.containing("Bearer test-token"))
                .withHeader("Accept", WireMock.equalTo("vnd.urbanairship+json"))
                .withHeader("From", WireMock.equalTo("noreply@example.com"))
                .withRequestBody(WireMock.matchingJsonPath("$.audience.ua_address", WireMock.equalTo("testuser@example.com")))
                .withRequestBody(WireMock.matchingJsonPath("$.create_and_send[0].template_id", WireMock.equalTo("keycloak-verification-email")))
                .withRequestBody(WireMock.matchingJsonPath("$.create_and_send[0].from", WireMock.equalTo("noreply@example.com")))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"success\", \"messageId\": \"test-message-id\"}")));


        String userId = createTestUser();

        given()
                .auth().oauth2(adminAccessToken)
                .contentType(ContentType.JSON)
                .when()
                .put(baseUrl + "/users/" + userId + "/send-verify-email")
                .then()
                .statusCode(204);
    
        // Verify the request was made with the expected parameters
        wireMock.verifyThat(1, WireMock.postRequestedFor(WireMock.urlPathEqualTo("/airship/send-email"))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withHeader("Authorization", WireMock.containing("Bearer test-token"))
                .withHeader("Accept", WireMock.equalTo("vnd.urbanairship+json"))
                .withHeader("From", WireMock.equalTo("noreply@example.com"))
                .withRequestBody(WireMock.matchingJsonPath("$.audience.ua_address", WireMock.equalTo("testuser@example.com")))
                .withRequestBody(WireMock.matchingJsonPath("$.create_and_send[0].template_id", WireMock.equalTo("keycloak-verification-email")))
                .withRequestBody(WireMock.matchingJsonPath("$.create_and_send[0].from", WireMock.equalTo("noreply@example.com"))));
        }

    @Test
    void testEmailVerificationFailure() {
        WireMock wireMock = new WireMock(
                wiremockServer.getHost(),
                wiremockServer.getMappedPort(8080)
        );

        wireMock.register(WireMock.post(WireMock.urlPathEqualTo("/airship/send-email"))
                .willReturn(WireMock.aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Unauthorized\"}")));

        String userId = createTestUser();
        
        given()
                .auth().oauth2(adminAccessToken)
                .contentType(ContentType.JSON)
                .when()
                .put(baseUrl + "/users/" + userId + "/send-verify-email")
                .then()
                .statusCode(500); // Or whatever status code your implementation returns
        }

        @Test
        void testEmailVerificationUserNotFound() {
        given()
                .auth().oauth2(adminAccessToken)
                .contentType(ContentType.JSON)
                .when()
                .put(baseUrl + "/users/non-existent-user/send-verify-email")
                .then()
                .statusCode(404);
        }

        @Test
        void testEmailVerificationMalformedRequest() {
        given()
                .auth().oauth2(adminAccessToken)
                .contentType(ContentType.JSON)
                .when()
                .put(baseUrl + "/users//send-verify-email")
                .then()
                .statusCode(404);
        }

    /**
     * Create a test user in Keycloak
     * @return Created user ID
     */
    private String createTestUser() {
        // Prepare user creation payload
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", "testuser");
        userPayload.put("email", "testuser@example.com");
        userPayload.put("firstName", "Test");
        userPayload.put("lastName", "User");
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", false);

        // Create user and extract user ID
        return given()
                .auth().oauth2(adminAccessToken)
                .contentType(ContentType.JSON)
                .body(userPayload)
                .when()
                .post(baseUrl + "/users")
                .then()
                .statusCode(201)
                .extract()
                .header("Location")
                .split("/users/")[1];
    }

    @Test
    void testContainersAreRunning() {
        assertThat(container.isRunning()).isTrue();
        assertThat(wiremockServer.isRunning()).isTrue();
    }
}