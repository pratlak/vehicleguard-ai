package com.vehicleguard.rates.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final String DB_URL = System.getenv("DB_URL");

    static final PostgreSQLContainer<?> postgres;

    static {
        if (DB_URL == null) {
            postgres = new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("vehicleguard_test")
                    .withUsername("vehicleguard")
                    .withPassword("vehicleguard123");
            postgres.start();
        } else {
            postgres = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (DB_URL != null) {
            registry.add("spring.datasource.url", () -> DB_URL);
            registry.add("spring.datasource.username",
                    () -> System.getenv().getOrDefault("DB_USERNAME", "vehicleguard"));
            registry.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("DB_PASSWORD", "vehicleguard123"));
        } else {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
