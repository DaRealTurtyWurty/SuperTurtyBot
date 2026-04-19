package dev.darealturtywurty.superturtybot.dashboard.service.sticky_messages;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyMessage;
import dev.darealturtywurty.superturtybot.modules.StickyMessageManager;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StickyMessagesService {
    private final JDA jda;

    public StickyMessagesService(JDA jda) {
        this.jda = jda;
    }

    public DashboardStickyMessagesResponse getSettings(long guildId) {
        Guild guild = requireGuild(guildId);
        return new DashboardStickyMessagesResponse(listStickies(guild));
    }

    public DashboardStickyMessagesResponse upsertSticky(long guildId, StickyMessagesRequest request) {
        Guild guild = requireGuild(guildId);
        GuildMessageChannel channel = requireMessageChannel(guild, request.channelId());
        String content = normalizeContent(request.content());

        if (content.trim().isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_sticky_content",
                    "Sticky content cannot be blank.");
        }

        if (content.length() > 2000) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_sticky_content",
                    "Sticky content must be 2000 characters or fewer.");
        }

        StickyMessage sticky = new StickyMessage(guildId, channel.getIdLong(), 0L, content, null);
        StickyMessage existing = StickyMessageManager.getSticky(guildId, channel.getIdLong());
        if (existing != null) {
            sticky.setOwner(existing.getOwner());
        } else {
            sticky.setOwner(0L);
        }

        StickyMessageManager.saveSticky(sticky);
        StickyMessageManager.repostSticky(guild, channel, sticky);
        return getSettings(guildId);
    }

    public DashboardStickyMessagesResponse deleteSticky(long guildId, long channelId) {
        Guild guild = requireGuild(guildId);
        if (!StickyMessageManager.clearSticky(guild, channelId)) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "sticky_message_not_found",
                    "No sticky message was configured for that channel.");
        }

        return getSettings(guildId);
    }

    private List<DashboardStickyMessageInfo> listStickies(Guild guild) {
        return Database.getDatabase().stickyMessages.find(Filters.eq("guild", guild.getIdLong()))
                .into(new ArrayList<>())
                .stream()
                .map(sticky -> toInfo(guild, sticky))
                .sorted(Comparator
                        .comparing(DashboardStickyMessageInfo::connected).reversed()
                        .thenComparing(DashboardStickyMessageInfo::channelName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DashboardStickyMessageInfo::channelId))
                .toList();
    }

    private static DashboardStickyMessageInfo toInfo(Guild guild, StickyMessage sticky) {
        GuildChannel channel = guild.getGuildChannelById(sticky.getChannel());
        Member owner = guild.getMemberById(sticky.getOwner());

        return new DashboardStickyMessageInfo(
                Long.toString(sticky.getChannel()),
                channel == null ? "Unknown Channel" : channel.getName(),
                channel != null,
                sticky.hasText() ? sticky.getContent() : "",
                sticky.hasEmbed(),
                owner == null ? "Unknown User" : owner.getEffectiveName(),
                Long.toString(sticky.getOwner()),
                sticky.getPostedMessage(),
                sticky.getUpdatedAt()
        );
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private static GuildMessageChannel requireMessageChannel(Guild guild, String channelId) {
        long parsedChannelId = parseChannelId(channelId);
        GuildChannel channel = guild.getGuildChannelById(parsedChannelId);
        if (!(channel instanceof GuildMessageChannel messageChannel)) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_sticky_channel",
                    "The supplied channel was not a valid message channel in this guild.");
        }

        return messageChannel;
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_sticky_channel",
                    "A valid channel ID is required.");
        }

        try {
            long parsed = Long.parseLong(channelId.trim());
            if (parsed <= 0L) {
                throw new NumberFormatException("Channel ID must be positive.");
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_sticky_channel",
                    "The supplied channel ID was not a valid Discord snowflake.");
        }
    }

    private static String normalizeContent(String content) {
        return content == null ? "" : content;
    }
}
