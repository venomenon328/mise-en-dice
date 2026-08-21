package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeploymentDiscordSecretIsolationTest {

    Path temporaryDirectory;

    @BeforeEach
    void createTemporaryDirectory() throws IOException {
        temporaryDirectory = Files.createTempDirectory("mise-en-dice-provider-isolation-");
    }

    @AfterEach
    void restoreTemporaryFileWritePermissions() throws IOException {
        if (temporaryDirectory == null || Files.notExists(temporaryDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
            List<Path> reverseOrder = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : reverseOrder) {
                path.toFile().setWritable(true, false);
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void writesProviderSecretsOnlyToTheirConfiguredInstanceType() throws Exception {
        Path admin = temporaryDirectory.resolve("admin.properties");
        Path discord = temporaryDirectory.resolve("discord.properties");
        Path openai = temporaryDirectory.resolve("openai.properties");
        Path acceptance = temporaryDirectory.resolve("acceptance.properties");
        Files.writeString(admin, "admin.enabled=true\n"
                + "mise-en-dice.discord.enabled=true\n"
                + "mise-en-dice.discord.token=accidental-admin-token\n"
                + "mise-en-dice.curation.openai.api-key=accidental-admin-key\n", StandardCharsets.UTF_8);
        Files.writeString(discord, "mise-en-dice.discord.enabled=true\n"
                + "mise-en-dice.discord.token=production-bot-token\n"
                + "mise-en-dice.discord.guild-id=12345\n", StandardCharsets.UTF_8);
        Files.writeString(openai, "mise-en-dice.curation.openai.enabled=true\n"
                + "mise-en-dice.curation.openai.api-key=production-openai-key\n"
                + "mise-en-dice.curation.openai.model=gpt-5.6-terra\n"
                + "mise-en-dice.curation.openai.reasoning-effort=medium\n", StandardCharsets.UTF_8);
        Files.writeString(acceptance, "mise-en-dice.discord.enabled=true\n"
                + "mise-en-dice.discord.token=acceptance-bot-token\n"
                + "mise-en-dice.discord.guild-id=98765\n"
                + "mise-en-dice.curation.openai.enabled=true\n"
                + "mise-en-dice.curation.openai.api-key=acceptance-openai-key\n"
                + "mise-en-dice.curation.openai.model=gpt-5.6-terra\n"
                + "mise-en-dice.curation.openai.reasoning-effort=medium\n", StandardCharsets.UTF_8);

        Path production = temporaryDirectory.resolve("production.properties");
        Path liveAcceptance = temporaryDirectory.resolve("live-acceptance.properties");
        Path preview = temporaryDirectory.resolve("preview.properties");
        Path smoke = temporaryDirectory.resolve("smoke.properties");
        writeProperties(admin, discord, openai, acceptance, production, true, "production");
        writeProperties(admin, discord, openai, acceptance, liveAcceptance, false, "acceptance");
        writeProperties(admin, discord, openai, acceptance, preview, false, "none");
        writeProperties(admin, discord, openai, acceptance, smoke, false, "none");

        String productionProperties = Files.readString(production, StandardCharsets.UTF_8);
        String acceptanceProperties = Files.readString(liveAcceptance, StandardCharsets.UTF_8);
        String previewProperties = Files.readString(preview, StandardCharsets.UTF_8);
        String smokeProperties = Files.readString(smoke, StandardCharsets.UTF_8);
        assertThat(productionProperties)
                .contains("production-bot-token", "production-openai-key", "spring.profiles.active=production")
                .doesNotContain("acceptance-bot-token", "acceptance-openai-key", "accidental-admin-token", "accidental-admin-key");
        assertThat(acceptanceProperties)
                .contains("acceptance-bot-token", "acceptance-openai-key", "spring.profiles.active=production")
                .doesNotContain("production-bot-token", "production-openai-key", "accidental-admin-token", "accidental-admin-key");
        assertThat(previewProperties).contains("mise-en-dice.discord.enabled=false", "mise-en-dice.curation.openai.enabled=false")
                .doesNotContain("production-bot-token", "production-openai-key", "acceptance-bot-token", "acceptance-openai-key");
        assertThat(smokeProperties).contains("mise-en-dice.discord.enabled=false", "mise-en-dice.curation.openai.enabled=false")
                .doesNotContain("production-bot-token", "production-openai-key", "acceptance-bot-token", "acceptance-openai-key");
    }

    private static void writeProperties(Path admin, Path discord, Path openai, Path acceptance, Path destination,
                                        boolean secureCookie, String providerSource)
            throws IOException, InterruptedException {
        String script = "set -euo pipefail\n"
                + "source \"$1\"\n"
                + "ADMIN_PROPERTIES=\"$2\"\n"
                + "DISCORD_PROPERTIES=\"$3\"\n"
                + "OPENAI_PROPERTIES=\"$4\"\n"
                + "ACCEPTANCE_PROPERTIES=\"$5\"\n"
                + "write_instance_application_properties \"$6\" \"$7\" \"$8\"\n";
        Process process = new ProcessBuilder(bashExecutable(), "-c", script, "bash",
                Path.of("deploy/operator/runtime.sh").toAbsolutePath().toString(), admin.toString(), discord.toString(),
                openai.toString(), acceptance.toString(), destination.toString(), Boolean.toString(secureCookie), providerSource)
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.waitFor()).isZero();
        assertThat(output).doesNotContain("production-bot-token", "production-openai-key", "acceptance-bot-token",
                "acceptance-openai-key", "accidental-admin-token", "accidental-admin-key");
    }

    private static String bashExecutable() {
        Path gitBash = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:/Program Files"), "Git", "bin", "bash.exe");
        return Files.isExecutable(gitBash) ? gitBash.toString() : "bash";
    }
}
