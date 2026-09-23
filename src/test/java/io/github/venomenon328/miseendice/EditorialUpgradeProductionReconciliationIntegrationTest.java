package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Tag("migration")
class EditorialUpgradeProductionReconciliationIntegrationTest {
    private static final Path DIRECTORY = Path.of("deploy/reconciliation/293");
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";

    @Test
    void incidentAndOperationalDriftReachMasterWithoutOverwritingUnrelatedFields() throws Exception {
        try (var db = PostgreSqlTestServer.createTemporaryDatabase("issue293_drift");
                var connection = db.openConnection()) {
            incident(connection);
            var jdbc = jdbc(connection);
            assertThat(state(connection)).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select display_name from ingredient_concept where code='EGG'", String.class))
                    .isEqualTo("Hühnerei");
            assertThat(jdbc.queryForList("""
                    select a.curator_note from ingredient_availability a
                    join ingredient_concept c on c.id=a.ingredient_concept_id where c.code='EGG'
                    """, String.class)).allMatch(note -> note.startsWith("Ei:"));
            // Demonstrate the actual old failure, then roll back its transaction.
            assertThatThrownBy(() -> liquibase(connection).update(1, new Contexts(), new LabelExpression()))
                    .hasStackTraceContaining("Availability note consolidation: unreviewed or missing values for EGG/");
            connection.rollback();
            jdbc.execute("""
                    update ingredient_concept set active=false, random_draw_enabled=false,
                        curator_note='Preserved operational concept note', novelty_level=5,
                        base_draw_weight=0.3333 where code='EGG';
                    update ingredient_concept set display_name='Operational chickpea name' where code='CHICKPEAS';
                    update ingredient_concept set display_name='Operational berry name' where code='BLUEBERRY';
                    update culinary_country set display_name='Operational country name' where code='JP';
                    update ingredient_concept set curator_note='Operational metadata' where code in ('HERRING','TEA','CRUSTACEANS','CRAB','SHRIMP');
                    update ingredient_concept set challenge_specificity='OPEN' where code='SHRIMP';
                    update ingredient_availability a set availability_level='UNAVAILABLE', curator_note='Operational availability'
                    from ingredient_concept c where c.id=a.ingredient_concept_id and c.code in ('ALIGUE','HERRING','CRAB','EGG');
                    delete from ingredient_availability where ingredient_concept_id=(select id from ingredient_concept where code='TEA');
                    insert into ingredient_concept (code,display_name,active,random_draw_enabled,challenge_specificity,curator_note)
                    values ('ISSUE_293_EXTRA','Issue 293 extra',false,false,'SPECIFIC','Preserved extra');
                    insert into participant (code,display_name) values ('ISSUE_293_OTHER','Issue 293 other');
                    insert into ingredient_availability (ingredient_concept_id,participant_id,availability_level,curator_note)
                    select c.id,p.id,'DIFFICULT','Preserved other participant' from ingredient_concept c cross join participant p
                    where c.code='EGG' and p.code='ISSUE_293_OTHER';
                    insert into ingredient_culinary_country select id,'FR' from ingredient_concept where code='EGG'
                    on conflict do nothing;
                    insert into ingredient_culinary_country select id,'FR' from ingredient_concept where code='CREAM'
                    on conflict do nothing;
                    delete from ingredient_refinement where parent_concept_id=(select id from ingredient_concept where code='POD_VEGETABLES')
                      and child_concept_id=(select id from ingredient_concept where code='GREEN_PEAS');
                    insert into ingredient_refinement select p.id,c.id from ingredient_concept p cross join ingredient_concept c
                    where p.code='GREEN_PEAS' and c.code='GREEN_SPLIT_PEAS' on conflict do nothing;
                    insert into ingredient_refinement select p.id,c.id from ingredient_concept p cross join ingredient_concept c
                    where p.code='CRAB' and c.code='ISSUE_293_EXTRA';
                    delete from ingredient_refinement where parent_concept_id=(select id from ingredient_concept where code='CRUSTACEANS')
                      and child_concept_id=(select id from ingredient_concept where code='LOBSTER');
                    """);
            String unsupported = jdbc.queryForObject("""
                    select letter from (values ('ß'),('ﬃ')) candidates(letter)
                    where letter ~ '^[[:lower:]]$' and (upper(letter)=letter or char_length(upper(letter))<>1)
                    limit 1
                    """, String.class);
            assertThat(unsupported).isNotBlank();
            jdbc.update("""
                    insert into ingredient_availability (ingredient_concept_id,participant_id,availability_level,curator_note)
                    select c.id,p.id,'EASY',? from ingredient_concept c cross join participant p
                    where c.code='ISSUE_293_EXTRA' and p.code='ISSUE_293_OTHER'
                    """, unsupported + " unchanged");
            connection.commit();
            reconcile(connection, true);
            assertThat(state(connection)).isEqualTo("APPLIED");
            assertThat(jdbc.queryForMap("""
                    select display_name,active,random_draw_enabled,curator_note,novelty_level
                    from ingredient_concept where code='EGG'
                    """)).containsEntry("display_name", "Hühnerei").containsEntry("active", false)
                    .containsEntry("random_draw_enabled", false).containsEntry("novelty_level", 5)
                    .containsEntry("curator_note", "Preserved operational concept note");
            assertThat(jdbc.queryForObject("select display_name from ingredient_concept where code='CHICKPEAS'", String.class))
                    .isEqualTo("Operational chickpea name");
            assertThat(jdbc.queryForObject("select challenge_specificity from ingredient_concept where code='SHRIMP'", String.class))
                    .isEqualTo("OPEN");
            assertThat(jdbc.queryForObject("""
                    select count(*) from ingredient_refinement r join ingredient_concept c on c.id=r.child_concept_id
                    where c.code='ISSUE_293_EXTRA'
                    """, Integer.class)).isOne();
            assertThat(jdbc.queryForObject("""
                    select a.curator_note from ingredient_availability a join participant p on p.id=a.participant_id
                    join ingredient_concept c on c.id=a.ingredient_concept_id
                    where p.code='ISSUE_293_OTHER' and c.code='EGG'
                    """, String.class)).isEqualTo("Preserved other participant");
            assertThat(jdbc.queryForObject("""
                    select a.curator_note from ingredient_availability a
                    join ingredient_concept c on c.id=a.ingredient_concept_id
                    where c.code='ISSUE_293_EXTRA'
                    """, String.class)).isEqualTo(unsupported + " unchanged");
            assertThat(jdbc.queryForObject("""
                    select count(*) from ingredient_culinary_country r join ingredient_concept c on c.id=r.ingredient_concept_id
                    where (c.code='EGG' and r.country_code='FR') or (c.code='CREAM' and r.country_code='FR')
                    """, Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("""
                    select count(*) from ingredient_concept_alias a join ingredient_concept c on c.id=a.ingredient_concept_id
                    where c.code='CHICKPEAS' and a.alias_text='Operational alias'
                    """, Integer.class)).isOne();
            // Approved values are read from the migration's own temporary target
            // tables before commit in reconcile(); no second catalog oracle.
            String after = snapshot(connection);
            assertThat(state(connection)).isEqualTo("APPLIED"); // operator no-op branch
            liquibase(connection).update(new Contexts(), new LabelExpression());
            liquibase(connection).update(new Contexts(), new LabelExpression());
            assertThat(snapshot(connection)).isEqualTo(after);
            assertThat(jdbc.queryForObject("select count(*) from databasechangelog where md5sum is null and filename like 'db/%'", Integer.class)).isZero();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-code", "name-collision", "graph-cycle", "missing-participant"})
    void structuralFailuresRollBackTheEntireCorridorIncludingSchemaAndHistory(String failure) throws Exception {
        try (var db = PostgreSqlTestServer.createTemporaryDatabase("issue293_invalid");
                var connection = db.openConnection()) {
            incident(connection);
            var jdbc = jdbc(connection);
            String mutation = switch (failure) {
                case "missing-code" -> "update ingredient_concept set code='ISSUE_293_MISSING' where code='EGG'";
                case "name-collision" -> "update ingredient_concept set display_name='Krabben' where code='EGG'";
                case "graph-cycle" -> """
                        insert into ingredient_refinement select p.id,c.id from ingredient_concept p cross join ingredient_concept c
                        where p.code='GREEN_SPLIT_PEAS' and c.code='GREEN_PEAS'
                        """;
                default -> """
                        alter table participant disable trigger trg_participant_code_immutable;
                        update participant set code='ISSUE_293_MISSING' where code='TOBIAS';
                        alter table participant enable trigger trg_participant_code_immutable
                        """;
            };
            jdbc.execute(mutation);
            connection.commit();
            String before = snapshot(connection);
            String expected = switch (failure) {
                case "missing-code" -> "missing 037 concept code";
                case "name-collision" -> "collide";
                case "graph-cycle" -> "cycle";
                default -> "missing required participants";
            };
            assertThatThrownBy(() -> reconcile(connection, false)).isInstanceOf(SQLException.class)
                    .hasMessageContaining(expected);
            connection.rollback();
            assertThat(snapshot(connection)).isEqualTo(before);
            assertThat(state(connection)).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select to_regclass('ingredient_concept_alias')::text", String.class)).isNull();
            assertThat(jdbc.queryForObject("select to_regclass('catalog_audit_entry')::text", String.class)).isNotNull();
        }
    }

    @Test
    void unknownPartialHistoryAndActiveLiquibaseLockFailClosed() throws Exception {
        try (var db = PostgreSqlTestServer.createTemporaryDatabase("issue293_history");
                var connection = db.openConnection()) {
            incident(connection);
            var jdbc = jdbc(connection);
            jdbc.update("update databasechangeloglock set locked=true");
            assertThat(state(connection)).isEqualTo("INVALID");
            jdbc.update("update databasechangeloglock set locked=false");
            jdbc.update("update databasechangelog set author='unknown' where id='035-england-curation'");
            assertThat(state(connection)).isEqualTo("INVALID");
            assertThatThrownBy(() -> reconcile(connection, false)).hasMessageContaining("exact post-036");
            connection.rollback();
        }
    }

    private static void incident(Connection connection) throws Exception {
        var lb = liquibase(connection);
        var unrun = lb.listUnrunChangeSets(new Contexts(), new LabelExpression());
        int before036 = 0;
        while (!unrun.get(before036).getId().equals("036-availability-note-prefix-cleanup")) before036++;
        lb.update(before036, new Contexts(), new LabelExpression());
        jdbc(connection).update("update ingredient_concept set display_name='Hühnerei' where code='EGG'");
        jdbc(connection).update("update databasechangelog set exectype='MARK_RAN' where id='033-availability-novelty-final-review'");
        connection.commit();
        liquibase(connection).update(1, new Contexts(), new LabelExpression());
    }

    private static void reconcile(Connection c, boolean drift) throws Exception {
        execute(c, read(DIRECTORY.resolve("history.sql")));
        execute(c, read(DIRECTORY.resolve("setup.sql")));
        for (String row : Files.readAllLines(DIRECTORY.resolve("parts.tsv"))) {
            String[] part = row.split("\t");
            String source = read(Path.of(part[0]));
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                    .as("versioned SQL source %s", part[0]).isEqualTo(part[3]);
            if (drift && part[0].endsWith("044-apply.sql")) {
                // Aliases become available at schema 023 inside this corridor.
                // Seed operational drift and one already installed target then.
                execute(c, """
                        insert into ingredient_concept_alias select id,'Operational alias' from ingredient_concept where code='CHICKPEAS';
                        insert into ingredient_concept_alias select c.id,a from ingredient_name_alias_r3_review r
                        join ingredient_concept c using(code) cross join lateral unnest(r.target_aliases) a
                        where r.code='ADZUKI_BEANS';
                        update ingredient_concept c set display_name='Operational rename target'
                        where c.code=(select min(code) from ingredient_name_alias_r3_review
                                      where previous_display_name<>target_display_name);
                        insert into ingredient_concept_alias
                        select c.id,r.target_display_name from ingredient_name_alias_r3_review r
                        join ingredient_concept c using(code)
                        where c.display_name='Operational rename target';
                        """);
            }
            if (drift && part[0].endsWith("045-apply.sql")) {
                execute(c, """
                        insert into ingredient_concept_alias select id,'Krabben' from ingredient_concept
                        where code='CRAB' and lower(display_name)<>lower('Krabben');
                        """);
            }
            if (drift && part[0].endsWith("040-apply.sql")) {
                execute(c,"""
                        insert into ingredient_culinary_country select id,'GB-SCT' from ingredient_concept
                        where code='OATS' on conflict do nothing;
                        create temp table issue_293_scotland_version as
                        select id,version from ingredient_concept where code='OATS';
                        """);
            }
            if (drift && part[0].endsWith("041-apply.sql")) {
                execute(c,"""
                        insert into ingredient_culinary_country select id,'FI' from ingredient_concept
                        where code='RYE_BREAD' on conflict do nothing;
                        create temp table issue_293_finland_version as
                        select id,version from ingredient_concept where code='RYE_BREAD';
                        """);
            }
            if (drift && part[0].endsWith("042-apply.sql")) {
                execute(c,"""
                        insert into ingredient_culinary_country select c.id,t.country_code
                        from d3_approved_country_relation t join ingredient_concept c on c.code=t.concept_code
                        on conflict do nothing;
                        create temp table issue_293_d3_versions as select id,version from ingredient_concept;
                        """);
            }
            if (drift && part[0].endsWith("047-apply.sql")) {
                execute(c,"""
                        update ingredient_concept c set base_draw_weight=t.target_weight
                        from approved_catalog_draw_weight t
                        where c.code=t.code and t.code=(select min(code) from approved_catalog_draw_weight);
                        create temp table issue_293_weight_version as
                        select c.id,c.version from ingredient_concept c
                        where c.code=(select min(code) from approved_catalog_draw_weight);
                        """);
            }
            List<String> lines = source.lines().toList();
            execute(c, String.join("\n", lines.subList(Integer.parseInt(part[1])-1,Integer.parseInt(part[2]))));
            if (part[0].endsWith("039-apply.sql")) {
        assertThat(jdbc(c).queryForObject("""
                select count(*) from availability_r3_review target
                join ingredient_concept c using(code)
                cross join lateral (values ('GEORGIA',target.georgia_target_level,target.georgia_target_note),
                    ('TOBIAS',target.tobias_target_level,target.tobias_target_note)) t(code,level,note)
                join participant p on p.code=t.code
                left join ingredient_availability a on a.ingredient_concept_id=c.id and a.participant_id=p.id
                where row(a.availability_level,a.curator_note) is distinct from row(t.level,t.note)
                """,Integer.class)).isZero();
            }
            if (drift && part[0].endsWith("040-apply.sql")) {
                assertThat(jdbc(c).queryForObject("select count(*) from ingredient_concept c join issue_293_scotland_version v using(id) where c.version<>v.version",Integer.class)).isZero();
            }
            if (drift && part[0].endsWith("041-apply.sql")) {
                assertThat(jdbc(c).queryForObject("select count(*) from ingredient_concept c join issue_293_finland_version v using(id) where c.version<>v.version",Integer.class)).isZero();
            }
            if (drift && part[0].endsWith("042-apply.sql")) {
                assertThat(jdbc(c).queryForObject("select count(*) from ingredient_concept c join issue_293_d3_versions v using(id) where c.version<>v.version",Integer.class)).isZero();
            }
            if (drift && part[0].endsWith("047-apply.sql")) {
                assertThat(jdbc(c).queryForObject("select count(*) from ingredient_concept c join issue_293_weight_version v using(id) where c.version<>v.version",Integer.class)).isZero();
            }
        }
        var jdbc=jdbc(c);
        assertThat(jdbc.queryForObject("""
                select count(*) from approved_catalog_draw_weight target join ingredient_concept c using(code)
                where c.base_draw_weight is distinct from target.target_weight
                """,Integer.class)).isZero();
        execute(c,read(DIRECTORY.resolve("record.sql")));
    }

    private static String state(Connection c) throws Exception {
        execute(c, "drop table if exists issue_293_history");
        execute(c, read(DIRECTORY.resolve("history.sql")));
        String state = jdbc(c).queryForObject("select pg_temp.issue_293_state()",String.class);
        execute(c,"drop table issue_293_history");
        return state;
    }
    private static String snapshot(Connection c) {
        return List.of("ingredient_concept", "ingredient_availability", "ingredient_refinement", "ingredient_culinary_country")
                .stream().map(table -> jdbc(c).queryForObject("select jsonb_agg(to_jsonb(r) order by to_jsonb(r)::text)::text from " + table + " r",String.class))
                .toList().toString();
    }
    private static String read(Path p) throws Exception { return Files.readString(p).replace("\r\n","\n"); }
    private static void execute(Connection c,String sql) throws SQLException { try(var s=c.createStatement()) { s.execute(sql); } }
    private static JdbcTemplate jdbc(Connection c) { return new JdbcTemplate(new SingleConnectionDataSource(c,true)); }
    private static Liquibase liquibase(Connection c) throws Exception {
        return new Liquibase(MASTER,new ClassLoaderResourceAccessor(),DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c)));
    }
}
