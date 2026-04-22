package dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers;

import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.DashboardGuildInfo;
import dev.darealturtywurty.superturtybot.database.pojos.VoiceChannelNotifier;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.VoiceChannelNotifierManager;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class VoiceChannelNotifierDashboardService {
    private final JDA jda;

    public VoiceChannelNotifierDashboardService(JDA jda) {
        this.jda = jda;
    }

    public DashboardVoiceChannelNotifierResponse getSettings(long guildId) {
        Guild guild = requireGuild(guildId);
        DashboardGuildInfo guildInfo = createGuildInfo(guild);
        List<DashboardVoiceChannelNotifierEntry> notifiers = listNotifiers(guild);
        return new DashboardVoiceChannelNotifierResponse(guildInfo, notifiers);
    }

    public DashboardVoiceChannelNotifierResponse upsertNotifier(long guildId, VoiceChannelNotifierUpsertRequest request) {
        Guild guild = requireGuild(guildId);
        VoiceChannel voiceChannel = requireVoiceChannel(guild, request.voiceChannelId());
        MessageChannel textChannel = requireMessageChannel(guild, request.sendToChannelId());

        List<Long> mentionRoles = request.mentionRoleIds().stream()
                .map(roleIdStr -> {
                    try {
                        long roleId = Long.parseLong(roleIdStr.trim());
                        if (roleId <= 0L)
                            throw new NumberFormatException("Role ID must be positive.");

                        return roleId;
                    } catch (NumberFormatException exception) {
                        throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_mention_role_id",
                                "One or more mention role IDs were not valid Discord snowflakes.");
                    }
                })
                .peek(roleId -> {
                    if (guild.getRoleById(roleId) == null)
                        throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_mention_role_id",
                                "One or more mention role IDs do not exist in this guild.");
                })
                .toList();

        String message = normalizeContent(request.message());
        if (message.trim().isBlank())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_message",
                    "Notifier message cannot be blank.");

        if (message.length() > 2000)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_message",
                    "Notifier message must be 2000 characters or fewer.");

        if (message.contains("{mentions}") && mentionRoles.isEmpty())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_message",
                    "Notifier message cannot contain the {mentions} placeholder if no mention roles are configured.");

        var notifier = new VoiceChannelNotifier(
                voiceChannel.getIdLong(),
                textChannel.getIdLong(),
                mentionRoles,
                message,
                request.enabled(),
                request.announcePerJoin(),
                request.notifyLeaves(),
                request.cooldownMs()
        );

        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        guildData.getVoiceChannelNotifiers().put(voiceChannel.getId(), notifier);
        VoiceChannelNotifierManager.INSTANCE.saveGuildNotifiers(guildData);
        return getSettings(guildId);
    }

    public DashboardVoiceChannelNotifierResponse deleteNotifier(long guildId, String voiceChannelId) {
        Guild guild = requireGuild(guildId);
        VoiceChannel voiceChannel = requireVoiceChannel(guild, voiceChannelId);

        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        if (guildData.getVoiceChannelNotifiers().remove(voiceChannel.getId()) == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "notifier_not_found",
                    "No notifier was configured for that voice channel.");

        VoiceChannelNotifierManager.INSTANCE.saveGuildNotifiers(guildData);
        return getSettings(guildId);
    }

    private List<DashboardVoiceChannelNotifierEntry> listNotifiers(Guild guild) {
        return GuildData.getOrCreateGuildData(guild).getVoiceChannelNotifiers().values().stream()
                .map(notifier -> toEntry(guild, notifier))
                .sorted(Comparator
                        .comparing(DashboardVoiceChannelNotifierEntry::enabled).reversed()
                        .thenComparing(DashboardVoiceChannelNotifierEntry::voiceChannelName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DashboardVoiceChannelNotifierEntry::sendToChannelName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DashboardVoiceChannelNotifierEntry::voiceChannelId)
                        .thenComparing(DashboardVoiceChannelNotifierEntry::sendToChannelId))
                .toList();
    }

    private static @NotNull DashboardVoiceChannelNotifierEntry toEntry(Guild guild, VoiceChannelNotifier notifier) {
        VoiceChannel voiceChannel = guild.getVoiceChannelById(notifier.getVoiceChannelId());
        String voiceChannelName = voiceChannel != null ? voiceChannel.getName() : "Unknown Channel";

        TextChannel textChannel = guild.getTextChannelById(notifier.getSendToChannelId());
        String sendToChannelName = textChannel != null ? textChannel.getName() : "Unknown Channel";

        return new DashboardVoiceChannelNotifierEntry(
                String.valueOf(notifier.getVoiceChannelId()),
                voiceChannelName,
                String.valueOf(notifier.getSendToChannelId()),
                sendToChannelName,
                notifier.getMentionRoles().stream().map(String::valueOf).toList(),
                notifier.getMessage(),
                notifier.isEnabled(),
                notifier.isAnnouncePerJoin(),
                notifier.isNotifyLeaves(),
                notifier.getCooldownMs()
        );
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");

        return guild;
    }

    private DashboardGuildInfo createGuildInfo(Guild guild) {
        return new DashboardGuildInfo(
                guild.getId(),
                guild.getName(),
                guild.getIconUrl(),
                guild.getMemberCount(),
                true
        );
    }

    private static MessageChannel requireMessageChannel(Guild guild, String channelId) {
        long parsedChannelId = parseChannelId(channelId);
        GuildChannel channel = guild.getGuildChannelById(parsedChannelId);
        if (!(channel instanceof MessageChannel messageChannel))
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_channel",
                    "The supplied channel was not a valid message channel in this guild.");

        return messageChannel;
    }

    private static VoiceChannel requireVoiceChannel(Guild guild, String channelId) {
        long parsedChannelId = parseChannelId(channelId);
        GuildChannel channel = guild.getGuildChannelById(parsedChannelId);
        if (!(channel instanceof VoiceChannel voiceChannel))
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_voice_channel",
                    "The supplied channel was not a valid voice channel in this guild.");

        return voiceChannel;
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_channel",
                    "A valid channel ID is required.");

        try {
            long parsed = Long.parseLong(channelId.trim());
            if (parsed <= 0L)
                throw new NumberFormatException("Channel ID must be positive.");

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_notifier_channel",
                    "The supplied channel ID was not a valid Discord snowflake.");
        }
    }

    private static String normalizeContent(String content) {
        return content == null ? "" : content;
    }
}
