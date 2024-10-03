package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.forum.update.GenericForumTagUpdateEvent;
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.GenericEmojiUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.GenericRoleUpdateEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GenericGuildStickerUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

public class LoggingManager extends ListenerAdapter {
    public static final LoggingManager INSTANCE = new LoggingManager();

    private static final Map<Long, List<MessageData>> MESSAGE_CACHE = new HashMap<>();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;
        if (event.getAuthor().isSystem()) return;

        final Guild guild = event.getGuild();

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) return;
        if (!config.isLogMessageDelete() || !config.isLogMessageUpdate()) return;

        final long channelId = event.getChannel().getIdLong();
        final long messageId = event.getMessageIdLong();
        final long authorId = event.getAuthor().getIdLong();
        final String message = event.getMessage().getContentRaw();

        MESSAGE_CACHE.computeIfAbsent(channelId, k -> new ArrayList<>()).add(new MessageData(messageId, authorId, message));
        if (MESSAGE_CACHE.get(channelId).size() > 100) MESSAGE_CACHE.get(channelId).removeFirst();
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Channel Created", null, guild.getIconUrl());
            builder.addField("Channel", event.getChannel().getAsMention(), true);
            builder.addField("Created By", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String channelName = event.getChannel().getName();
            String channelType = event.getChannel().getType().name();
            String timeCreated = TimeFormat.DATE_TIME_LONG.format(event.getChannel().getTimeCreated());

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Channel Deleted", null, guild.getIconUrl());
            builder.addField("Channel", channelName, true);
            builder.addField("Channel Type", channelType, true);
            builder.addField("Time Created", timeCreated, true);
            builder.addField("Deleted By", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.EMOJI_CREATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String emoji = event.getEmoji().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Emoji Created", null, guild.getIconUrl());
            builder.addField("Emoji", emoji, true);
            builder.addField("Created By", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.EMOJI_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String emojiName = event.getEmoji().getName();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Emoji Deleted", null, guild.getIconUrl());
            builder.addField("Emoji", emojiName, true);
            builder.addField("Deleted By", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGenericChannelUpdate(GenericChannelUpdateEvent<?> event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String change = event.getPropertyIdentifier() + ": " + event.getOldValue() + " -> " + event.getNewValue();
            String channelName = event.getChannel().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Channel Updated", null, guild.getIconUrl());
            builder.addField("Channel", channelName, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Change", change, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGenericEmojiUpdate(GenericEmojiUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.EMOJI_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String change = event.getPropertyIdentifier() + ": " + event.getOldValue() + " -> " + event.getNewValue();
            String emoji = event.getEmoji().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Emoji Updated", null, guild.getIconUrl());
            builder.addField("Emoji", emoji, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Change", change, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGenericForumTagUpdate(GenericForumTagUpdateEvent event) {
        Guild guild = event.getChannel().getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            String tag = event.getTag().getName();
            String property = event.getPropertyIdentifier();
            Object oldVal = event.getOldValue();
            Object newVal = event.getNewValue();
            String oldValStr = oldVal == null ? "" : oldVal.toString();
            String newValStr = newVal == null ? "" : newVal.toString();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Forum Tag Updated", null, guild.getIconUrl());
            builder.addField("Tag", tag, true);
            builder.addField("Property", property, true);
            builder.addField("Old Value", oldValStr, true);
            builder.addField("New Value", newValStr, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        });
    }

    @Override
    public void onGenericGuildStickerUpdate(GenericGuildStickerUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.STICKER_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String change = event.getPropertyIdentifier() + ": " + event.getOldValue() + " -> " + event.getNewValue();
            String sticker = event.getSticker().getName();
            String url = event.getSticker().getIconUrl();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Sticker Updated", null, guild.getIconUrl());
            builder.addField("Sticker", sticker, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Change", change, false);
            builder.setThumbnail(url);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGenericGuildUpdate(GenericGuildUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String change = event.getPropertyIdentifier() + ": " + event.getOldValue() + " -> " + event.getNewValue();
            String guildName = guild.getName();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Guild Updated", null, guild.getIconUrl());
            builder.addField("Guild", guildName, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Change", change, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String change = event.getPropertyIdentifier() + ": " + event.getOldValue() + " -> " + event.getNewValue();
            String role = event.getRole().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Role Updated", null, guild.getIconUrl());
            builder.addField("Role", role, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Change", change, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.BAN).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
            String banner = event.getUser().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Member Banned", null, guild.getIconUrl());
            builder.addField("Banned Member", userName, true);
            builder.addField("Banned By", banner, true);
            builder.addField("Reason", reason, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.INVITE_CREATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String url = event.getUrl();
            String channelName = event.getChannel().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Invite Created", null, guild.getIconUrl());
            builder.addField("Invite", url, true);
            builder.addField("Created By", userName, true);
            builder.addField("Channel", channelName, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.INVITE_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String url = event.getUrl();
            String channelName = event.getChannel().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Invite Deleted", null, guild.getIconUrl());
            builder.addField("Invite", url, true);
            builder.addField("Deleted By", userName, true);
            builder.addField("Channel", channelName, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            String userName = event.getUser().getAsMention();
            String memberCount = String.valueOf(guild.getMemberCount());

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Member Joined", null, guild.getIconUrl());
            builder.addField("Member", userName, true);
            builder.addField("Member Number", memberCount, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        });
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            String userName = event.getUser().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Member Left", null, guild.getIconUrl());
            builder.addField("Member", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        });
    }

    @Override
    public void onGuildStickerAdded(GuildStickerAddedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            GuildSticker sticker = event.getSticker();
            String name = sticker.getName();
            String description = sticker.getDescription();
            String formatName = sticker.getFormatType().name();
            String formatExtension = sticker.getFormatType().getExtension();
            String format = formatName + "(" + formatExtension + ")";
            User user = sticker.retrieveOwner().complete();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String url = sticker.getIconUrl();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Sticker Added", null, guild.getIconUrl());
            builder.addField("Sticker", name, true);
            builder.addField("Description", description, true);
            builder.addField("Format", format, true);
            builder.addField("Added By", userName, true);
            builder.setThumbnail(url);
            channel.sendMessageEmbeds(builder.build()).queue();
        });
    }

    @Override
    public void onGuildStickerRemoved(GuildStickerRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.STICKER_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String name = event.getSticker().getName();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Sticker Removed", null, guild.getIconUrl());
            builder.addField("Sticker", name, true);
            builder.addField("Removed By", userName, true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
            TemporalAccessor timeOutEnd = event.getNewTimeOutEnd() == null ? Instant.now() : event.getNewTimeOutEnd();
            String time = TimeFormat.RELATIVE.format(timeOutEnd);
            String timeoutedUserName = event.getUser().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Member Timeout Updated", null, guild.getIconUrl());
            builder.addField("Member", timeoutedUserName, true);
            builder.addField("Updated By", userName, true);
            builder.addField("Timeout End", time, true);
            builder.addField("Reason", reason, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.UNBAN).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
            String unbannedUserName = event.getUser().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Member Unbanned", null, guild.getIconUrl());
            builder.addField("Member", unbannedUserName, true);
            builder.addField("Unbanned By", userName, true);
            builder.addField("Reason", reason, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.MESSAGE_BULK_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();
            int size = event.getMessageIds().size();
            String channelName = event.getChannel().getAsMention();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Bulk Message Delete", null, guild.getIconUrl());
            builder.addField("Channel", channelName, true);
            builder.addField("Deleted By", userName, true);
            builder.addField("Number of Messages", String.valueOf(size), true);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            List<MessageData> messages = MESSAGE_CACHE.get(event.getChannel().getIdLong());
            long messageId = event.getMessageIdLong();
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Message Deleted", null, guild.getIconUrl());
            builder.addField("Message ID", String.valueOf(messageId), true);
            builder.addField("Channel", event.getChannel().getAsMention(), true);

            if (messages == null || messages.isEmpty()) {
                channel.sendMessageEmbeds(builder.build()).queue();
                return;
            }

            Optional<MessageData> optional = messages.stream().filter(data -> data.getId() == event.getMessageIdLong()).findFirst();
            if (optional.isEmpty()) {
                channel.sendMessageEmbeds(builder.build()).queue();
                return;
            }

            MessageData data = optional.get();
            String content = data.getContent();
            User author = event.getJDA().getUserById(data.getAuthor());
            String authorName = author == null ? "Unknown" : author.getAsMention();
            String truncated = content.substring(0, Math.min(content.length(), 512)) + (content.length() > 512 ? "..." : "");
            builder.addField("Author", authorName, true);
            builder.addField("Content", truncated.isBlank() ? "`" + truncated + "`" : truncated, false);
            channel.sendMessageEmbeds(builder.build()).queue();

            messages.remove(data);
        });
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            String channelName = event.getChannel().getAsMention();
            String messageId = event.getMessageId();
            String jumpUrl = event.getJumpUrl();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("All Reactions Removed", null, guild.getIconUrl());
            builder.addField("Channel", channelName, true);
            builder.addField("Message ID", messageId, true);
            builder.addField("Jump URL", jumpUrl, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        });
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            List<MessageData> messages = MESSAGE_CACHE.computeIfAbsent(event.getChannel().getIdLong(), id -> new ArrayList<>());
            Optional<MessageData> optional = messages.stream().filter(data -> data.getId() == event.getMessageIdLong()).findFirst();

            long messageId = event.getMessageIdLong();
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFFFF00);
            builder.setAuthor("Message Edited", null, guild.getIconUrl());
            builder.addField("Message ID", String.valueOf(messageId), true);
            builder.addField("Channel", event.getChannel().getAsMention(), true);

            if (optional.isEmpty()) {
                channel.sendMessageEmbeds(builder.build()).queue();
                messages.add(new MessageData(event.getMessageIdLong(), event.getAuthor().getIdLong(), event.getMessage().getContentRaw()));
                return;
            }

            MessageData data = optional.get();
            String content = data.getContent();
            User author = event.getJDA().getUserById(data.getAuthor());
            String authorName = author == null ? "Unknown" : author.getAsMention();
            String truncatedOld = content.substring(0, Math.min(content.length(), 512)) + (content.length() > 512 ? "..." : "");
            String truncatedNew = event.getMessage().getContentRaw().substring(0, Math.min(event.getMessage().getContentRaw().length(), 512)) + (event.getMessage().getContentRaw().length() > 512 ? "..." : "");
            builder.addField("Author", authorName, true);
            builder.addField("Old Content", truncatedOld.isBlank() ? "`" + truncatedOld + "`" : truncatedOld, false);
            builder.addField("New Content", truncatedNew.isBlank() ? "`" + truncatedNew + "`" : truncatedNew, false);
            channel.sendMessageEmbeds(builder.build()).queue();

            data.setContent(event.getMessage().getContentRaw());
            messages.remove(data);
            messages.add(data);
        });
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.ROLE_CREATE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();

            Role role = event.getRole();
            String roleName = role.getAsMention();
            int color = role.getColorRaw();
            String hex = String.format("#%06x", color);
            String hoisted = role.isHoisted() ? "Yes" : "No";
            String mentionable = role.isMentionable() ? "Yes" : "No";
            String position = "#" + role.getPosition() + 1;
            String mention = role.getAsMention();
            String permissions = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0x00FF00);
            builder.setAuthor("Role Created", null, guild.getIconUrl());
            builder.addField("Name", roleName, true);
            builder.addField("Color", hex, true);
            builder.addField("Hoisted", hoisted, true);
            builder.addField("Mentionable", mentionable, true);
            builder.addField("Position", position, true);
            builder.addField("Created By", userName, true);
            builder.addField("Mention", mention, true);
            builder.addField("Permissions", permissions, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> guild.retrieveAuditLogs().type(ActionType.ROLE_DELETE).queue(entries -> {
            if (entries.isEmpty()) return;

            AuditLogEntry entry = entries.getFirst();

            User user = entry.getUser();
            String userName = user == null ? "Unknown" : user.getAsMention();

            Role role = event.getRole();
            String roleName = role.getName();
            int color = role.getColorRaw();
            String hex = String.format("#%06x", color);
            String hoisted = role.isHoisted() ? "Yes" : "No";
            String mentionable = role.isMentionable() ? "Yes" : "No";
            String position = "#" + role.getPosition() + 1;
            String permissions = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(0xFF0000);
            builder.setAuthor("Role Deleted", null, guild.getIconUrl());
            builder.addField("Name", roleName, true);
            builder.addField("Color", hex, true);
            builder.addField("Hoisted", hoisted, true);
            builder.addField("Mentionable", mentionable, true);
            builder.addField("Position", position, true);
            builder.addField("Deleted By", userName, true);
            builder.addField("Permissions", permissions, false);
            channel.sendMessageEmbeds(builder.build()).queue();
        }));
    }

    private static Optional<TextChannel> getLogChannelAndValidate(Guild guild, Event event) {
        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) return Optional.empty();

        TextChannel channel = guild.getTextChannelById(config.getLoggingChannel());
        if (channel == null) return Optional.empty();

        if (!config.shouldLog(event)) return Optional.empty();

        return Optional.of(channel);
    }

    @Getter
    private static class MessageData {
        private final long id;
        private final long author;
        private String content;
        @Setter
        private long lastEdited;

        public MessageData(long id, long author, String content) {
            this.id = id;
            this.author = author;
            this.content = content;
            this.lastEdited = System.currentTimeMillis();
        }

        public void setContent(String content) {
            this.content = content;
            setLastEdited(System.currentTimeMillis());
        }
    }
}
