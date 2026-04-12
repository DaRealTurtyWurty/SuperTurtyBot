package dev.darealturtywurty.superturtybot.dashboard.service.opt_in;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class OptInChannelsSettingsService {
    private final JDA jda;

    public OptInChannelsSettingsService(JDA jda) {
        this.jda = jda;
    }

    public OptInChannelsSettingsResponse getSettings(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return new OptInChannelsSettingsResponse(toChannelIds(guild, guildData.getOptInChannels()));
    }

    public OptInChannelsSettingsResponse updateSettings(long guildId, OptInChannelsSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        List<String> optInChannelIds = sanitizeChannelIds(guild, request.optInChannelIds());

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setOptInChannels(joinDelimited(optInChannelIds));
        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);

        return new OptInChannelsSettingsResponse(optInChannelIds);
    }

    private List<String> sanitizeChannelIds(Guild guild, List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String channelId : channelIds) {
            if (channelId == null || channelId.isBlank()) {
                continue;
            }

            long parsedChannelId;
            try {
                parsedChannelId = Long.parseLong(channelId.trim());
            } catch (NumberFormatException exception) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                        "One of the supplied channel IDs was not a valid Discord snowflake.");
            }

            StandardGuildChannel channel = guild.getChannelById(StandardGuildChannel.class, parsedChannelId);
            if (channel == null || channel.getType() == ChannelType.CATEGORY || channel.getType().isThread()) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_opt_in_channel",
                        "One of the supplied channels was not a valid opt-in channel in this guild.");
            }

            sanitized.add(Long.toString(parsedChannelId));
        }

        return new ArrayList<>(sanitized);
    }

    private List<String> toChannelIds(Guild guild, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return GuildData.getLongs(value).stream()
                .filter(channelId -> {
                    if (guild == null) {
                        return true;
                    }

                    StandardGuildChannel channel = guild.getChannelById(StandardGuildChannel.class, channelId);
                    return channel != null && channel.getType() != ChannelType.CATEGORY && !channel.getType().isThread();
                })
                .map(String::valueOf)
                .toList();
    }

    private static String joinDelimited(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join(";", values);
    }
}
