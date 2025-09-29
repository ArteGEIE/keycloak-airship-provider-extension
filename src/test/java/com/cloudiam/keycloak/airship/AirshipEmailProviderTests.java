package com.cloudiam.keycloak.airship;

import com.github.tomakehurst.wiremock.client.WireMock;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.isNotNull;
import static org.slf4j.LoggerFactory.getLogger;

@Testcontainers
class AirshipEmailProviderTests {
    private static final Logger LOGGER = getLogger(AirshipEmailProviderTests.class);

    private static final Network SHARED_NETWORK = Network.newNetwork();
    private static final String TEST_API_URL = "/api/email";
    private static final String TEST_ACCESS_TOKEN = "test-token";
    private static final String TEST_APP_KEY = "test-app-key";
    private static final String TEST_DEFAULT_SENDER = "noreply@example.com";
    private static final String TEST_DEFAULT_SENDER_NAME = "John Doe";
    private static final String TEST_DEFAULT_REPLY_TO = "John Doe <noreply@example.com>";
    private static final String TEST_AIRSHIP_HEADER = "vnd.urbanairship+json";
    public static final String USER_EMAIL = "user@example.com";

    static Keycloak keycloakAdminClient;

    @BeforeEach
    void setUp() {
        var wiremockServer = createWiremockContainer();
        wiremockServer.start();
        var keycloak = createKeycloakContainer(wiremockServer);
        keycloak.start();

        WireMock.configureFor(wiremockServer.getHost(), wiremockServer.getPort());
        keycloakAdminClient = keycloak.getKeycloakAdminClient();
    }

    @Test
    void sendEmailSuccessfully() {
        stubFor(post(TEST_API_URL).willReturn(ok()));

        String userId = createUser();
        keycloakAdminClient.realm(KeycloakContainer.MASTER_REALM).users().get(userId).sendVerifyEmail();

        verify(postRequestedFor(urlEqualTo(TEST_API_URL))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/" + TEST_AIRSHIP_HEADER + "; version=3"))
                .withHeader("Authorization", equalTo("Bearer " + TEST_ACCESS_TOKEN))
                .withHeader("X-UA-App-Key", equalTo(TEST_APP_KEY))
                .withRequestBody(matchingJsonPath("$.audience.create_and_send[0].ua_address", equalTo(USER_EMAIL)))
                .withRequestBody(matchingJsonPath("$.device_types[0]", equalTo("email")))
                .withRequestBody(matchingJsonPath("$.notification.email.subject", equalTo("Verify email")))
                .withRequestBody(matchingJsonPath("$.notification.email.html_body", isNotNull()))
                .withRequestBody(matchingJsonPath("$.notification.email.plaintext_body", isNotNull()))
                .withRequestBody(matchingJsonPath("$.notification.email.sender_name", equalTo(TEST_DEFAULT_SENDER_NAME)))
                .withRequestBody(matchingJsonPath("$.notification.email.sender_address", equalTo(TEST_DEFAULT_SENDER)))
                .withRequestBody(matchingJsonPath("$.notification.email.reply_to", equalTo(TEST_DEFAULT_REPLY_TO)))
                .withRequestBody(matchingJsonPath("$.campaigns.categories[0]", equalTo("keycloak")))
        );

    }

    String createUser() {
        var user = new UserRepresentation();
        user.setEmail(USER_EMAIL);
        user.setUsername("user");
        user.setEnabled(true);
        var response = keycloakAdminClient.realm(KeycloakContainer.MASTER_REALM).users().create(user);
        String userId = CreatedResponseUtil.getCreatedId(response);
        LOGGER.info("user created wit id {}", userId);
        return userId;
    }

    WireMockContainer createWiremockContainer() {
        var container = new WireMockContainer("wiremock/wiremock")
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases("wiremock")
                .waitingFor(Wait.forHttp("/__admin"));
        LOGGER.info("Wiremock container created");
        return container;
    }

    KeycloakContainer createKeycloakContainer(WireMockContainer wiremockServer) {
        var container = new KeycloakContainer("quay.io/keycloak/keycloak:26.3.4")
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases("keycloak")
                .withEnv("KC_LOG_LEVEL", "INFO,com.cloudiam:DEBUG,org.apache.http.wire:DEBUG")
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                .withEnv("AIRSHIP_ENDPOINT", TEST_API_URL)
                .withEnv("AIRSHIP_ACCESS_TOKEN", TEST_ACCESS_TOKEN)
                .withEnv("AIRSHIP_APP_KEY", TEST_APP_KEY)
                .withEnv("AIRSHIP_EMAIL_SENDER", TEST_DEFAULT_SENDER)
                .withEnv("AIRSHIP_DOMAIN", "http://wiremock:8080")
                .withEnv("AIRSHIP_EMAIL_SENDER", TEST_DEFAULT_SENDER)
                .withEnv("AIRSHIP_EMAIL_SENDER_NAME", TEST_DEFAULT_SENDER_NAME)
                .withEnv("AIRSHIP_EMAIL_REPLY_TO", TEST_DEFAULT_REPLY_TO)
                .withDefaultProviderClasses();
        LOGGER.info("Keycloak container created");
        return container;
    }

}
