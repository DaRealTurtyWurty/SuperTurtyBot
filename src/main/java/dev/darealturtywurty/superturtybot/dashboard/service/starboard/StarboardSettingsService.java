package dev.darealturtywurty.superturtybot.dashboard.service.starboard;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class StarboardSettingsService {
    private final JDA jda;

    public StarboardSettingsService(JDA jda) {
        this.jda = jda;
    }

    public StarboardSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public StarboardSettingsResponse updateSettings(long guildId, StarboardSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(guild, request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setStarboardEnabled(request.isStarboardEnabled());
        guildData.setStarboard(parseChannelId(request.getStarboardChannelId()));
        guildData.setMinimumStars(request.getMinimumStars());
        guildData.setBotStarsCount(request.isBotStarsCount());
        guildData.setShowcaseChannels(String.join(" ", normalizeChannelIds(request.getShowcaseChannelIds())));
        guildData.setStarboardMediaOnly(request.isStarboardMediaOnly());
        guildData.setStarEmoji(request.getStarEmoji().trim());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static StarboardSettingsResponse toResponse(GuildData guildData) {
        return new StarboardSettingsResponse(
                guildData.isStarboardEnabled(),
                guildData.getStarboard() == 0L ? null : Long.toString(guildData.getStarboard()),
                guildData.getMinimumStars(),
                guildData.isBotStarsCount(),
                GuildData.getLongs(guildData.getShowcaseChannels()).stream().map(String::valueOf).toList(),
                guildData.isStarboardMediaOnly(),
                guildData.getStarEmoji()
        );
    }

    private static void validateRequest(Guild guild, StarboardSettingsRequest request) {
        if (request.getMinimumStars() < 1 || request.getMinimumStars() > Math.max(1, guild.getMemberCount())) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_minimum_stars",
                    "Minimum stars must be between 1 and the guild member count.");
        }

        if (request.isStarboardEnabled() && parseChannelId(request.getStarboardChannelId()) == 0L) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_starboard_channel",
                    "A starboard channel is required while starboard is enabled.");
        }

        if (parseChannelId(request.getStarboardChannelId()) != 0L && guild.getTextChannelById(parseChannelId(request.getStarboardChannelId())) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_starboard_channel",
                    "The supplied starboard channel was not a text channel in this guild.");
        }

        for (String showcaseChannelId : normalizeChannelIds(request.getShowcaseChannelIds())) {
            TextChannel channel = guild.getTextChannelById(showcaseChannelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_showcase_channel",
                        "One or more showcase channels were not valid text channels in this guild.");
            }
        }

        String starEmoji = request.getStarEmoji() == null ? "" : request.getStarEmoji().trim();
        if (starEmoji.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_star_emoji",
                    "A star emoji is required.");
        }

        try {
            Emoji.fromFormatted(starEmoji);
        } catch (IllegalArgumentException ignored) {
            try {
                Emoji.fromUnicode(starEmoji);
            } catch (IllegalArgumentException exception) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_star_emoji",
                        "The supplied star emoji was not recognized as a Discord or Unicode emoji.");
            }
        }
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank())
            return 0L;

        try {
            long parsed = Long.parseLong(channelId.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                    "One of the supplied channel IDs was not a valid Discord snowflake.");
        }
    }

    private static List<String> normalizeChannelIds(List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty())
            return List.of();

        return new ArrayList<>(new LinkedHashSet<>(channelIds.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }
}
