package dev.darealturtywurty.superturtybot.modules.audit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.dashboard.DashboardConfig;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.DashboardNotifierEntry;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.DashboardNotifierSection;
import dev.darealturtywurty.superturtybot.dashboard.service.notifiers.NotifiersService;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.warnings.WarningSanctionConfig;
import dev.darealturtywurty.superturtybot.weblisteners.social.*;
import dev.darealturtywurty.superturtybot.weblisteners.social.game.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only diagnostics. Missing cache entries are described as missing/inaccessible, not proven deletions.
 */
public final class ServerAuditService {
    private static final Pattern ROLE_MENTION = Pattern.compile("<@&(\\d+)>");

    public record Finding(String section, String message, String settingsPage) {
    }

    public record HealthCheck(String name, String status, String detail) {
    }

    public static final class Report {
        private final List<Finding> findings = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<HealthCheck> healthChecks = new ArrayList<>();

        public List<HealthCheck> healthChecks() {
            return List.copyOf(this.healthChecks);
        }

        public void healthCheck(String name, String status, String detail) {
            this.healthChecks.add(new HealthCheck(name, status, detail));
        }

        public List<Finding> findings() {
            return List.copyOf(this.findings);
        }

        public void issue(String section, String message) {
            issue(section, message, "Notifiers".equals(section) ? "notifiers" : null);
        }

        public void issue(String section, String message, String settingsPage) {
            this.findings.add(new Finding(section, message, settingsPage));
        }

        public List<String> notes() {
            return List.copyOf(this.notes);
        }

        public void note(String message) {
            this.notes.add(message);
        }

    }

    public Report audit(Guild guild) {
        var report = new Report();
        checkPermissions(guild, report);
        try {
            GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
            checkSettings(guild, data == null ? new GuildData(guild.getIdLong()) : data, report);
            report.note("Settings checked against Discord's current cache; no settings were changed.");
        } catch (RuntimeException exception) {
            Constants.LOGGER.warn("Could not audit settings for guild {}", guild.getIdLong(), exception);
            report.issue("Settings",
                "INCOMPLETE: settings could not be read. Check database connectivity and bot logs.");
        }

        try {
            checkNotifiers(guild, report);
        } catch (RuntimeException exception) {
            Constants.LOGGER.warn("Could not audit notifiers for guild {}", guild.getIdLong(), exception);
            report.issue("Notifiers",
                "INCOMPLETE: notifier records could not be read. Check database connectivity and bot logs.");
        }

        checkHealth(guild.getJDA(), report);
        return report;
    }

