package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeploymentDiscordSecretIsolationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void copiesDiscordSecretsOnlyToAConfiguredProductionInstance() throws Exception {
        Path admin = temporaryDirectory.resolve("admin.properties");
        Path discord = temporaryDirectory.resolve("discord.properties");
        Files.writeString(admin, "admin.enabled=true\n"
                + "mise-en-dice.discord.enabled=true\n"
                + "mise-en-dice.discord.token=accidental-admin-token\n", StandardCharsets.UTF_8);
        Files.writeString(discord, "mise-en-dice.discord.enabled=true\n"
                + "mise-en-dice.discord.token=production-bot-token\n"
                + "mise-en-dice.discord.guild-id=12345\n", StandardCharsets.UTF_8);

        Path production = temporaryDirectory.resolve("production.properties");
        Path preview = temporaryDirectory.resolve("preview.properties");
        Path missingSecretProduction = temporaryDirectory.resolve("missing-secret.properties");
        writeProperties(admin, discord, production, true, true);
        writeProperties(admin, discord, preview, false, false);
        writeProperties(admin, temporaryDirectory.resolve("missing-discord.properties"), missingSecretProduction, true, true);

        String productionProperties = Files.readString(production, StandardCharsets.UTF_8);
        String previewProperties = Files.readString(preview, StandardCharsets.UTF_8);
        String missingSecretProperties = Files.readString(missingSecretProduction, StandardCharsets.UTF_8);
        assertThat(productionProperties).contains("production-bot-token", "mise-en-dice.discord.enabled=true")
                .doesNotContain("accidental-admin-token");
        assertThat(previewProperties).contains("mise-en-dice.discord.enabled=false")
                .doesNotContain("production-bot-token", "accidental-admin-token", "mise-en-dice.discord.enabled=true");
        assertThat(missingSecretProperties).contains("mise-en-dice.discord.enabled=false")
                .doesNotContain("production-bot-token", "accidental-admin-token", "mise-en-dice.discord.enabled=true");
    }

    private static void writeProperties(Path admin, Path discord, Path destination, boolean secureCookie, boolean includeDiscord)
            throws IOException, InterruptedException {
        String script = "set -euo pipefail\n"
                + "source \"$1\"\n"
                + "ADMIN_PROPERTIES=\"$2\"\n"
                + "DISCORD_PROPERTIES=\"$3\"\n"
                + "write_instance_application_properties \"$4\" \"$5\" \"$6\"\n";
        Process process = new ProcessBuilder(bashExecutable(), "-c", script, "bash",
                Path.of("deploy/operator/runtime.sh").toAbsolutePath().toString(), admin.toString(), discord.toString(),
                destination.toString(), Boolean.toString(secureCookie), Boolean.toString(includeDiscord))
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.waitFor()).isZero();
        assertThat(output).doesNotContain("production-bot-token", "accidental-admin-token");
    }

    private static String bashExecutable() {
        Path gitBash = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:/Program Files"), "Git", "bin", "bash.exe");
        return Files.isExecutable(gitBash) ? gitBash.toString() : "bash";
    }
}
