package io.github.venomenon328.miseendice.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Stable datasource contract shared by compatible current-schema Spring integration tests. */
public abstract class CurrentSchemaPostgresIntegrationTest {

    @DynamicPropertySource
    static void sharedPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgreSqlTestServer::currentSchemaJdbcUrl);
        registry.add("spring.datasource.username", PostgreSqlTestServer::username);
        registry.add("spring.datasource.password", PostgreSqlTestServer::password);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 4);
    }
}
