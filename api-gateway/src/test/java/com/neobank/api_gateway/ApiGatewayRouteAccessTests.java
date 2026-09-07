package com.neobank.api_gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRouteAccessTests {

    @LocalServerPort
    private int port;

    @Autowired
    private RouteLocator routeLocator;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Route Configuration Verification")
    class RouteConfigurationTests {

        @Test
        @DisplayName("Verify defined microservice routes exist in RouteLocator")
        void verifyDefinedRoutesExist() {
            Flux<Route> routesFlux = routeLocator.getRoutes();

            StepVerifier.create(routesFlux.collectList())
                    .assertNext(routes -> {
                        List<String> routeIds = routes.stream()
                                .map(Route::getId)
                                .toList();

                        assertThat(routeIds)
                                .contains("auth-service", "account-service", "transaction-service");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Verify Eureka server is NOT exposed as a route in RouteLocator")
        void verifyEurekaServerIsNotExposedAsRoute() {
            Flux<Route> routesFlux = routeLocator.getRoutes();

            StepVerifier.create(routesFlux.collectList())
                    .assertNext(routes -> {
                        List<String> routeIds = routes.stream()
                                .map(Route::getId)
                                .toList();

                        assertThat(routeIds)
                                .as("Eureka server / discovery endpoints should not be registered as routes in API Gateway")
                                .doesNotContain("discovery-server", "eureka-server", "eureka");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Negative Test Cases - Gateway Access Control")
    class NegativeAccessControlTests {

        @Test
        @DisplayName("Negative Test: Client accessing /eureka/apps through Gateway is rejected with 404 NOT_FOUND")
        void testEurekaAppsEndpointNotAccessibleThroughGateway() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka/apps"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .as("External client attempting to access Eureka discovery through the Gateway should get 404")
                    .isEqualTo(404);
        }

        @Test
        @DisplayName("Negative Test: Client accessing root /eureka through Gateway is rejected with 404 NOT_FOUND")
        void testEurekaRootNotAccessibleThroughGateway() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/eureka"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .as("External client attempting to access Eureka root through the Gateway should get 404")
                    .isEqualTo(404);
        }

        @Test
        @DisplayName("Negative Test: Client accessing unmapped arbitrary endpoints is rejected with 404 NOT_FOUND")
        void testUnmappedPathReturnsNotFound() throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/api/unmapped/unknown-service"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .as("Unmapped API path must return 404")
                    .isEqualTo(404);
        }
    }
}
