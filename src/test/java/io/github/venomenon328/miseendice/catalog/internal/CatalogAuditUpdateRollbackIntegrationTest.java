package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Proves that an audit failure also rolls an existing aggregate update and version advance back. */
@SpringBootTest(classes = {MiseEnDiceApplication.class, CatalogAuditUpdateRollbackIntegrationTest.FailingAuditConfiguration.class})
@Testcontainers
class CatalogAuditUpdateRollbackIntegrationTest {

    private static final String CODE = "TEST_ISSUE11_AUDIT_UPDATE_FAILURE";

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

    @AfterEach
    void removeTestConcept() {
        jdbcTemplate.update("delete from ingredient_concept where code = ?", CODE);
    }

    @Test
    void rollsBackExistingConceptFieldsAndVersionWhenWritingAuditFails() {
        long conceptId = jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, 'Audit update original', true, false, 'SPECIFIC', 1.0000)
                returning id
                """, Long.class, CODE);

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                conceptId,
                0,
                "Audit update changed",
                true,
                false,
                "SPECIFIC",
                new BigDecimal("0.7500"),
                2,
                "must roll back",
                "issue11-audit-update-admin",
                false
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForMap("""
                select display_name, version, base_draw_weight, novelty_level, curator_note
                from ingredient_concept
                where id = ?
                """, conceptId))
                .containsEntry("display_name", "Audit update original")
                .containsEntry("version", 0L)
                .containsEntry("base_draw_weight", new BigDecimal("1.0000"))
                .containsEntry("novelty_level", null)
                .containsEntry("curator_note", null);
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