    private void checkPermissions(Guild guild, Report report) {
        for (Permission permission : List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY)) {
            requirePermission(guild, permission, "General bot functionality", report);
        }
    }

    private void requirePermission(Guild guild, Permission permission, String feature, Report report) {
        if (!guild.getSelfMember().hasPermission(permission)) {
            report.issue("Permissions", feature + ": missing server permission " + permission.getName()
                + ". Grant it to a bot role (channel overrides are checked separately).");
        }
    }

    public void checkSettings(Guild guild, GuildData data, Report report) {
        Map<String, Long> destinations = createDestinations(data);
        destinations.forEach((key, id) -> {
            if (id != 0) {
                checkChannel(guild, id, key, "Settings", true, report);
            }
        });
        Map<String, String> channelLists = createChannelLists(data);
        channelLists.forEach((key, value) -> checkIds(value, key, report,
            id -> checkChannel(guild, id, key, "Settings", false, report)));
        checkIds(data.getXpBoostedRoles(), "xpBoostedRoles", report,
            id -> checkRole(guild, id, "xpBoostedRoles", false, report));
        checkIds(data.getModmailModeratorRoles(), "modmailModeratorRoles", report,
            id -> checkRole(guild, id, "modmailModeratorRoles", false, report));
        if (data.getPatronRole() != 0) {
            checkRole(guild, data.getPatronRole(), "patronRole", false, report);
        }

        if (data.getLevelRoles() != null && !data.getLevelRoles().isBlank()) {
            for (String entry : data.getLevelRoles().split("[ ;]+")) {
                String[] parts = entry.split("->", -1);
                try {
                    if (parts.length != 2 || Integer.parseInt(parts[0]) < 0)
                        throw new NumberFormatException();

                    long role = Long.parseLong(parts[1]);
                    if (role <= 0)
                        throw new NumberFormatException();

                    checkRole(guild, role, "levelRoles", true, report);
                } catch (NumberFormatException exception) {
                    report.issue("Settings",
                        "levelRoles contains a malformed level->role mapping. Correct it in server settings.",
                        "levelling");
                }
            }
        }
        requiredDestination(data.isStarboardEnabled(), data.getStarboard(), "starboard", report);
        requiredDestination(data.isHasLevelUpChannel(), data.getLevelUpMessageChannel(), "levelUpMessageChannel",
            report);
        requiredDestination(data.isChatRevivalEnabled(), data.getChatRevivalChannel(), "chatRevivalChannel", report);
        if (data.getMinXP() > data.getMaxXP()) {
            report.issue("Settings", "minXP exceeds maxXP. Correct the XP range.", "levelling");
        }

        if (data.isStickyRolesEnabled() || (data.getLevelRoles() != null && !data.getLevelRoles().isBlank())) {
            requirePermission(guild, Permission.MANAGE_ROLES, "Configured role assignments", report);
        }

        if (data.isImageSpamAutoBanEnabled()) {
            requirePermission(guild, Permission.BAN_MEMBERS, "Image spam auto-ban", report);
        }

        if (data.isDiscordInviteGuardEnabled() || data.isScamDetectionEnabled()) {
            requirePermission(guild, Permission.MESSAGE_MANAGE, "Automod message removal", report);
        }

        if (data.getAutoThreadChannels() != null && !data.getAutoThreadChannels().isBlank()) {
            requirePermission(guild, Permission.CREATE_PUBLIC_THREADS, "Automatic threads", report);
        }

        List<WarningSanctionConfig> sanctions = data.getWarningSanctions();
        if (sanctions == null || sanctions.isEmpty()) {
            sanctions = GuildData.createDefaultWarningSanctions();
        }

        Set<Permission> sanctionPermissions = EnumSet.noneOf(Permission.class);
        for (WarningSanctionConfig sanction : sanctions) {
            if (sanction == null || sanction.getAction() == null || sanction.getWarningCount() <= 0) {
                report.issue("Settings",
                    "warningSanctions contains an invalid action or warning threshold. Correct it in the dashboard.",
                    "warnings");
                continue;
            }

            sanctionPermissions.addAll(sanction.getAction().getRequiredPermissions());
        }

        sanctionPermissions.forEach(permission -> requirePermission(guild, permission, "Warning sanctions", report));
        if (data.getVoiceChannelNotifiers() != null) {
            data.getVoiceChannelNotifiers().forEach((key, notifier) -> {
                String label = "Voice notifier " + key;
                if (notifier == null) {
                    report.issue("Notifiers", label + " has an empty configuration. Remove or recreate it.",
                        "voice-channel-notifiers");
                    return;
                }

                if (notifier.getVoiceChannelId() <= 0
                    || guild.getVoiceChannelById(notifier.getVoiceChannelId()) == null) {
                    report.issue("Notifiers", label + " watches a missing or invalid voice channel. Reconfigure it.",
                        "voice-channel-notifiers");
                }

                checkChannel(guild, notifier.getSendToChannelId(), label, "Notifiers", true, report);
                if (notifier.getMentionRoles() != null) {
                    notifier.getMentionRoles().forEach(id -> {
                        if (id == null) {
                            report.issue("Notifiers", label + " has an empty role reference.",
                                "voice-channel-notifiers");
                        } else {
                            checkRole(guild, id, label, false, report);
                        }
                    });
                }

                if (!notifier.isEnabled()) {
                    report.note(label + " is disabled.");
                }
            });
        }
    }

    private static String settingsPage(String label) {
        if (label.startsWith("Voice notifier"))
            return "voice-channel-notifiers";
        if (label.contains("notifier"))
            return "notifiers";
        return switch (label) {
            case "modLogging", "loggingChannel" -> "logging";
            case "suggestions" -> "suggestions";
            case "birthdayChannel" -> "birthday";
            case "welcomeChannel" -> "welcome";
            case "collectorChannel" -> "collectables";
            case "starboard", "showcaseChannels" -> "starboard";
            case "levelUpMessageChannel", "disabledLevellingChannels", "xpBoostedChannels", "xpBoostedRoles",
                "levelRoles" -> "levelling";
            case "chatRevivalChannel" -> "chat-revival";
            case "optInChannels" -> "opt-in-channels";
            case "nsfwChannels" -> "nsfw";
            case "discordInviteWhitelistChannels" -> "automod";
            case "autoThreadChannels" -> "threads";
            case "aiChannelWhitelist" -> "ai";
            case "modmailModeratorRoles" -> "modmail";
            case "patronRole" -> "misc";
            default -> null;
        };
    }

    private static @NotNull Map<String, String> createChannelLists(GuildData data) {
        Map<String, String> channelLists = new LinkedHashMap<>();
        channelLists.put("optInChannels", data.getOptInChannels());
        channelLists.put("nsfwChannels", data.getNsfwChannels());
        channelLists.put("discordInviteWhitelistChannels", data.getDiscordInviteWhitelistChannels());
        channelLists.put("showcaseChannels", data.getShowcaseChannels());
        channelLists.put("disabledLevellingChannels", data.getDisabledLevellingChannels());
        channelLists.put("xpBoostedChannels", data.getXpBoostedChannels());
        channelLists.put("autoThreadChannels", data.getAutoThreadChannels());
        channelLists.put("aiChannelWhitelist", data.getAiChannelWhitelist());
        return channelLists;
    }

    private static @NotNull Map<String, Long> createDestinations(GuildData data) {
        Map<String, Long> destinations = new LinkedHashMap<>();
        destinations.put("modLogging", data.getModLogging());
        destinations.put("suggestions", data.getSuggestions());
        destinations.put("birthdayChannel", data.getBirthdayChannel());
        destinations.put("welcomeChannel", data.getWelcomeChannel());
        destinations.put("collectorChannel", data.getCollectorChannel());
        destinations.put("triviaChannel", data.getTriviaChannel());
        destinations.put("starboard", data.getStarboard());
        destinations.put("levelUpMessageChannel", data.getLevelUpMessageChannel());
        destinations.put("loggingChannel", data.getLoggingChannel());
        destinations.put("chatRevivalChannel", data.getChatRevivalChannel());
        return destinations;
    }

    private void requiredDestination(boolean enabled, long id, String key, Report report) {
        if (enabled && id == 0) {
            report.issue("Settings", key + " is unset for an enabled feature. Set it in server settings.",
                settingsPage(key));
        }
    }

    private static void checkIds(String value, String label, Report report, LongConsumer consumer) {
        if (value == null || value.isBlank())
            return;

        for (String token : value.trim().split("[ ;]+")) {
            try {
                long id = Long.parseLong(token);
                if (id <= 0)
                    throw new NumberFormatException();

                consumer.accept(id);
            } catch (NumberFormatException exception) {
                report.issue("Settings", label + " contains an invalid Discord ID. Correct it in server settings.",
                    settingsPage(label));
            }
        }
    }

    private void checkRole(Guild guild, long id, String label, boolean assignable, Report report) {
        Role role = id > 0 ? guild.getRoleById(id) : null;
        if (role == null) {
            report.issue("Settings",
                label + ": role " + id + " is deleted or unavailable. Remove or replace the reference.",
                settingsPage(label));
        } else if (assignable
            && (role.isManaged() || role.isPublicRole() || !guild.getSelfMember().canInteract(role))) {
            report.issue("Permissions",
                label + ": role " + id + " cannot be assigned. Use an unmanaged role below the bot's highest role.");
        }
    }

    private void checkChannel(Guild guild, long id, String label, String section, boolean delivery, Report report) {
        GuildChannel channel = id > 0 ? guild.getGuildChannelById(id) : null;
        if (channel == null) {
            report.issue(section,
                label + ": channel " + id + " is deleted or inaccessible. Remove or replace the reference.",
                settingsPage(label));
            return;
        }

        if (!delivery)
            return;

        if (!(channel instanceof StandardGuildMessageChannel destination)) {
            report.issue(section,
                label + ": channel " + id + " is not a text/announcement destination. Select a supported channel.",
                settingsPage(label));
            return;
        }

        if (!destination.canTalk()
            || !guild.getSelfMember().hasPermission(destination, Permission.MESSAGE_EMBED_LINKS)) {
            report.issue(section, label + ": cannot deliver to channel " + id
                + ". Grant View Channel, Send Messages and Embed Links in its overrides.");
        }
    }

    private void checkNotifiers(Guild guild, Report report) {
        var response = new NotifiersService(guild.getJDA()).getNotifiers(guild.getIdLong());
        Set<String> checkedTypes = new HashSet<>();
        for (DashboardNotifierSection section : response.sections()) {
            for (DashboardNotifierEntry entry : section.entries()) {
                String label = entry.kind() + " notifier (" + entry.targetValue() + ")";
                long channelId;
                try {
                    channelId = Long.parseLong(entry.channelId());
                } catch (RuntimeException exception) {
                    channelId = 0;
                }

                checkChannel(guild, channelId, label, "Notifiers", true, report);
                if (entry.targetValue() == null || entry.targetValue().isBlank()) {
                    report.issue("Notifiers", label + " has an empty target. Remove or recreate it with /notifier.");
                }

                if (entry.mention() != null) {
                    Matcher matcher = ROLE_MENTION.matcher(entry.mention());
                    while (matcher.find()) {
                        checkRole(guild, Long.parseLong(matcher.group(1)), label, false, report);
                    }
                }

                if (checkedTypes.add(entry.type()) && !listenerReady(entry.type())) {
                    report.issue("Notifiers", entry.kind()
                        + " listener is not running/initialized. Ask the bot operator to check startup logs.");
                }
            }
        }

        report.note("Checked " + response.totalCount()
            + " social/game notifiers. Stale means an unusable destination, target or listener; delivery timestamps and upstream account existence are not available.");
    }

    private boolean listenerReady(String type) {
        return switch (type) {
            case "youtube" -> YouTubeListener.isRunning();
            case "twitch" -> TwitchListener.isInitialized();
            case "reddit" -> RedditListener.isInitialized();
            case "steam" -> SteamListener.isRunning();
            case "steam-store" -> SteamStoreListener.isInitialized();
            case "minecraft" -> MinecraftListener.isInitialized();
            case "siege" -> SiegeListener.isInitialized();
            case "rocket-league" -> RocketLeagueListener.isInitialized();
            case "league" -> LeagueListener.isInitialized();
            case "valorant" -> ValorantListener.isInitialized();
            default -> false;
        };
    }

    private void checkHealth(JDA jda, Report report) {
        if (jda.getStatus() != JDA.Status.CONNECTED) {
            report.issue("Health", "Discord gateway status: " + jda.getStatus() + ". Check the bot connection.");
            report.healthCheck("Discord gateway", "failed", "Connection status: " + jda.getStatus());
        } else {
            report.healthCheck("Discord gateway", "passed", "The bot is connected to Discord.");
        }

        DashboardConfig config = DashboardConfig.fromEnvironment();
        if (!config.enabled()) {
            report.healthCheck("API key configuration", "skipped", "The dashboard/API is disabled by configuration.");
            report.healthCheck("Local dashboard/API", "skipped",
                "The dashboard/API is disabled; no HTTP request was made.");
            report.healthCheck("Public dashboard/API", "skipped",
                "The dashboard/API is disabled; no HTTP request was made.");
            return;
        }

        if (!config.hasApiKey()) {
            report.issue("Health", "Dashboard API key is unset. Ask the operator to configure DASHBOARD_API_KEY.");
            report.healthCheck("API key configuration", "failed", "No dashboard API key is configured.");
        } else {
            report.healthCheck("API key configuration", "passed",
                "An API key is configured. Authenticated routes are not tested by this check.");
        }

        String host = config.host();
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
            host = "127.0.0.1";
        }
        try {
            probeHealth(new URI("http", null, host, config.port(), "/api/health", null, null), "Local dashboard/API",
                report);
        } catch (Exception exception) {
            report.issue("Health",
                "Local dashboard URL configuration is invalid. Ask the operator to check host and port.");
            report.healthCheck("Local dashboard/API", "failed", "The local API URL could not be constructed.");
        }

        if (config.publicUrl() == null) {
            report.healthCheck("Public dashboard/API", "skipped",
                "No public API URL is configured; external reachability was not checked.");
            return;
        }
        try {
            URI base = URI.create(config.publicUrl());
            if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme())) || base.getHost() == null
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null)
                throw new IllegalArgumentException("Invalid public URL");
            probeHealth(URI.create(config.publicUrl().replaceAll("/+$", "") + "/api/health"), "Public dashboard/API",
                report);
        } catch (Exception exception) {
            report.issue("Health",
                "Public dashboard URL configuration is invalid. Ask the operator to check the public API URL.");
            report.healthCheck("Public dashboard/API", "failed", "The public API URL is invalid.");
        }
    }

    private void probeHealth(URI uri, String label, Report report) {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || !healthyBody(response.body())) {
                report.issue("Health",
                    label + " health check failed (HTTP " + response.statusCode() + "). Check service/proxy logs.");
                report.healthCheck(label, "failed",
                    "The /api/health response was unsuccessful or did not report a connected bot (HTTP "
                        + response.statusCode() + ").");
            } else {
                report.healthCheck(label, "passed",
                    "The /api/health endpoint returned HTTP 200 and reported a connected bot.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            report.issue("Health", label + " check was interrupted; health is unknown.");
            report.healthCheck(label, "skipped", "The check was interrupted; health is unknown.");
        } catch (Exception exception) {
            report.issue("Health",
                label + " is unreachable or timed out. Check the service, firewall and reverse proxy.");
            report.healthCheck(label, "failed", "The health endpoint could not be reached within the timeout.");
        }
    }

    private static boolean healthyBody(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return "ok".equals(json.get("status").getAsString())
                && "CONNECTED".equals(json.get("botStatus").getAsString());
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
