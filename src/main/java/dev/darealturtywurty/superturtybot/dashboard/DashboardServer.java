package dev.darealturtywurty.superturtybot.dashboard;

import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardRoutes;
import dev.darealturtywurty.superturtybot.dashboard.service.automod.AutomodSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.ai.AiSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.chat_revival.ChatRevivalSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.counting.CountingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.birthday.BirthdaySettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.collectables.CollectablesSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.opt_in.OptInChannelsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.modmail.ModmailSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets.ModmailTicketsService;
import dev.darealturtywurty.superturtybot.dashboard.service.quotes.QuotesDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.tags.TagsDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.sticky_messages.StickyMessagesService;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.NotifiersService;
import dev.darealturtywurty.superturtybot.dashboard.service.reports.ReportsService;
import dev.darealturtywurty.superturtybot.dashboard.service.session.DashboardSessionService;
import dev.darealturtywurty.superturtybot.dashboard.service.session.DashboardUserProfileService;
import dev.darealturtywurty.superturtybot.dashboard.service.economy.EconomySettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.guild_config.GuildConfigCatalogService;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.GuildSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionsDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.misc.MiscSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.levelling.LevellingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.logging.LoggingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.nsfw.NsfwSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.threads.ThreadSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.warnings.WarningsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.starboard.StarboardSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.welcome.WelcomeSettingsService;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import net.dv8tion.jda.api.JDA;

import java.util.concurrent.atomic.AtomicReference;

public final class DashboardServer {
    private static final AtomicReference<DashboardServer> INSTANCE = new AtomicReference<>();

    private final DashboardConfig config;
    private final JDA jda;
    private Javalin app;

    private DashboardServer(DashboardConfig config, JDA jda) {
        this.config = config;
        this.jda = jda;
    }

    public static void startIfEnabled(JDA jda) {
        DashboardConfig config = DashboardConfig.fromEnvironment();
        if (!config.enabled()) {
            Constants.LOGGER.info("Dashboard service is disabled.");
            return;
        }

        var server = new DashboardServer(config, jda);
        if (!INSTANCE.compareAndSet(null, server)) {
            Constants.LOGGER.warn("Dashboard service was already started, skipping duplicate bootstrap.");
            return;
        }

        try {
            server.start();
            ShutdownHooks.register(server::stop);
        } catch (RuntimeException exception) {
            INSTANCE.compareAndSet(server, null);
            throw exception;
        }
    }

    private void start() {
        var catalogService = new GuildConfigCatalogService();
        var guildSettingsService = new GuildSettingsService(this.jda, catalogService);
        var sessionService = new DashboardSessionService();
        var userProfileService = new DashboardUserProfileService();
        var starboardSettingsService = new StarboardSettingsService(this.jda);
        var levellingSettingsService = new LevellingSettingsService(this.jda);
        var loggingSettingsService = new LoggingSettingsService(this.jda);
        var warningsSettingsService = new WarningsSettingsService(this.jda);
        var economySettingsService = new EconomySettingsService(this.jda);
        var welcomeSettingsService = new WelcomeSettingsService(this.jda);
        var birthdaySettingsService = new BirthdaySettingsService(this.jda);
        var collectablesSettingsService = new CollectablesSettingsService(this.jda);
        var optInChannelsSettingsService = new OptInChannelsSettingsService(this.jda);
        var suggestionsSettingsService = new SuggestionsSettingsService(this.jda);
        var suggestionsDashboardService = new SuggestionsDashboardService(this.jda);
        var aiSettingsService = new AiSettingsService(this.jda);
        var chatRevivalSettingsService = new ChatRevivalSettingsService(this.jda);
        var nsfwSettingsService = new NsfwSettingsService(this.jda);
        var threadSettingsService = new ThreadSettingsService(this.jda);
        var miscSettingsService = new MiscSettingsService(this.jda);
        var countingSettingsService = new CountingSettingsService(this.jda);
        var quotesDashboardService = new QuotesDashboardService(this.jda);
        var tagsDashboardService = new TagsDashboardService(this.jda);
        var automodSettingsService = new AutomodSettingsService(this.jda);
        var modmailSettingsService = new ModmailSettingsService(this.jda);
        var modmailTicketsService = new ModmailTicketsService(this.jda);
        var notifiersService = new NotifiersService(this.jda);
        var reportsService = new ReportsService(this.jda);
        var stickyMessagesService = new StickyMessagesService(this.jda);

        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.http.defaultContentType = "application/json";
            config.http.prefer405over404 = true;
            config.http.generateEtags = false;
            config.jsonMapper(new JavalinGson(Constants.GSON, false));
        });

        new DashboardRoutes(
                this.config,
                this.jda,
                catalogService,
                guildSettingsService,
                sessionService,
                userProfileService,
                starboardSettingsService,
                levellingSettingsService,
                loggingSettingsService,
                warningsSettingsService,
                economySettingsService,
                welcomeSettingsService,
                birthdaySettingsService,
                suggestionsSettingsService,
                collectablesSettingsService,
                optInChannelsSettingsService,
                suggestionsDashboardService,
                aiSettingsService,
                chatRevivalSettingsService,
                nsfwSettingsService,
                threadSettingsService,
                miscSettingsService,
                countingSettingsService,
                quotesDashboardService,
                tagsDashboardService,
                automodSettingsService,
                modmailSettingsService,
                modmailTicketsService,
                notifiersService,
                reportsService,
                stickyMessagesService
        ).register(this.app);
        this.app.start(this.config.host(), this.config.port());

        if (this.config.publicUrl() == null) {
            Constants.LOGGER.info("Dashboard service started on {}.", this.config.bindAddress());
        } else {
            Constants.LOGGER.info("Dashboard service started on {} ({})", this.config.bindAddress(), this.config.publicUrl());
        }
    }

    public void stop() {
        if (this.app == null)
            return;

        try {
            this.app.stop();
            Constants.LOGGER.info("Dashboard service stopped.");
        } catch (RuntimeException exception) {
            Constants.LOGGER.warn("Failed to stop dashboard service cleanly.", exception);
        } finally {
            this.app = null;
            INSTANCE.compareAndSet(this, null);
        }
    }
}
