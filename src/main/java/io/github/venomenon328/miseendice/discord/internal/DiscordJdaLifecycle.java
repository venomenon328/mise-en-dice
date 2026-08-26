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
    private final DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow;
    private final DiscordResultCaptureWorkflow resultCaptureWorkflow;
    private final Executor executor;
    private final Executor autocompleteExecutor;
    private volatile JDA jda;
    private volatile boolean running;

    DiscordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                        DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                        DiscordChallengeArchiveWorkflow archiveWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, null, null, executor, executor);
    }

    DiscordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                        DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                        DiscordChallengeArchiveWorkflow archiveWorkflow,
                        DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, participantAdministrationWorkflow, null, executor,
                executor);
    }

    DiscordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                        DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                        DiscordChallengeArchiveWorkflow archiveWorkflow,
                        DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow,
                        DiscordResultCaptureWorkflow resultCaptureWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, participantAdministrationWorkflow,
                resultCaptureWorkflow, executor, executor);
    }

    DiscordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                        DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                        DiscordChallengeArchiveWorkflow archiveWorkflow,
                        DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow,
                        DiscordResultCaptureWorkflow resultCaptureWorkflow, Executor executor, Executor autocompleteExecutor) {
        this.properties = properties;
        this.workflow = workflow;
        this.ingredientLookupWorkflow = ingredientLookupWorkflow;
        this.archiveWorkflow = archiveWorkflow;
        this.participantAdministrationWorkflow = participantAdministrationWorkflow;
        this.resultCaptureWorkflow = resultCaptureWorkflow;
        this.executor = executor;
        this.autocompleteExecutor = autocompleteExecutor;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        properties.validateEnabledConfiguration();
        DiscordResultCaptureJdaListener resultListener = resultCaptureWorkflow == null ? null
                : new DiscordResultCaptureJdaListener(properties, resultCaptureWorkflow, archiveWorkflow, executor);
        jda = JDABuilder.createLight(properties.token(), GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(new DiscordJdaListener(properties, workflow, ingredientLookupWorkflow, archiveWorkflow,
                        participantAdministrationWorkflow, resultListener, executor, autocompleteExecutor)).build();
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
