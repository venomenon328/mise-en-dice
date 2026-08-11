package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Proves that an audit persistence failure rolls the catalog mutation back with it. */
@SpringBootTest(classes = {MiseEnDiceApplication.class, CatalogAuditTransactionIntegrationTest.FailingAuditConfiguration.class})
@Testcontainers
class CatalogAuditTransactionIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CatalogCommands catalogCommands;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackTheNewConceptWhenWritingItsAuditEntryFails() {
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                "TEST_ISSUE11_AUDIT_FAILURE", "Issue eleven audit failure", "issue11-audit-admin"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept where code = 'TEST_ISSUE11_AUDIT_FAILURE'", Integer.class
        )).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingAuditConfiguration {

        @Bean
        @Primary
        CatalogAuditLog failingCatalogAuditLog() {
            return new CatalogAuditLog() {
                @Override
                public CatalogAuditEntry append(CatalogAuditEntryDraft entry) {
                    throw new IllegalStateException("deliberate audit failure");
                }

                @Override
                public Optional<CatalogAuditEntry> findById(long id) {
                    return Optional.empty();
                }
            };
        }
    }
}
