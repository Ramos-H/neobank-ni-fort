package com.neobank.discovery_server;

import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DiscoveryServerApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private EurekaServerContext eurekaServerContext;

    @Autowired(required = false)
    private PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${eureka.client.register-with-eureka}")
    private boolean registerWithEureka;

    @Value("${eureka.client.fetch-registry}")
    private boolean fetchRegistry;

    @Test
    @DisplayName("Context loads successfully and Spring application context is populated")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Verify Eureka server core beans are initialized")
    void verifyEurekaServerBeansAreInitialized() {
        assertThat(eurekaServerContext)
                .as("EurekaServerContext should be initialized in the Spring context")
                .isNotNull();

        assertThat(peerAwareInstanceRegistry)
                .as("PeerAwareInstanceRegistry should be initialized in the Spring context")
                .isNotNull();
    }

    @Test
    @DisplayName("Verify Eureka standalone server configuration properties")
    void verifyEurekaServerConfiguration() {
        assertThat(applicationName).isEqualTo("discovery-server");
        assertThat(registerWithEureka).isFalse();
        assertThat(fetchRegistry).isFalse();
    }
}

