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
class CatalogBaselineDiagnosticTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_catalog_diagnostic")
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
    void printCatalogConsolidationReport() {
        print("ACTIVE_ROOTS", """
                select ic.code,
                       ic.display_name,
                       ic.challenge_specificity,
                       ic.random_draw_enabled,
                       ic.base_draw_weight,
                       ic.novelty_level,
                       string_agg(fr.code, ',' order by fr.code) as roles
                from ingredient_concept ic
                left join ingredient_refinement parent_edge on parent_edge.child_concept_id = ic.id
                left join ingredient_functional_role ifr on ifr.ingredient_concept_id = ic.id
                left join functional_role fr on fr.id = ifr.functional_role_id
                where ic.active and parent_edge.child_concept_id is null
                group by ic.id
                order by ic.challenge_specificity, ic.display_name
                """);

        print("REDUNDANT_DIRECT_EDGES", """
                with recursive paths(root_id, current_id, depth) as (
                    select ir.parent_concept_id, ir.child_concept_id, 1
                    from ingredient_refinement ir
                    union all
                    select paths.root_id, ir.child_concept_id, paths.depth + 1
                    from paths
                    join ingredient_refinement ir on ir.parent_concept_id = paths.current_id
                    where paths.depth < 32
                )
                select parent.code as parent_code,
                       parent.display_name as parent_name,
                       child.code as child_code,
                       child.display_name as child_name
                from ingredient_refinement direct
                join ingredient_concept parent on parent.id = direct.parent_concept_id
                join ingredient_concept child on child.id = direct.child_concept_id
                where exists (
                    select 1
                    from paths
                    where paths.root_id = direct.parent_concept_id
                      and paths.current_id = direct.child_concept_id
                      and paths.depth >= 2
                )
                order by parent.display_name, child.display_name
                """);

        print("ROLE_DISJOINT_EDGES", """
                select parent.code as parent_code,
                       parent.display_name as parent_name,
                       child.code as child_code,
                       child.display_name as child_name,
                       string_agg(distinct parent_role.code, ',' order by parent_role.code) as parent_roles,
                       string_agg(distinct child_role.code, ',' order by child_role.code) as child_roles
                from ingredient_refinement ir
                join ingredient_concept parent on parent.id = ir.parent_concept_id
                join ingredient_concept child on child.id = ir.child_concept_id
                left join ingredient_functional_role parent_ifr on parent_ifr.ingredient_concept_id = parent.id
                left join functional_role parent_role on parent_role.id = parent_ifr.functional_role_id
                left join ingredient_functional_role child_ifr on child_ifr.ingredient_concept_id = child.id
                left join functional_role child_role on child_role.id = child_ifr.functional_role_id
                group by parent.id, child.id
                having count(*) filter (where parent_ifr.functional_role_id = child_ifr.functional_role_id) = 0
                order by parent.display_name, child.display_name
                """);

        print("HIGH_WEIGHT_SPECIALTIES", """
                select ic.code,
                       ic.display_name,
                       ic.base_draw_weight,
                       ic.novelty_level,
                       string_agg(fr.code, ',' order by fr.code) as roles,
                       string_agg(p.code || ':' || ia.availability_level, ',' order by p.code) as availability
                from ingredient_concept ic
                join ingredient_functional_role ifr on ifr.ingredient_concept_id = ic.id
                join functional_role fr on fr.id = ifr.functional_role_id
                join ingredient_availability ia on ia.ingredient_concept_id = ic.id
                join participant p on p.id = ia.participant_id and p.code in ('TOBIAS', 'GEORGIA')
                where ic.active
                  and ic.random_draw_enabled
                  and (
                      (ic.novelty_level >= 3 and ic.base_draw_weight >= 0.4500)
                      or (ic.base_draw_weight >= 0.5000 and exists (
                          select 1
                          from ingredient_availability difficult
                          where difficult.ingredient_concept_id = ic.id
                            and difficult.availability_level = 'DIFFICULT'
                      ))
                  )
                group by ic.id
                order by ic.base_draw_weight desc, ic.novelty_level desc, ic.display_name
                """);

        print("HIGH_WEIGHT_COOKING_ALCOHOL", """
                with recursive descendants(id) as (
                    select child.id
                    from ingredient_concept parent
                    join ingredient_refinement ir on ir.parent_concept_id = parent.id
                    join ingredient_concept child on child.id = ir.child_concept_id
                    where parent.code = 'COOKING_ALCOHOL'
                    union
                    select child.id
                    from descendants
                    join ingredient_refinement ir on ir.parent_concept_id = descendants.id
                    join ingredient_concept child on child.id = ir.child_concept_id
                )
                select ic.code, ic.display_name, ic.base_draw_weight, ic.novelty_level
                from descendants
                join ingredient_concept ic on ic.id = descendants.id
                order by ic.base_draw_weight desc, ic.display_name
                """);

        print("MULTI_PARENT_CONCEPTS", """
                select child.code,
                       child.display_name,
                       count(*) as parent_count,
                       string_agg(parent.code, ',' order by parent.code) as parents
                from ingredient_refinement ir
                join ingredient_concept parent on parent.id = ir.parent_concept_id
                join ingredient_concept child on child.id = ir.child_concept_id
                group by child.id
                having count(*) > 1
                order by count(*) desc, child.display_name
                """);
    }

    private void print(String section, String sql) {
        System.out.println("CATALOG_AUDIT_BEGIN " + section);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        rows.forEach(row -> System.out.println("CATALOG_AUDIT_ROW " + section + " " + row));
        System.out.println("CATALOG_AUDIT_END " + section + " count=" + rows.size());
    }
}
