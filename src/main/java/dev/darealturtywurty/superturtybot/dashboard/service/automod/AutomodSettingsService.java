package dev.darealturtywurty.superturtybot.dashboard.service.automod;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class AutomodSettingsService {
    private final JDA jda;

    public AutomodSettingsService(JDA jda) {
        this.jda = jda;
    }

    public AutomodSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public AutomodSettingsResponse updateSettings(long guildId, AutomodSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(guild, request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setDiscordInviteGuardEnabled(request.isInviteGuardEnabled());
        guildData.setDiscordInviteWhitelistChannels(String.join(" ", normalizeChannelIds(request.getInviteGuardWhitelistChannelIds())));
        guildData.setScamDetectionEnabled(request.isScamDetectionEnabled());
        guildData.setImageSpamAutoBanEnabled(request.isImageSpamAutoBanEnabled());
        guildData.setImageSpamWindowSeconds(request.getImageSpamWindowSeconds());
        guildData.setImageSpamMinImages(request.getImageSpamMinImages());
        guildData.setImageSpamNewMemberThresholdHours(request.getImageSpamNewMemberThresholdHours());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static AutomodSettingsResponse toResponse(GuildData guildData) {
        return new AutomodSettingsResponse(
                guildData.isDiscordInviteGuardEnabled(),
                GuildData.getLongs(guildData.getDiscordInviteWhitelistChannels()).stream().map(String::valueOf).toList(),
                guildData.isScamDetectionEnabled(),
                guildData.isImageSpamAutoBanEnabled(),
                guildData.getImageSpamWindowSeconds(),
                guildData.getImageSpamMinImages(),
                guildData.getImageSpamNewMemberThresholdHours()
        );
    }

    private static void validateRequest(Guild guild, AutomodSettingsRequest request) {
        if (request.getImageSpamWindowSeconds() < 1) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_image_spam_window",
                    "Image spam window seconds must be at least 1.");
        }

        if (request.getImageSpamMinImages() < 1) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_image_spam_min_images",
                    "Image spam minimum images must be at least 1.");
        }

        if (request.getImageSpamNewMemberThresholdHours() < 1) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_image_spam_threshold",
                    "Image spam new member threshold hours must be at least 1.");
        }

        List<String> whitelistChannelIds = normalizeChannelIds(request.getInviteGuardWhitelistChannelIds());
        if (request.isInviteGuardEnabled() && whitelistChannelIds.isEmpty()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_invite_guard_channels",
                    "Invite guard requires at least one text channel while it is enabled.");
        }

        for (String channelId : whitelistChannelIds) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_invite_guard_channel",
                        "One or more invite guard channels were not valid text channels in this guild.");
            }
        }
    }

    private static List<String> normalizeChannelIds(List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(new LinkedHashSet<>(channelIds.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }
}
