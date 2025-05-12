package com.cloudiam.keycloak.airship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirshipEmailProviderMockTest {

    @Mock
    private KeycloakSession mockSession;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private AirshipEmailProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        String TEST_API_URL = "https://test-airship-domain.com/api/test"; // Add scheme
        String TEST_AIRSHIP_DOMAIN = "https://test-airship-domain.com";  // Add scheme
        String TEST_APP_KEY = "test-app-key";
        String TEST_AIRSHIP_HEADER = "vnd.urbanairship+json";
        String TEST_DEFAULT_SENDER = "test@sender.com";
        String TEST_ACCESS_TOKEN = "test-access-token";
        provider = new AirshipEmailProvider(
                mockSession,
                TEST_API_URL,
                TEST_AIRSHIP_DOMAIN,
                TEST_ACCESS_TOKEN,
                TEST_APP_KEY,
                TEST_DEFAULT_SENDER,
                TEST_AIRSHIP_HEADER
        );

        Field httpClientField = AirshipEmailProvider.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(provider, mockHttpClient);

        when(mockSession.getProvider(HttpClientProvider.class)).thenReturn(provider);

        lenient().when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
        lenient().when(mockResponse.statusCode()).thenReturn(200);
        lenient().when(mockResponse.body()).thenReturn("{\"ok\": true}");
    }

    @Test
    void shouldSendEmailSuccessfully() throws Exception {
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String textBody = "Test text body";
        String htmlBody = "<p>Test HTML body</p>";
        Map<String, String> config = new HashMap<>();

        provider.send(config, recipient, subject, textBody, htmlBody);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldUseCustomSenderWhenProvided() throws Exception {
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String textBody = "Test text body";
        String htmlBody = "<p>Test HTML body</p>";
        String customSender = "custom@sender.com";
        Map<String, String> config = new HashMap<>();
        config.put("from", customSender);

        provider.send(config, recipient, subject, textBody, htmlBody);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldThrowExceptionOnApiError() throws Exception {
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String textBody = "Test text body";
        String htmlBody = "<p>Test HTML body</p>";
        Map<String, String> config = new HashMap<>();

        lenient().when(mockResponse.statusCode()).thenReturn(400); // Mark as lenient
        lenient().when(mockResponse.body()).thenReturn("{\"error\": \"Bad Request\"}"); // Mark as lenient

        EmailException exception = assertThrows(EmailException.class,
                () -> provider.send(config, recipient, subject, textBody, htmlBody));

        String expectedErrorPrefix = "Failed to send email via Airship";
        assertTrue(exception.getMessage().startsWith(expectedErrorPrefix),
                "Exception message should start with '" + expectedErrorPrefix + "', but was: " + exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnHttpClientError() throws Exception {
        String recipient = "recipient@example.com";
        String subject = "Test Subject";
        String textBody = "Test text body";
        String htmlBody = "<p>Test HTML body</p>";
        Map<String, String> config = new HashMap<>();

        String errorMessage = "Connection refused";
        IOException ioException = new IOException(errorMessage);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(ioException);

        // When/Then
        EmailException exception = assertThrows(EmailException.class,
                () -> provider.send(config, recipient, subject, textBody, htmlBody));

        // Verify exception details
        assertEquals("Failed to send email via Airship", exception.getMessage(),
                "Exception message should match expected text");
        assertSame(ioException, exception.getCause(),
                "Exception cause should be our IOException instance");
    }

    @Test
    void shouldDiscoverProviderThroughServiceLoader() throws Exception {
        ServiceLoader<EmailSenderProviderFactory> loader = ServiceLoader.load(EmailSenderProviderFactory.class);

        boolean found = false;
        for (EmailSenderProviderFactory factory : loader) {
            if (factory instanceof AirshipEmailFactory) {
                found = true;
                break;
            }
        }

        assertTrue(found, "AirshipEmailFactory should be discovered by ServiceLoader");
    }
}
