package io.github.venomenon328.miseendice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class AdapterBoundaryTest {

    @Test
    void adaptersDoNotDependOnModuleInternalsOrJdbc() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("io.github.venomenon328.miseendice");

        noClasses()
                .that().resideInAnyPackage(
                        "..administration..",
                        "..discord.."
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..catalog.internal..",
                        "..challenge.internal..",
                        "org.springframework.jdbc..",
                        "javax.sql.."
                )
                .check(classes);
    }

    @Test
    void otherModulesDoNotUseTheCatalogAuditPersistencePortDirectly() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("io.github.venomenon328.miseendice");

        noClasses()
                .that().resideInAnyPackage(
                        "..administration..",
                        "..discord..",
                        "..challenge.."
                )
                .should().dependOnClassesThat().haveFullyQualifiedName(
                        "io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog"
                )
                .because("catalog audit persistence is an internal foundation; adapters must use later application APIs")
                .check(classes);
    }

    @Test
    void challengeUsesOnlyThePublicCatalogApiAndConfinesJdbcToItsRepositoryAdapter() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("io.github.venomenon328.miseendice");

        noClasses()
                .that().resideInAPackage("..challenge..")
                .should().dependOnClassesThat().resideInAPackage("..catalog.internal..")
                .check(classes);

        noClasses()
                .that().resideInAPackage("..challenge.api..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..", "javax.sql..")
                .check(classes);

        noClasses()
                .that().resideInAPackage("..challenge.internal..")
                .and().doNotHaveSimpleName("JdbcGenerationRepository")
                .and().doNotHaveSimpleName("JdbcCurationRepository")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..", "javax.sql..")
                .because("explicit SQL is confined to the challenge persistence adapters")
                .check(classes);
    }
}
