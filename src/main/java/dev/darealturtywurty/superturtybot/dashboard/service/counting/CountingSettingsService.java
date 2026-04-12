package dev.darealturtywurty.superturtybot.dashboard.service.counting;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Counting;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.counting.CountingManager;
import dev.darealturtywurty.superturtybot.modules.counting.CountingMode;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CountingSettingsService {
    private final JDA jda;

    public CountingSettingsService(JDA jda) {
        this.jda = jda;
    }

    public DashboardCountingSettingsResponse getSettings(long guildId) {
        Guild guild = requireGuild(guildId);
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return new DashboardCountingSettingsResponse(guildData.getMaxCountingSuccession(), listChannels(guildId, guild), listAvailableModes());
    }

    public DashboardCountingSettingsResponse upsertChannel(long guildId, CountingChannelUpsertRequest request) {
        Guild guild = requireGuild(guildId);
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);

        if (request.getMaxCountingSuccession() != null) {
            int maxCountingSuccession = request.getMaxCountingSuccession();
            if (maxCountingSuccession < 1) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_succession",
                        "Maximum counting succession must be at least 1.");
            }

            guildData.setMaxCountingSuccession(maxCountingSuccession);
        }

        if (request.getChannelId() != null || request.getMode() != null) {
            if (request.getChannelId() == null || request.getMode() == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_channel",
                        "A channel ID and counting mode are required.");
            }

            long channelId = parseChannelId(request.getChannelId());
            CountingMode mode = parseMode(request.getMode());
            TextChannel channel = requireTextChannel(guild, channelId);

            Bson filter = Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", channelId));
            Counting existing = Database.getDatabase().counting.find(filter).first();
            if (existing != null && existing.getCountingMode() != null && existing.getCountingMode().equalsIgnoreCase(mode.name())) {
                Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
                return getSettings(guildId);
            }

            CountingManager.INSTANCE.removeCountingChannel(guild, channel);
            CountingManager.INSTANCE.setCountingChannel(guild, channel, mode);
        }

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return getSettings(guildId);
    }

    public DashboardCountingSettingsResponse deleteChannel(long guildId, long channelId) {
        requireGuild(guildId);
        Database.getDatabase().counting.deleteOne(Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", channelId)));
        return getSettings(guildId);
    }

    private List<DashboardCountingChannelInfo> listChannels(long guildId, Guild guild) {
        return Database.getDatabase().counting.find(Filters.eq("guild", guildId)).into(new ArrayList<>()).stream()
                .map(profile -> toChannelInfo(guild, profile))
                .sorted(Comparator
                        .comparing(DashboardCountingChannelInfo::connected).reversed()
                        .thenComparing(DashboardCountingChannelInfo::channelName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DashboardCountingChannelInfo::channelId))
                .toList();
    }

    private static DashboardCountingChannelInfo toChannelInfo(Guild guild, Counting profile) {
        TextChannel channel = guild.getTextChannelById(profile.getChannel());
        return new DashboardCountingChannelInfo(
                Long.toString(profile.getChannel()),
                channel == null ? "Unknown Channel" : channel.getName(),
                profile.getCountingMode(),
                channel != null,
                profile.getCurrentCount(),
                profile.getHighestCount()
        );
    }

    private static List<DashboardCountingModeInfo> listAvailableModes() {
        return Arrays.stream(CountingMode.values())
                .map(mode -> new DashboardCountingModeInfo(
                        mode.name(),
                        mode.getDisplayName(),
                        mode.getDescription()
                ))
                .toList();
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private static TextChannel requireTextChannel(Guild guild, long channelId) {
        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_channel",
                    "The supplied channel was not a valid text channel in this guild.");
        }

        return channel;
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_channel",
                    "A valid text channel ID is required.");
        }

        try {
            return Long.parseLong(channelId.trim());
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_channel",
                    "The supplied channel ID was not a valid Discord snowflake.");
        }
    }

    private static CountingMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_mode",
                    "A counting mode is required.");
        }

        try {
            return CountingMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_counting_mode",
                    "The supplied counting mode was not recognized.");
        }
    }
}
