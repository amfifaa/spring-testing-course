package com.example.crashcourse;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Tests don't use compose.yaml. Instead Testcontainers starts a fresh Postgres in Docker,
// and @ServiceConnection points the datasource at it automatically.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // STATIC: one container for the whole test run, shared by every Spring context that imports this
    // class (the integration tests AND the @DataJdbcTest slice). Without "static", each context
    // would start its own Postgres container: that's several seconds per context.
    // The price: tests share one database, so each test must clean up or use its own data.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
