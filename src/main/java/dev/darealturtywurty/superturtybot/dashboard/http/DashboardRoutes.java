package dev.darealturtywurty.superturtybot.dashboard.http;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.dashboard.DashboardConfig;
import dev.darealturtywurty.superturtybot.dashboard.service.automod.AutomodSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.automod.AutomodSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.ai.AiSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.ai.AiSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.chat_revival.ChatRevivalSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.chat_revival.ChatRevivalSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.collectables.CollectablesSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.collectables.CollectablesSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.counting.CountingChannelUpsertRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.counting.CountingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.birthday.BirthdaySettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.birthday.BirthdaySettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.opt_in.OptInChannelsSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.opt_in.OptInChannelsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.modmail.ModmailSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.modmail.ModmailSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets.ModmailTicketsService;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.NotifiersService;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.DashboardNotifierMutationRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.reports.ReportsService;
import dev.darealturtywurty.superturtybot.dashboard.service.session.DashboardSessionService;
import dev.darealturtywurty.superturtybot.dashboard.service.session.DashboardUserProfileService;
import dev.darealturtywurty.superturtybot.dashboard.service.economy.EconomySettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.economy.EconomySettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.guild_config.GuildConfigCatalogService;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.GuildSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.levelling.LevellingSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.levelling.LevellingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.logging.LoggingSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.logging.LoggingSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.nsfw.NsfwSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.nsfw.NsfwSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.threads.ThreadSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.threads.ThreadSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers.VoiceChannelNotifierDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers.VoiceChannelNotifierUpsertRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.warnings.WarningsSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.warnings.WarningsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.starboard.StarboardSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.starboard.StarboardSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.welcome.WelcomeSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.welcome.WelcomeSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionsSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionActionRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionsDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.suggestions.SuggestionsSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.misc.MiscSettingsRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.misc.MiscSettingsService;
import dev.darealturtywurty.superturtybot.dashboard.service.quotes.QuotesDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.tags.DashboardTagCreateRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.tags.TagsDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.service.sticky_messages.StickyMessagesRequest;
import dev.darealturtywurty.superturtybot.dashboard.service.sticky_messages.StickyMessagesService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class DashboardRoutes {
    private static final String API_KEY_HEADER = "X-TurtyBot-Api-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ALLOWED_HEADERS = "Authorization, Content-Type, X-TurtyBot-Api-Key";
    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";

    private final DashboardConfig config;
    private final JDA jda;
    private final GuildConfigCatalogService catalogService;
    private final GuildSettingsService guildSettingsService;
    private final DashboardSessionService sessionService;
    private final DashboardUserProfileService userProfileService;
    private final StarboardSettingsService starboardSettingsService;
    private final LevellingSettingsService levellingSettingsService;
    private final LoggingSettingsService loggingSettingsService;
    private final WarningsSettingsService warningsSettingsService;
    private final EconomySettingsService economySettingsService;
    private final WelcomeSettingsService welcomeSettingsService;
    private final BirthdaySettingsService birthdaySettingsService;
    private final SuggestionsSettingsService suggestionsSettingsService;
    private final CollectablesSettingsService collectablesSettingsService;
    private final OptInChannelsSettingsService optInChannelsSettingsService;
    private final SuggestionsDashboardService suggestionsDashboardService;
    private final AiSettingsService aiSettingsService;
    private final ChatRevivalSettingsService chatRevivalSettingsService;
    private final NsfwSettingsService nsfwSettingsService;
    private final ThreadSettingsService threadSettingsService;
    private final MiscSettingsService miscSettingsService;
    private final CountingSettingsService countingSettingsService;
    private final QuotesDashboardService quotesDashboardService;
    private final TagsDashboardService tagsDashboardService;
    private final AutomodSettingsService automodSettingsService;
    private final ModmailSettingsService modmailSettingsService;
    private final ModmailTicketsService modmailTicketsService;
    private final NotifiersService notifiersService;
    private final ReportsService reportsService;
    private final StickyMessagesService stickyMessagesService;
    private final VoiceChannelNotifierDashboardService voiceChannelNotifierDashboardService;

    public DashboardRoutes(
            DashboardConfig config,
            JDA jda,
            GuildConfigCatalogService catalogService,
            GuildSettingsService guildSettingsService,
            DashboardSessionService sessionService,
            DashboardUserProfileService userProfileService,
            StarboardSettingsService starboardSettingsService,
            LevellingSettingsService levellingSettingsService,
            LoggingSettingsService loggingSettingsService,
            WarningsSettingsService warningsSettingsService,
            EconomySettingsService economySettingsService,
            WelcomeSettingsService welcomeSettingsService,
            BirthdaySettingsService birthdaySettingsService,
            SuggestionsSettingsService suggestionsSettingsService,
            CollectablesSettingsService collectablesSettingsService,
            OptInChannelsSettingsService optInChannelsSettingsService,
            SuggestionsDashboardService suggestionsDashboardService,
            AiSettingsService aiSettingsService,
            ChatRevivalSettingsService chatRevivalSettingsService,
            NsfwSettingsService nsfwSettingsService,
            ThreadSettingsService threadSettingsService,
            MiscSettingsService miscSettingsService,
            CountingSettingsService countingSettingsService,
            QuotesDashboardService quotesDashboardService,
            TagsDashboardService tagsDashboardService,
            AutomodSettingsService automodSettingsService,
            ModmailSettingsService modmailSettingsService,
            ModmailTicketsService modmailTicketsService,
            NotifiersService notifiersService,
            ReportsService reportsService,
            StickyMessagesService stickyMessagesService,
            VoiceChannelNotifierDashboardService voiceChannelNotifierDashboardService
    ) {
        this.config = config;
        this.jda = jda;
        this.catalogService = catalogService;
        this.guildSettingsService = guildSettingsService;
        this.sessionService = sessionService;
        this.userProfileService = userProfileService;
        this.starboardSettingsService = starboardSettingsService;
        this.levellingSettingsService = levellingSettingsService;
        this.loggingSettingsService = loggingSettingsService;
        this.warningsSettingsService = warningsSettingsService;
        this.economySettingsService = economySettingsService;
        this.welcomeSettingsService = welcomeSettingsService;
        this.birthdaySettingsService = birthdaySettingsService;
        this.suggestionsSettingsService = suggestionsSettingsService;
        this.collectablesSettingsService = collectablesSettingsService;
        this.optInChannelsSettingsService = optInChannelsSettingsService;
        this.suggestionsDashboardService = suggestionsDashboardService;
        this.aiSettingsService = aiSettingsService;
        this.chatRevivalSettingsService = chatRevivalSettingsService;
        this.nsfwSettingsService = nsfwSettingsService;
        this.threadSettingsService = threadSettingsService;
        this.miscSettingsService = miscSettingsService;
        this.countingSettingsService = countingSettingsService;
        this.quotesDashboardService = quotesDashboardService;
        this.tagsDashboardService = tagsDashboardService;
        this.automodSettingsService = automodSettingsService;
        this.modmailSettingsService = modmailSettingsService;
        this.modmailTicketsService = modmailTicketsService;
        this.notifiersService = notifiersService;
        this.reportsService = reportsService;
        this.stickyMessagesService = stickyMessagesService;
        this.voiceChannelNotifierDashboardService = voiceChannelNotifierDashboardService;
    }

    public void register(Javalin app) {
        app.before(this::handleBefore);

        app.exception(DashboardApiException.class, (exception, ctx) ->
                writeError(ctx, exception.status(), exception.errorCode(), exception.getMessage()));

        app.exception(Exception.class, (exception, ctx) -> {
            Constants.LOGGER.error("Unhandled dashboard request failure for {} {}", ctx.req().getMethod(), ctx.path(), exception);
            writeError(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "dashboard_internal_error",
                    "The dashboard service hit an unexpected error.");
        });

        app.error(HttpStatus.NOT_FOUND.getCode(), "application/json", ctx ->
                writeError(ctx, HttpStatus.NOT_FOUND, "dashboard_not_found", "The requested dashboard route does not exist."));

        app.get("/api/health", this::getHealth);
        app.get("/api/config/options", ctx -> ctx.json(this.catalogService.listOptions()));
        app.get("/api/guilds/{guildId}/config", ctx -> ctx.json(this.guildSettingsService.getGuildConfigSnapshot(parseGuildId(ctx))));
        app.get("/api/guilds/{guildId}/channels", ctx -> ctx.json(this.guildSettingsService.getGuildChannels(
                parseGuildId(ctx),
                parseUserIdQuery(ctx)
        )));
        app.get("/api/guilds/{guildId}/roles", ctx -> ctx.json(this.guildSettingsService.getGuildRoles(
                parseGuildId(ctx),
                parseUserIdQuery(ctx)
        )));
        app.get("/api/guilds/{guildId}/members", ctx -> ctx.json(this.guildSettingsService.searchGuildMembers(
                parseGuildId(ctx),
                parseUserIdQuery(ctx),
                ctx.queryParam("query")
        )));
        app.get("/api/guilds/{guildId}/config/starboard", ctx -> ctx.json(this.starboardSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/starboard", this::updateStarboardSettings);
        app.get("/api/guilds/{guildId}/config/levelling", ctx -> ctx.json(this.levellingSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/levelling", this::updateLevellingSettings);
        app.get("/api/guilds/{guildId}/config/logging", ctx -> ctx.json(this.loggingSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/logging", this::updateLoggingSettings);
        app.get("/api/guilds/{guildId}/warnings", ctx -> ctx.json(this.warningsSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/warnings", this::updateWarningsSettings);
        app.get("/api/guilds/{guildId}/warnings/users/{userId}", this::getWarningHistory);
        app.get("/api/guilds/{guildId}/warnings/{warningUuid}", this::getWarningDetail);
        app.delete("/api/guilds/{guildId}/warnings/{warningUuid}", this::deleteWarning);
        app.get("/api/guilds/{guildId}/notifiers", this::getNotifiers);
        app.post("/api/guilds/{guildId}/notifiers/{type}", this::createNotifier);
        app.put("/api/guilds/{guildId}/notifiers/{type}", this::updateNotifier);
        app.delete("/api/guilds/{guildId}/notifiers/{type}", this::deleteNotifier);
        app.get("/api/guilds/{guildId}/reports/{userId}", this::getReportsForUser);
        app.get("/api/guilds/{guildId}/config/economy", ctx -> ctx.json(this.economySettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/economy", this::updateEconomySettings);
        app.get("/api/guilds/{guildId}/config/welcome", ctx -> ctx.json(this.welcomeSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/welcome", this::updateWelcomeSettings);
        app.get("/api/guilds/{guildId}/config/birthday", ctx -> ctx.json(this.birthdaySettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/birthday", this::updateBirthdaySettings);
        app.get("/api/guilds/{guildId}/config/collectables", ctx -> ctx.json(this.collectablesSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/collectables", this::updateCollectablesSettings);
        app.get("/api/guilds/{guildId}/config/opt-in-channels", ctx -> ctx.json(this.optInChannelsSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/opt-in-channels", this::updateOptInChannelsSettings);
        app.get("/api/guilds/{guildId}/config/suggestions", ctx -> ctx.json(this.suggestionsSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/suggestions", this::updateSuggestionsSettings);
        app.get("/api/guilds/{guildId}/suggestions", ctx -> ctx.json(this.suggestionsDashboardService.getSuggestions(
                parseGuildId(ctx),
                parsePositiveIntQuery(ctx, "page", 1),
                parsePositiveIntQuery(ctx, "pageSize", 10)
        )));
        app.patch("/api/guilds/{guildId}/suggestions/{messageId}", this::moderateSuggestion);
        app.delete("/api/guilds/{guildId}/suggestions/{messageId}", this::deleteSuggestion);
        app.get("/api/guilds/{guildId}/config/ai", ctx -> ctx.json(this.aiSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/ai", this::updateAiSettings);
        app.get("/api/guilds/{guildId}/config/chat-revival", ctx -> ctx.json(this.chatRevivalSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/chat-revival", this::updateChatRevivalSettings);
        app.get("/api/guilds/{guildId}/config/nsfw", ctx -> ctx.json(this.nsfwSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/nsfw", this::updateNsfwSettings);
        app.get("/api/guilds/{guildId}/config/threads", ctx -> ctx.json(this.threadSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/threads", this::updateThreadSettings);
        app.get("/api/guilds/{guildId}/config/misc", ctx -> ctx.json(this.miscSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/misc", this::updateMiscSettings);
        app.get("/api/guilds/{guildId}/quotes", ctx -> ctx.json(this.quotesDashboardService.getQuotes(
                parseGuildId(ctx),
                parsePositiveIntQuery(ctx, "page", 1),
                parsePositiveIntQuery(ctx, "pageSize", 10)
        )));
        app.delete("/api/guilds/{guildId}/quotes/{quoteNumber}", this::deleteQuote);
        app.get("/api/guilds/{guildId}/tags", ctx -> ctx.json(this.tagsDashboardService.getTags(
                parseGuildId(ctx),
                parsePositiveIntQuery(ctx, "page", 1),
                parsePositiveIntQuery(ctx, "pageSize", 10)
        )));
        app.post("/api/guilds/{guildId}/tags", this::createTag);
        app.delete("/api/guilds/{guildId}/tags/{tagName}", this::deleteTag);
        app.get("/api/guilds/{guildId}/config/automod", ctx -> ctx.json(this.automodSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/automod", this::updateAutomodSettings);
        app.get("/api/guilds/{guildId}/config/modmail", ctx -> ctx.json(this.modmailSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/config/modmail", this::updateModmailSettings);
        app.get("/api/guilds/{guildId}/modmail/tickets", ctx -> ctx.json(this.modmailTicketsService.listTickets(
                parseGuildId(ctx),
                ctx.queryParam("status")
        )));
        app.get("/api/guilds/{guildId}/modmail/tickets/{ticketNumber}", this::getModmailTicket);
        app.get("/api/guilds/{guildId}/sticky-messages", ctx -> ctx.json(this.stickyMessagesService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/sticky-messages", this::upsertStickyMessage);
        app.delete("/api/guilds/{guildId}/sticky-messages/{channelId}", this::deleteStickyMessage);
        app.get("/api/guilds/{guildId}/counting", ctx -> ctx.json(this.countingSettingsService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/counting", this::upsertCountingChannel);
        app.delete("/api/guilds/{guildId}/counting/{channelId}", this::deleteCountingChannel);
        app.get("/api/guilds/{guildId}/voice-channel-notifiers", ctx ->
                ctx.json(this.voiceChannelNotifierDashboardService.getSettings(parseGuildId(ctx))));
        app.put("/api/guilds/{guildId}/voice-channel-notifiers", this::upsertVoiceChannelNotifier);
        app.delete("/api/guilds/{guildId}/voice-channel-notifiers/{voiceChannelId}", this::deleteVoiceChannelNotifier);

        app.get("/api/users/{userId}/profile", ctx -> ctx.json(this.userProfileService.getUserProfile(parseSnowflakeId(ctx, "userId", "invalid_user_id"))));
        app.post("/api/sessions", this::createSession);
        app.get("/api/sessions/{sessionId}", this::getSession);
        app.delete("/api/sessions/{sessionId}", this::deleteSession);
    }

    private void handleBefore(Context ctx) {
        if (!ctx.path().startsWith("/api"))
            return;

        applyCorsHeaders(ctx);
        ctx.header("Cache-Control", "no-store");

        if ("OPTIONS".equalsIgnoreCase(ctx.req().getMethod())) {
            ctx.status(HttpStatus.NO_CONTENT);
            ctx.result("");
            ctx.skipRemainingHandlers();
            return;
        }

        if ("/api/health".equals(ctx.path()))
            return;

        authenticate(ctx);
    }

    private void authenticate(Context ctx) {
        if (!this.config.hasApiKey())
            throw new DashboardApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "dashboard_api_key_not_configured",
                    "The dashboard API key has not been configured on the bot."
            );

        String providedApiKey = extractApiKey(ctx);
        if (providedApiKey == null || !isApiKeyMatch(this.config.apiKey(), providedApiKey))
            throw new DashboardApiException(
                    HttpStatus.UNAUTHORIZED,
                    "dashboard_invalid_api_key",
                    "The dashboard API key was missing or invalid."
            );
    }

    private void applyCorsHeaders(Context ctx) {
        String origin = ctx.header("Origin");
        if (!this.config.isOriginAllowed(origin))
            return;

        ctx.header("Access-Control-Allow-Origin", origin);
        ctx.header("Vary", "Origin");
        ctx.header("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        ctx.header("Access-Control-Allow-Methods", ALLOWED_METHODS);
    }

    private void getHealth(Context ctx) {
        ctx.json(new DashboardHealthResponse(
                "ok",
                Environment.INSTANCE.isDevelopment() ? "development" : "production",
                this.jda.getStatus().name(),
                TurtyBot.START_TIME,
                this.catalogService.listOptions().size(),
                this.config.publicUrl()
        ));
    }

    private void createSession(Context ctx) {
        DashboardSessionUpsertRequest request = ctx.bodyAsClass(DashboardSessionUpsertRequest.class);
        validateSessionRequest(request);
        ctx.status(HttpStatus.CREATED);
        ctx.json(this.sessionService.createOrReplaceSession(request));
    }

    private void updateStarboardSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        StarboardSettingsRequest request = ctx.bodyAsClass(StarboardSettingsRequest.class);
        ctx.json(this.starboardSettingsService.updateSettings(guildId, request));
    }

    private void updateLevellingSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        LevellingSettingsRequest request = ctx.bodyAsClass(LevellingSettingsRequest.class);
        ctx.json(this.levellingSettingsService.updateSettings(guildId, request));
    }

    private void updateLoggingSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        LoggingSettingsRequest request = ctx.bodyAsClass(LoggingSettingsRequest.class);
        ctx.json(this.loggingSettingsService.updateSettings(guildId, request));
    }

    private void updateWarningsSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        WarningsSettingsRequest request = ctx.bodyAsClass(WarningsSettingsRequest.class);
        ctx.json(this.warningsSettingsService.updateSettings(guildId, request));
    }

    private void getWarningDetail(Context ctx) {
        long guildId = parseGuildId(ctx);
        ctx.json(this.warningsSettingsService.getWarningDetail(guildId, ctx.pathParam("warningUuid")));
    }

    private void getWarningHistory(Context ctx) {
        long guildId = parseGuildId(ctx);
        long userId = parseSnowflakeId(ctx, "userId", "invalid_user_id");
        ctx.json(this.warningsSettingsService.getUserWarnings(guildId, userId));
    }

    private void getNotifiers(Context ctx) {
        long guildId = parseGuildId(ctx);
        ctx.json(this.notifiersService.getNotifiers(guildId));
    }

    private void createNotifier(Context ctx) {
        long guildId = parseGuildId(ctx);
        DashboardNotifierMutationRequest request = ctx.bodyAsClass(DashboardNotifierMutationRequest.class);
        ctx.json(this.notifiersService.addNotifier(guildId, ctx.pathParam("type"), request));
    }

    private void updateNotifier(Context ctx) {
        long guildId = parseGuildId(ctx);
        DashboardNotifierMutationRequest request = ctx.bodyAsClass(DashboardNotifierMutationRequest.class);
        ctx.json(this.notifiersService.updateNotifier(guildId, ctx.pathParam("type"), request));
    }

    private void deleteNotifier(Context ctx) {
        long guildId = parseGuildId(ctx);
        DashboardNotifierMutationRequest request = ctx.bodyAsClass(DashboardNotifierMutationRequest.class);
        ctx.json(this.notifiersService.deleteNotifier(guildId, ctx.pathParam("type"), request));
    }

    private void deleteWarning(Context ctx) {
        long guildId = parseGuildId(ctx);
        ctx.json(this.warningsSettingsService.deleteWarning(guildId, ctx.pathParam("warningUuid")));
    }

    private void getReportsForUser(Context ctx) {
        long guildId = parseGuildId(ctx);
        long userId = parseSnowflakeId(ctx, "userId", "invalid_user_id");
        ctx.json(this.reportsService.getUserReports(guildId, userId));
    }

    private void updateEconomySettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        EconomySettingsRequest request = ctx.bodyAsClass(EconomySettingsRequest.class);
        ctx.json(this.economySettingsService.updateSettings(guildId, request));
    }

    private void updateWelcomeSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        WelcomeSettingsRequest request = ctx.bodyAsClass(WelcomeSettingsRequest.class);
        ctx.json(this.welcomeSettingsService.updateSettings(guildId, request));
    }

    private void updateBirthdaySettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        BirthdaySettingsRequest request = ctx.bodyAsClass(BirthdaySettingsRequest.class);
        ctx.json(this.birthdaySettingsService.updateSettings(guildId, request));
    }

    private void updateCollectablesSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        CollectablesSettingsRequest request = ctx.bodyAsClass(CollectablesSettingsRequest.class);
        ctx.json(this.collectablesSettingsService.updateSettings(guildId, request));
    }

    private void updateOptInChannelsSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        OptInChannelsSettingsRequest request = ctx.bodyAsClass(OptInChannelsSettingsRequest.class);
        ctx.json(this.optInChannelsSettingsService.updateSettings(guildId, request));
    }

    private void updateSuggestionsSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        SuggestionsSettingsRequest request = ctx.bodyAsClass(SuggestionsSettingsRequest.class);
        ctx.json(this.suggestionsSettingsService.updateSettings(guildId, request));
    }

    private void moderateSuggestion(Context ctx) {
        long guildId = parseGuildId(ctx);
        String messageId = ctx.pathParam("messageId");
        SuggestionActionRequest request = ctx.bodyAsClass(SuggestionActionRequest.class);
        ctx.json(this.suggestionsDashboardService.moderateSuggestion(
                guildId,
                messageId,
                request,
                parsePositiveIntQuery(ctx, "page", 1),
                parsePositiveIntQuery(ctx, "pageSize", 10)
        ));
    }

    private void deleteSuggestion(Context ctx) {
        long guildId = parseGuildId(ctx);
        String messageId = ctx.pathParam("messageId");
        SuggestionActionRequest request = ctx.bodyAsClass(SuggestionActionRequest.class);
        ctx.json(this.suggestionsDashboardService.deleteSuggestion(
                guildId,
                messageId,
                request,
                parsePositiveIntQuery(ctx, "page", 1),
                parsePositiveIntQuery(ctx, "pageSize", 10)
        ));
    }

    private void updateAiSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        AiSettingsRequest request = ctx.bodyAsClass(AiSettingsRequest.class);
        ctx.json(this.aiSettingsService.updateSettings(guildId, request));
    }

    private void updateChatRevivalSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        ChatRevivalSettingsRequest request = ctx.bodyAsClass(ChatRevivalSettingsRequest.class);
        ctx.json(this.chatRevivalSettingsService.updateSettings(guildId, request));
    }

    private void updateNsfwSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        NsfwSettingsRequest request = ctx.bodyAsClass(NsfwSettingsRequest.class);
        ctx.json(this.nsfwSettingsService.updateSettings(guildId, request));
    }

    private void updateThreadSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        ThreadSettingsRequest request = ctx.bodyAsClass(ThreadSettingsRequest.class);
        ctx.json(this.threadSettingsService.updateSettings(guildId, request));
    }

    private void updateMiscSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        MiscSettingsRequest request = ctx.bodyAsClass(MiscSettingsRequest.class);
        ctx.json(this.miscSettingsService.updateSettings(guildId, request));
    }

    private void updateAutomodSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        AutomodSettingsRequest request = ctx.bodyAsClass(AutomodSettingsRequest.class);
        ctx.json(this.automodSettingsService.updateSettings(guildId, request));
    }

    private void updateModmailSettings(Context ctx) {
        long guildId = parseGuildId(ctx);
        ModmailSettingsRequest request = ctx.bodyAsClass(ModmailSettingsRequest.class);
        ctx.json(this.modmailSettingsService.updateSettings(guildId, request));
    }

    private void getModmailTicket(Context ctx) {
        long guildId = parseGuildId(ctx);
        long ticketNumber = parseLongValue(ctx.pathParam("ticketNumber"), "invalid_ticket_number");
        ctx.json(this.modmailTicketsService.getTicket(guildId, ticketNumber));
    }

    private void upsertStickyMessage(Context ctx) {
        long guildId = parseGuildId(ctx);
        StickyMessagesRequest request = ctx.bodyAsClass(StickyMessagesRequest.class);
        ctx.json(this.stickyMessagesService.upsertSticky(guildId, request));
    }

    private void deleteStickyMessage(Context ctx) {
        long guildId = parseGuildId(ctx);
        long channelId = parseSnowflakeId(ctx, "channelId", "invalid_sticky_channel");
        ctx.json(this.stickyMessagesService.deleteSticky(guildId, channelId));
    }

    private void upsertCountingChannel(Context ctx) {
        long guildId = parseGuildId(ctx);
        CountingChannelUpsertRequest request = ctx.bodyAsClass(CountingChannelUpsertRequest.class);
        ctx.json(this.countingSettingsService.upsertChannel(guildId, request));
    }

    private void deleteCountingChannel(Context ctx) {
        long guildId = parseGuildId(ctx);
        long channelId = parseSnowflakeId(ctx, "channelId", "invalid_channel_id");
        ctx.json(this.countingSettingsService.deleteChannel(guildId, channelId));
    }

    private void deleteQuote(Context ctx) {
        long guildId = parseGuildId(ctx);
        int quoteNumber = parsePositiveIntPath(ctx, "quoteNumber", "invalid_quote_number");
        int page = parsePositiveIntQuery(ctx, "page", 1);
        int pageSize = parsePositiveIntQuery(ctx, "pageSize", 10);
        ctx.json(this.quotesDashboardService.deleteQuote(guildId, quoteNumber, page, pageSize));
    }

    private void deleteTag(Context ctx) {
        long guildId = parseGuildId(ctx);
        String tagName = ctx.pathParam("tagName");
        int page = parsePositiveIntQuery(ctx, "page", 1);
        int pageSize = parsePositiveIntQuery(ctx, "pageSize", 10);
        ctx.json(this.tagsDashboardService.deleteTag(guildId, tagName, page, pageSize));
    }

    private void createTag(Context ctx) {
        long guildId = parseGuildId(ctx);
        DashboardTagCreateRequest request = ctx.bodyAsClass(DashboardTagCreateRequest.class);
        int page = parsePositiveIntQuery(ctx, "page", 1);
        int pageSize = parsePositiveIntQuery(ctx, "pageSize", 10);
        ctx.json(this.tagsDashboardService.createTag(guildId, request, page, pageSize));
    }

    private void upsertVoiceChannelNotifier(Context ctx) {
        long guildId = parseGuildId(ctx);
        VoiceChannelNotifierUpsertRequest request = ctx.bodyAsClass(VoiceChannelNotifierUpsertRequest.class);
        ctx.json(this.voiceChannelNotifierDashboardService.upsertNotifier(guildId, request));
    }

    private void deleteVoiceChannelNotifier(Context ctx) {
        long guildId = parseGuildId(ctx);
        long voiceChannelId = parseSnowflakeId(ctx, "voiceChannelId", "invalid_voice_channel_id");
        ctx.json(this.voiceChannelNotifierDashboardService.deleteNotifier(guildId, String.valueOf(voiceChannelId)));
    }

    private void getSession(Context ctx) {
        DashboardSessionResponse session = this.sessionService.getSession(parseSessionId(ctx));
        if (session == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_session_not_found",
                    "The dashboard session was missing or expired.");
        }

        ctx.json(session);
    }

    private void deleteSession(Context ctx) {
        this.sessionService.deleteSession(parseSessionId(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
        ctx.result("");
    }

    private static long parseGuildId(Context ctx) {
        return parseSnowflakeId(ctx, "guildId", "invalid_guild_id");
    }

    private static long parseUserIdQuery(Context ctx) {
        String userId = ctx.queryParam("userId");
        if (userId == null || userId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_user_id",
                    "The supplied user ID was missing.");
        }

        return parseSnowflake(userId, "invalid_user_id");
    }

    private static long parseSnowflakeId(Context ctx, String pathParam, String errorCode) {
        return parseSnowflake(ctx.pathParam(pathParam), errorCode);
    }

    private static long parseSnowflake(String value, String errorCode) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0L)
                throw new NumberFormatException("Snowflake ID must be positive.");

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, errorCode,
                    "The supplied ID was not a valid Discord snowflake.");
        }
    }

    private static int parsePositiveIntPath(Context ctx, String pathParam, String errorCode) {
        String value = ctx.pathParam(pathParam);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("Value must be positive.");
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, errorCode,
                    "The supplied ID was not a valid positive integer.");
        }
    }

    private static long parseLongValue(String value, String errorCode) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0L)
                throw new NumberFormatException("Numeric value must be positive.");

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, errorCode,
                    "The supplied value was not a valid number.");
        }
    }

    private static int parsePositiveIntQuery(Context ctx, String key, int fallback) {
        String raw = ctx.queryParam(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_query_param",
                    "The query parameter %s was not a valid positive integer.".formatted(key));
        }
    }

    private static String parseSessionId(Context ctx) {
        String sessionId = ctx.pathParam("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_session_id",
                    "The supplied dashboard session ID was missing.");
        }

        return sessionId;
    }

    private static void validateSessionRequest(DashboardSessionUpsertRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_session_id",
                    "The supplied dashboard session ID was missing.");
        }

        if (request.getUser() == null || request.getUser().getId() == null || request.getUser().getId().isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_session_user",
                    "The supplied dashboard session user was missing.");
        }

        if (request.getExpiresAtMs() <= request.getCreatedAtMs()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_session_expiry",
                    "The supplied dashboard session expiry was invalid.");
        }
    }

    private static String extractApiKey(Context ctx) {
        String directApiKey = ctx.header(API_KEY_HEADER);
        if (directApiKey != null && !directApiKey.isBlank())
            return directApiKey.trim();

        String authorization = ctx.header(AUTHORIZATION_HEADER);
        if (authorization == null)
            return null;

        if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length()))
            return null;

        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static boolean isApiKeyMatch(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void writeError(Context ctx, HttpStatus status, String errorCode, String message) {
        ctx.status(status);
        ctx.json(new DashboardErrorResponse(errorCode, message));
    }
}
