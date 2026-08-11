package io.github.venomenon328.miseendice;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CatalogWeightDiagnosticTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_weight_diagnostic")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void printWeightAudit() {
        print("NOVELTY_4_5", """
                select code, display_name, base_draw_weight, novelty_level
                from ingredient_concept
                where active and random_draw_enabled and novelty_level >= 4
                order by base_draw_weight desc, novelty_level desc, display_name
                """);

        print("DIFFICULT_ABOVE_035", """
                select ic.code,
                       ic.display_name,
                       ic.base_draw_weight,
                       ic.novelty_level,
                       string_agg(p.code || ':' || ia.availability_level, ',' order by p.code) as availability
                from ingredient_concept ic
                join ingredient_availability ia on ia.ingredient_concept_id = ic.id
                join participant p on p.id = ia.participant_id and p.code in ('TOBIAS', 'GEORGIA')
                where ic.active and ic.random_draw_enabled
                group by ic.id
                having bool_or(ia.availability_level = 'DIFFICULT')
                   and ic.base_draw_weight > 0.3500
                order by ic.base_draw_weight desc, ic.display_name
                """);

        print("OPEN_ABOVE_060", """
                select code, display_name, base_draw_weight, novelty_level
                from ingredient_concept
                where active
                  and random_draw_enabled
                  and challenge_specificity = 'OPEN'
                  and base_draw_weight > 0.6000
                order by base_draw_weight desc, display_name
                """);

        print("SPECIFIC_SEASONING_ABOVE_060", """
                select distinct ic.code, ic.display_name, ic.base_draw_weight, ic.novelty_level
                from ingredient_concept ic
                join ingredient_functional_role ifr on ifr.ingredient_concept_id = ic.id
                join functional_role fr on fr.id = ifr.functional_role_id
                where ic.active
                  and ic.random_draw_enabled
                  and ic.challenge_specificity = 'SPECIFIC'
                  and fr.code = 'SEASONING'
                  and ic.base_draw_weight > 0.6000
                order by ic.base_draw_weight desc, ic.display_name
                """);
    }

    private void print(String section, String sql) {
        System.out.println("CATALOG_WEIGHT_AUDIT_BEGIN " + section);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        rows.forEach(row -> System.out.println("CATALOG_WEIGHT_AUDIT_ROW " + section + " " + row));
        System.out.println("CATALOG_WEIGHT_AUDIT_END " + section + " count=" + rows.size());
    }
}
