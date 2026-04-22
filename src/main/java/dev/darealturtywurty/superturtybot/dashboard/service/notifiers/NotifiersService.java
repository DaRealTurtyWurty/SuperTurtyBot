package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.DashboardGuildInfo;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import dev.darealturtywurty.superturtybot.weblisteners.social.TwitchListener;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class NotifiersService {
    private final JDA jda;

    public NotifiersService(JDA jda) {
        this.jda = jda;
    }

    public DashboardNotifiersResponse getNotifiers(long guildId) {
        Guild guild = requireGuild(guildId);
        List<DashboardNotifierSection> sections = List.of(
                buildSocialSection(guildId, guild),
                buildGameSection(guildId, guild)
        );

        int totalCount = sections.stream().mapToInt(DashboardNotifierSection::count).sum();
        return new DashboardNotifiersResponse(toGuildInfo(guild), totalCount, sections);
    }

    public DashboardNotifiersResponse addNotifier(long guildId, String type, DashboardNotifierMutationRequest request) {
        Guild guild = requireGuild(guildId);
        String normalizedType = normalizeType(type);
        String mention = requireMention(request.getMention());
        validateDeliveryChannel(guild, request);

        switch (normalizedType) {
            case "youtube" -> {
                String youtubeChannel = requireTarget(request.getTarget(), "youtube_channel_id", "YouTube channel ID");
                if (Database.getDatabase().youtubeNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("youtubeChannel", youtubeChannel))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this YouTube channel.");
                }

                Database.getDatabase().youtubeNotifier.insertOne(new YoutubeNotifier(guildId, requireDiscordChannel(request), youtubeChannel, mention));
            }
            case "twitch" -> {
                String twitchChannel = requireTarget(request.getTarget(), "twitch_channel", "Twitch channel");
                if (Database.getDatabase().twitchNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", twitchChannel))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this Twitch channel.");
                }

                if (!TwitchListener.subscribeChannel(twitchChannel)) {
                    throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_invalid_target",
                            "The supplied Twitch channel could not be subscribed.");
                }

                Database.getDatabase().twitchNotifier.insertOne(new TwitchNotifier(guildId, twitchChannel, requireDiscordChannel(request), mention));
            }
            case "reddit" -> {
                String subreddit = requireTarget(request.getTarget(), "subreddit", "subreddit");
                if (Database.getDatabase().redditNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("subreddit", subreddit))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this subreddit.");
                }

                Database.getDatabase().redditNotifier.insertOne(new RedditNotifier(guildId, subreddit, requireDiscordChannel(request), mention));
            }
            case "steam" -> {
                int appId = requireAppId(request.getTarget());
                if (Database.getDatabase().steamNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("appId", appId))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this Steam app.");
                }

                Database.getDatabase().steamNotifier.insertOne(new SteamNotifier(guildId, requireDiscordChannel(request), appId, mention));
            }
            case "steam-store" -> insertSteamStoreNotifier(guildId, request, mention);
            case "minecraft" -> insertMinecraftNotifier(guildId, request, mention);
            case "siege" -> insertSiegeNotifier(guildId, request, mention);
            case "rocket-league" -> insertRocketLeagueNotifier(guildId, request, mention);
            case "league" -> insertLeagueNotifier(guildId, request, mention);
            case "valorant" -> insertValorantNotifier(guildId, request, mention);
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }

        return getNotifiers(guildId);
    }

    public DashboardNotifiersResponse updateNotifier(long guildId, String type, DashboardNotifierMutationRequest request) {
        Guild guild = requireGuild(guildId);
        String normalizedType = normalizeType(type);
        String mention = requireMention(request.getMention());
        validateDeliveryChannel(guild, request);

        switch (normalizedType) {
            case "youtube" -> updateMultiNotifier(guildId, request, mention, "youtube");
            case "twitch" -> updateTwitchNotifier(guildId, request, mention);
            case "reddit" -> updateMultiNotifier(guildId, request, mention, "reddit");
            case "steam" -> updateSteamNotifier(guildId, request, mention);
            case "steam-store" -> updateSteamStoreNotifier(guildId, request, mention);
            case "minecraft" -> updateMinecraftNotifier(guildId, request, mention);
            case "siege" -> updateSiegeNotifier(guildId, request, mention);
            case "rocket-league" -> updateRocketLeagueNotifier(guildId, request, mention);
            case "league" -> updateLeagueNotifier(guildId, request, mention);
            case "valorant" -> updateValorantNotifier(guildId, request, mention);
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }

        return getNotifiers(guildId);
    }

    public DashboardNotifiersResponse deleteNotifier(long guildId, String type, DashboardNotifierMutationRequest request) {
        requireGuild(guildId);
        String normalizedType = normalizeType(type);
        String target = request.getTarget();

        switch (normalizedType) {
            case "youtube" -> deleteMultiNotifier(guildId, target, "youtube");
            case "twitch" -> {
                deleteMultiNotifier(guildId, target, "twitch");
                if (target != null && !target.isBlank()) {
                    TwitchListener.unsubscribe(target);
                }
            }
            case "reddit" -> deleteMultiNotifier(guildId, target, "reddit");
            case "steam" -> {
                deleteMultiNotifier(guildId, target, "steam");
            }
            case "steam-store" -> deleteSingleNotifier(guildId, "steam-store");
            case "minecraft" -> deleteSingleNotifier(guildId, "minecraft");
            case "siege" -> deleteSingleNotifier(guildId, "siege");
            case "rocket-league" -> deleteSingleNotifier(guildId, "rocket-league");
            case "league" -> deleteSingleNotifier(guildId, "league");
            case "valorant" -> deleteSingleNotifier(guildId, "valorant");
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }

        return getNotifiers(guildId);
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private DashboardGuildInfo toGuildInfo(Guild guild) {
        return new DashboardGuildInfo(
                Long.toString(guild.getIdLong()),
                guild.getName(),
                guild.getIconUrl(),
                guild.getMemberCount(),
                true
        );
    }

    private DashboardNotifierSection buildSocialSection(long guildId, Guild guild) {
        List<DashboardNotifierEntry> entries = new ArrayList<>();
        Database.getDatabase().youtubeNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "youtube",
                        "YouTube",
                        notifier.getYoutubeChannel(),
                        notifier.getYoutubeChannel(),
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of(
                                "Stored videos: " + sizeOf(notifier.getStoredVideos()),
                                "Command: /notifier youtube"
                        )
                )));
        Database.getDatabase().twitchNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "twitch",
                        "Twitch",
                        notifier.getChannel(),
                        notifier.getChannel(),
                        resolveChannelName(guild, notifier.getDiscordChannel()),
                        notifier.getDiscordChannel(),
                        notifier.getMention(),
                        List.of("Command: /notifier twitch")
                )));
        Database.getDatabase().redditNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "reddit",
                        "Reddit",
                        "r/" + notifier.getSubreddit(),
                        notifier.getSubreddit(),
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Command: /notifier reddit")
                )));

        return new DashboardNotifierSection(
                "social",
                "Social Notifiers",
                "YouTube, Twitch, and Reddit feeds.",
                entries.size(),
                entries
        );
    }

    private DashboardNotifierSection buildGameSection(long guildId, Guild guild) {
        List<DashboardNotifierEntry> entries = new ArrayList<>();

        Database.getDatabase().steamNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "steam",
                        "Steam",
                        "App " + notifier.getAppId(),
                        Integer.toString(notifier.getAppId()),
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Command: /notifier steam", notifier.getPreviousData() == null ? "No cached article data yet." : "Has cached article data.")
                )));

        Database.getDatabase().steamStoreNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "steam-store",
                        "Steam Store",
                        "Steam sales / fests",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Stored articles: " + sizeOf(notifier.getStoredArticleIds()), "Command: /notifier steamstore")
                )));

        Database.getDatabase().minecraftNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "minecraft",
                        "Minecraft",
                        "Minecraft news",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of(
                                "Stored articles: " + sizeOf(notifier.getStoredArticles()),
                                "Created: " + formatTime(notifier.getCreatedAt()),
                                "Command: /notifier minecraft"
                        )
                )));

        Database.getDatabase().siegeNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "siege",
                        "Rainbow Six Siege",
                        "Siege news",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Stored articles: " + sizeOf(notifier.getStoredArticleIds()), "Command: /notifier siege")
                )));

        Database.getDatabase().rocketLeagueNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "rocket-league",
                        "Rocket League",
                        "Rocket League news",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Stored articles: " + sizeOf(notifier.getStoredArticleSlugs()), "Command: /notifier rocketleague")
                )));

        Database.getDatabase().leagueNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "league",
                        "League of Legends",
                        "League news",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Stored articles: " + sizeOf(notifier.getStoredArticleUrls()), "Command: /notifier league")
                )));

        Database.getDatabase().valorantNotifier.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>())
                .forEach(notifier -> entries.add(entry(
                        "valorant",
                        "VALORANT",
                        "VALORANT news",
                        null,
                        resolveChannelName(guild, notifier.getChannel()),
                        notifier.getChannel(),
                        notifier.getMention(),
                        List.of("Stored articles: " + sizeOf(notifier.getStoredArticleUrls()), "Command: /notifier valorant")
                )));

        return new DashboardNotifierSection(
                "games",
                "Game Notifiers",
                "Game, store, and patch feed notifiers.",
                entries.size(),
                entries
        );
    }

    private DashboardNotifierEntry entry(String type, String kind, String targetLabel, String targetValue, String channelName, long channelId, String mention, List<String> details) {
        return new DashboardNotifierEntry(
                type,
                kind,
                targetLabel,
                targetValue,
                Long.toString(channelId),
                channelName,
                mention,
                details
        );
    }

    private long requireDiscordChannel(DashboardNotifierMutationRequest request) {
        String discordChannelId = request.getDiscordChannelId();
        if (discordChannelId == null || discordChannelId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_missing_channel",
                    "A Discord channel is required for this notifier.");
        }

        try {
            long parsed = Long.parseLong(discordChannelId.trim());
            if (parsed <= 0L) {
                throw new NumberFormatException("Channel ID must be positive.");
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_invalid_channel",
                    "The selected channel ID was invalid.");
        }
    }

    private void validateDeliveryChannel(Guild guild, DashboardNotifierMutationRequest request) {
        long channelId = requireDiscordChannel(request);
        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, channelId);
        if (channel == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_invalid_channel",
                    "The selected channel must be a text or announcement channel.");
        }

        if (!channel.canTalk() || !guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_missing_channel_permissions",
                    "TurtyBot must be able to send messages and embeds in that channel before the notifier can be saved.");
        }
    }

    private String requireMention(String mention) {
        if (mention == null || mention.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_missing_mention",
                    "A mention is required for this notifier.");
        }

        return mention.trim();
    }

    private String requireTarget(String target, String errorCode, String label) {
        if (target == null || target.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_missing_target",
                    "A " + label + " is required for this notifier.");
        }

        return target.trim();
    }

    private int requireAppId(String target) {
        String rawTarget = requireTarget(target, "dashboard_notifier_missing_target", "Steam app ID");
        try {
            int appId = Integer.parseInt(rawTarget);
            if (appId <= 0) {
                throw new NumberFormatException("Steam app ID must be positive.");
            }

            return appId;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_invalid_target",
                    "The supplied Steam app ID was not valid.");
        }
    }

    private void insertSteamStoreNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().steamStoreNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a Steam sales/fests notifier configured.");
        }

        Database.getDatabase().steamStoreNotifier.insertOne(new SteamStoreNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void insertMinecraftNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().minecraftNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a Minecraft notifier configured.");
        }

        Database.getDatabase().minecraftNotifier.insertOne(new MinecraftNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void insertSiegeNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().siegeNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a Rainbow Six Siege notifier configured.");
        }

        Database.getDatabase().siegeNotifier.insertOne(new SiegeNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void insertRocketLeagueNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().rocketLeagueNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a Rocket League notifier configured.");
        }

        Database.getDatabase().rocketLeagueNotifier.insertOne(new RocketLeagueNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void insertLeagueNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().leagueNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a League of Legends notifier configured.");
        }

        Database.getDatabase().leagueNotifier.insertOne(new LeagueNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void insertValorantNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        if (Database.getDatabase().valorantNotifier.find(Filters.eq("guild", guildId)).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a VALORANT notifier configured.");
        }

        Database.getDatabase().valorantNotifier.insertOne(new ValorantNotifier(guildId, requireDiscordChannel(request), mention));
    }

    private void updateMultiNotifier(long guildId, DashboardNotifierMutationRequest request, String mention, String type) {
        String originalTarget = requireTarget(request.getOriginalTarget(), "dashboard_notifier_missing_target",
                "existing notifier target");
        String newTarget = requireTarget(request.getTarget(), "dashboard_notifier_missing_target",
                "notifier target");

        switch (type) {
            case "youtube" -> {
                YoutubeNotifier notifier = Database.getDatabase().youtubeNotifier.find(
                        Filters.and(Filters.eq("guild", guildId), Filters.eq("youtubeChannel", originalTarget))
                ).first();
                if (notifier == null) {
                    throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                            "That YouTube notifier no longer exists.");
                }

                if (!originalTarget.equals(newTarget)
                        && Database.getDatabase().youtubeNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("youtubeChannel", newTarget))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this YouTube channel.");
                }

                notifier.setChannel(requireDiscordChannel(request));
                notifier.setMention(mention);
                notifier.setYoutubeChannel(newTarget);
                Database.getDatabase().youtubeNotifier.replaceOne(
                        Filters.and(Filters.eq("guild", guildId), Filters.eq("youtubeChannel", originalTarget)),
                        notifier
                );
            }
            case "reddit" -> {
                RedditNotifier notifier = Database.getDatabase().redditNotifier.find(
                        Filters.and(Filters.eq("guild", guildId), Filters.eq("subreddit", originalTarget))
                ).first();
                if (notifier == null) {
                    throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                            "That Reddit notifier no longer exists.");
                }

                if (!originalTarget.equals(newTarget)
                        && Database.getDatabase().redditNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("subreddit", newTarget))).first() != null) {
                    throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                            "You already have a notifier for this subreddit.");
                }

                notifier.setChannel(requireDiscordChannel(request));
                notifier.setMention(mention);
                notifier.setSubreddit(newTarget);
                Database.getDatabase().redditNotifier.replaceOne(
                        Filters.and(Filters.eq("guild", guildId), Filters.eq("subreddit", originalTarget)),
                        notifier
                );
            }
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }
    }

    private void updateTwitchNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        String originalTarget = requireTarget(request.getOriginalTarget(), "dashboard_notifier_missing_target",
                "existing notifier target");
        String newTarget = requireTarget(request.getTarget(), "dashboard_notifier_missing_target",
                "Twitch channel");

        TwitchNotifier notifier = Database.getDatabase().twitchNotifier.find(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", originalTarget))
        ).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Twitch notifier no longer exists.");
        }

        if (!originalTarget.equals(newTarget)
                && Database.getDatabase().twitchNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", newTarget))).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a notifier for this Twitch channel.");
        }

        if (!originalTarget.equals(newTarget) && !TwitchListener.subscribeChannel(newTarget)) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_invalid_target",
                    "The supplied Twitch channel could not be subscribed.");
        }

        notifier.setDiscordChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        notifier.setChannel(newTarget);
        Database.getDatabase().twitchNotifier.replaceOne(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", originalTarget)),
                notifier
        );

        if (!originalTarget.equals(newTarget)) {
            TwitchListener.unsubscribe(originalTarget);
        }
    }

    private void updateSteamNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        String originalTarget = requireTarget(request.getOriginalTarget(), "dashboard_notifier_missing_target",
                "existing notifier target");
        int originalAppId = requireAppId(originalTarget);
        int appId = requireAppId(request.getTarget());

        SteamNotifier notifier = Database.getDatabase().steamNotifier.find(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("appId", originalAppId))
        ).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Steam notifier no longer exists.");
        }

        if (appId != originalAppId
                && Database.getDatabase().steamNotifier.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("appId", appId))).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "dashboard_notifier_exists",
                    "You already have a notifier for this Steam app.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        notifier.setAppId(appId);
        Database.getDatabase().steamNotifier.replaceOne(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("appId", originalAppId)),
                notifier
        );
    }

    private void updateSteamStoreNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        SteamStoreNotifier notifier = Database.getDatabase().steamStoreNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Steam sales/fests notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().steamStoreNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void updateMinecraftNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        MinecraftNotifier notifier = Database.getDatabase().minecraftNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Minecraft notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().minecraftNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void updateSiegeNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        SiegeNotifier notifier = Database.getDatabase().siegeNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Rainbow Six Siege notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().siegeNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void updateRocketLeagueNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        RocketLeagueNotifier notifier = Database.getDatabase().rocketLeagueNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That Rocket League notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().rocketLeagueNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void updateLeagueNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        LeagueNotifier notifier = Database.getDatabase().leagueNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That League of Legends notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().leagueNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void updateValorantNotifier(long guildId, DashboardNotifierMutationRequest request, String mention) {
        ValorantNotifier notifier = Database.getDatabase().valorantNotifier.find(Filters.eq("guild", guildId)).first();
        if (notifier == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_notifier_not_found",
                    "That VALORANT notifier no longer exists.");
        }

        notifier.setChannel(requireDiscordChannel(request));
        notifier.setMention(mention);
        Database.getDatabase().valorantNotifier.replaceOne(Filters.eq("guild", guildId), notifier);
    }

    private void deleteSingleNotifier(long guildId, String type) {
        switch (type) {
            case "steam-store" -> Database.getDatabase().steamStoreNotifier.deleteMany(Filters.eq("guild", guildId));
            case "minecraft" -> Database.getDatabase().minecraftNotifier.deleteMany(Filters.eq("guild", guildId));
            case "siege" -> Database.getDatabase().siegeNotifier.deleteMany(Filters.eq("guild", guildId));
            case "rocket-league" -> Database.getDatabase().rocketLeagueNotifier.deleteMany(Filters.eq("guild", guildId));
            case "league" -> Database.getDatabase().leagueNotifier.deleteMany(Filters.eq("guild", guildId));
            case "valorant" -> Database.getDatabase().valorantNotifier.deleteMany(Filters.eq("guild", guildId));
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }
    }

    private void deleteMultiNotifier(long guildId, String target, String type) {
        if (target == null || target.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_notifier_missing_target",
                    "A notifier target is required for this removal.");
        }

        switch (type) {
            case "youtube" -> Database.getDatabase().youtubeNotifier.deleteMany(Filters.and(Filters.eq("guild", guildId), Filters.eq("youtubeChannel", target.trim())));
            case "twitch" -> Database.getDatabase().twitchNotifier.deleteMany(Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", target.trim())));
            case "reddit" -> Database.getDatabase().redditNotifier.deleteMany(Filters.and(Filters.eq("guild", guildId), Filters.eq("subreddit", target.trim())));
            case "steam" -> Database.getDatabase().steamNotifier.deleteMany(Filters.and(Filters.eq("guild", guildId), Filters.eq("appId", requireAppId(target))));
            default -> throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "dashboard_invalid_notifier_type",
                    "The supplied notifier type was not recognized.");
        }

        return type.trim().toLowerCase();
    }

    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private String resolveChannelName(Guild guild, long channelId) {
        if (channelId == 0L) {
            return null;
        }

        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, channelId);
        return channel == null ? null : channel.getName();
    }

    private String formatTime(long millis) {
        if (millis <= 0L) {
            return "Unknown";
        }

        return TimeUtils.formatTime(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC));
    }
}
