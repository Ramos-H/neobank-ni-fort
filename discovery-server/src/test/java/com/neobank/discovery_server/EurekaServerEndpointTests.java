package com.neobank.discovery_server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerEndpointTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Positive Test Cases - Eureka Server Endpoints")
    class PositiveEndpointTests {

        @Test
        @DisplayName("GET / should render the Eureka Server dashboard UI")
        void testEurekaDashboardIsAccessible() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .isNotNull()
                    .contains("Eureka");
        }

        @Test
        @DisplayName("GET /eureka/apps with XML accept header returns valid application registry")
        void testEurekaAppsEndpointXml() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps"))
                    .header("Accept", "application/xml")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .isNotNull()
                    .contains("applications");
        }

        @Test
        @DisplayName("GET /eureka/apps with JSON accept header returns valid application registry")
        void testEurekaAppsEndpointJson() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .isNotNull()
                    .contains("applications");
        }
    }

    @Nested
    @DisplayName("Negative Test Cases - Invalid / Non-Existent Resources")
    class NegativeEndpointTests {

        @Test
        @DisplayName("GET /eureka/apps/{appName} for non-existent service returns 404 NOT_FOUND")
        void testNonExistentApplicationReturnsNotFound() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps/NON_EXISTENT_SERVICE"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("GET /eureka/apps/{appName}/{instanceId} for non-existent instance returns 404 NOT_FOUND")
        void testNonExistentInstanceReturnsNotFound() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps/NON_EXISTENT_SERVICE/invalid-instance-id-12345"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("DELETE /eureka/apps/{appName}/{instanceId} for non-existent instance returns 404 NOT_FOUND")
        void testDeleteNonExistentInstanceReturnsNotFound() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps/NON_EXISTENT_SERVICE/invalid-instance-id-12345"))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("POST /eureka/apps/{appName} with invalid payload returns 4xx Client Error")
        void testInvalidRegistrationPayloadReturnsError() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps/TEST-SERVICE"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"invalid\": \"payload\"}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        }
    }
}
