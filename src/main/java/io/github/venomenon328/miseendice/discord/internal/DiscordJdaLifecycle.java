package io.github.venomenon328.miseendice.discord.internal;

import java.util.concurrent.Executor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.context.SmartLifecycle;

/** Starts JDA only for the explicitly enabled production adapter and shuts it down with Spring. */
final class DiscordJdaLifecycle implements SmartLifecycle {
    private final DiscordProperties properties;
    private final DiscordChallengeWorkflow workflow;
    private final DiscordIngredientLookupWorkflow ingredientLookupWorkflow;
    private final DiscordChallengeArchiveWorkflow archiveWorkflow;
    private final Executor executor;
    private volatile JDA jda;
    private volatile boolean running;

    DiscordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                        DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                        DiscordChallengeArchiveWorkflow archiveWorkflow, Executor executor) {
        this.properties = properties;
        this.workflow = workflow;
        this.ingredientLookupWorkflow = ingredientLookupWorkflow;
        this.archiveWorkflow = archiveWorkflow;
        this.executor = executor;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        properties.validateEnabledConfiguration();
        jda = JDABuilder.createLight(properties.token(), GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(new DiscordJdaListener(properties, workflow, ingredientLookupWorkflow, archiveWorkflow,
                        executor)).build();
        running = true;
    }

    @Override
    public synchronized void stop() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
