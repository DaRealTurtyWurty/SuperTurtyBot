package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import lombok.Getter;
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

        if(!event.isFromGuild()) return;
        if(event.getAuthor().isBot()) return;
        if(event.getAuthor().isSystem()) return;

        final Guild guild = event.getGuild();

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if(config == null) return;
        if(!config.isLogMessageDelete() || !config.isLogMessageUpdate()) return;

        final long channelId = event.getChannel().getIdLong();
        final long messageId = event.getMessageIdLong();
        final long authorId = event.getAuthor().getIdLong();
        final String message = event.getMessage().getContentRaw();

        MESSAGE_CACHE.computeIfAbsent(channelId, k -> new ArrayList<>()).add(new MessageData(messageId, authorId, message));
        if(MESSAGE_CACHE.get(channelId).size() > 100) MESSAGE_CACHE.get(channelId).remove(0);
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue(entries -> {
                User user = entries.get(0).getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Channel %s was created by %s!", event.getChannel().getAsMention(), userName).queue();
            });
        });
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).queue(entries -> {
                User user = entries.get(0).getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Channel %s was deleted by %s!", event.getChannel().getName(), userName).queue();
            });
        });
    }

    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.EMOJI_CREATE).queue(entries -> {
                User user = entries.get(0).getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Emoji %s was added by %s!", event.getEmoji().getAsMention(), userName).queue();
            });
        });
    }

    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.EMOJI_DELETE).queue(entries -> {
                User user = entries.get(0).getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Emoji %s was removed by %s!", event.getEmoji().getAsMention(), userName).queue();
            });
        });
    }

    @Override
    public void onGenericChannelUpdate(GenericChannelUpdateEvent<?> event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                StringBuilder changes = new StringBuilder();
                entry.getChanges().values().forEach(change -> changes.append(change.toString()).append("\n"));
                channel.sendMessageFormat("Channel %s was updated by %s! Changes:\n%s", event.getChannel().getName(), userName, changes).queue();
            });
        });
    }

    @Override
    public void onGenericEmojiUpdate(GenericEmojiUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.EMOJI_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                StringBuilder changes = new StringBuilder();
                entry.getChanges().values().forEach(change -> changes.append(change.toString()).append("\n"));
                channel.sendMessageFormat("Emoji %s was updated by %s! Changes:\n%s", event.getEmoji().getName(), userName, changes).queue();
            });
        });
    }

    @Override
    public void onGenericForumTagUpdate(GenericForumTagUpdateEvent event) {
        Guild guild = event.getChannel().getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(
                channel -> channel.sendMessageFormat("Forum tag `%s` had the property `%s` changed from `%s` to `%s`!",
                        event.getTag().getName(),
                        event.getPropertyIdentifier(),
                        event.getOldValue(),
                        event.getNewValue()
                ).queue());
    }

    @Override
    public void onGenericGuildStickerUpdate(GenericGuildStickerUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.STICKER_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                StringBuilder changes = new StringBuilder();
                entry.getChanges().values().forEach(change -> changes.append(change.toString()).append("\n"));
                channel.sendMessageFormat("Sticker %s was updated by %s! Changes:\n%s", event.getSticker().getName(), userName, changes).queue();
            });
        });
    }

    @Override
    public void onGenericGuildUpdate(GenericGuildUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                StringBuilder changes = new StringBuilder();
                entry.getChanges().values().forEach(change -> changes.append(change.toString()).append("\n"));
                channel.sendMessageFormat("Guild %s was updated by %s! Changes:\n%s", event.getGuild().getName(), userName, changes).queue();
            });
        });
    }

    @Override
    public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                StringBuilder changes = new StringBuilder();
                entry.getChanges().values().forEach(change -> changes.append(change.toString()).append("\n"));
                channel.sendMessageFormat("Role %s was updated by %s! Changes:\n%s", event.getRole().getName(), userName, changes).queue();
            });
        });
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.BAN).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
                channel.sendMessageFormat("User %s was banned by %s for reason:\n%s", event.getUser().getAsMention(), userName, reason).queue();
            });
        });
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.INVITE_CREATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Invite %s was created by %s!", event.getUrl(), userName).queue();
            });
        });
    }

    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.INVITE_DELETE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Invite %s was deleted by %s!", event.getUrl(), userName).queue();
            });
        });
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s joined!", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s left!", event.getUser().getAsMention()).queue();
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

            channel.sendMessageFormat("Sticker %s was added by %s!\n Description: %s\nFormat: %s",
                    name,
                    user.getAsMention(),
                    description,
                    format
            ).queue();
        });
    }

    @Override
    public void onGuildStickerRemoved(GuildStickerRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.STICKER_DELETE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("Sticker %s was removed by %s!", event.getSticker().getName(), userName).queue();
            });
        });
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
                TemporalAccessor timeOutEnd = event.getNewTimeOutEnd() == null ? Instant.now() : event.getNewTimeOutEnd();
                String time = TimeFormat.RELATIVE.format(timeOutEnd);
                channel.sendMessageFormat("User %s's timeout has been updated by %s for reason:\n%s\nNew timeout end: %s", event.getUser().getAsMention(), userName, reason, time).queue();
            });
        });
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.UNBAN).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                String reason = entry.getReason() == null ? "No reason provided" : entry.getReason();
                channel.sendMessageFormat("User %s was unbanned by %s for reason:\n%s", event.getUser().getAsMention(), userName, reason).queue();
            });
        });
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.MESSAGE_BULK_DELETE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();
                channel.sendMessageFormat("%s messages were deleted by %s in channel %s",
                        event.getMessageIds().size(),
                        userName,
                        event.getChannel().getAsMention()
                ).queue();
            });
        });
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            List<MessageData> messages = MESSAGE_CACHE.get(event.getChannel().getIdLong());
            if (messages == null || messages.isEmpty()) {
                channel.sendMessageFormat("Message %s was deleted", event.getMessageId()).queue();
                return;
            }

            Optional<MessageData> optional = messages.stream().filter(data -> data.getId() == event.getMessageIdLong()).findFirst();
            if (optional.isEmpty()) {
                channel.sendMessageFormat("Message %s was deleted", event.getMessageId()).queue();
                return;
            }

            MessageData data = optional.get();
            String content = data.getContent();
            User author = event.getJDA().getUserById(data.getAuthor());
            String authorName = author == null ? "Unknown" : author.getAsMention();
            channel.sendMessageFormat("Message `%s` in channel %s has been deleted! Info:\nAuthor:%s\nContent:%s",
                    data.getId(),
                    event.getChannel().getAsMention(),
                    authorName,
                    content.substring(0, Math.min(content.length(), 512)) + (content.length() > 512 ? "..." : "")
            ).queue();

            messages.remove(data);
        });
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(
                channel -> channel.sendMessageFormat("All reactions were removed from message:\n%s", event.getJumpUrl()).queue());
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            List<MessageData> messages = MESSAGE_CACHE.computeIfAbsent(event.getChannel().getIdLong(), id -> new ArrayList<>());
            Optional<MessageData> optional = messages.stream().filter(data -> data.getId() == event.getMessageIdLong()).findFirst();
            if (optional.isEmpty()) {
                channel.sendMessageFormat("Message %s was edited", event.getMessageId()).queue();
                messages.add(new MessageData(event.getMessageIdLong(), event.getAuthor().getIdLong(), event.getMessage().getContentRaw()));
                return;
            }

            MessageData data = optional.get();
            String content = data.getContent();
            User author = event.getJDA().getUserById(data.getAuthor());
            String authorName = author == null ? "Unknown" : author.getAsMention();
            channel.sendMessageFormat("Message `%s` in channel %s has been edited! Info:\nAuthor:%s\nOld Content:%s\nNew Content:%s",
                    data.getId(),
                    event.getChannel().getAsMention(),
                    authorName,
                    content.substring(0, Math.min(content.length(), 512)) + (content.length() > 512 ? "..." : ""),
                    event.getMessage().getContentRaw().substring(0, Math.min(event.getMessage().getContentRaw().length(), 512)) + (event.getMessage().getContentRaw().length() > 512 ? "..." : "")
            ).queue();

            data.setContent(event.getMessage().getContentRaw());
            messages.remove(data);
            messages.add(data);
        });
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.ROLE_CREATE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();

                Role role = event.getRole();
                String roleName = role.getAsMention();
                int color = role.getColorRaw();
                String hex = String.format("#%06x", color);
                String hoisted = role.isHoisted() ? "Yes" : "No";
                String mentionable = role.isMentionable() ? "Yes" : "No";
                String permissions = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));
                String position = "#" + role.getPosition() + 1;

                channel.sendMessageFormat("Role %s was created by %s! Info:\nColor:%s\nHoisted:%s\nMentionable:%s\nPermissions:%s\nPosition:%s",
                        roleName,
                        userName,
                        hex,
                        hoisted,
                        mentionable,
                        permissions,
                        position
                ).queue();
            });
        });
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            guild.retrieveAuditLogs().type(ActionType.ROLE_DELETE).queue(entries -> {
                AuditLogEntry entry = entries.get(0);
                User user = entry.getUser();
                String userName = user == null ? "Unknown" : user.getAsMention();

                Role role = event.getRole();
                String roleName = role.getName();
                int color = role.getColorRaw();
                String hex = String.format("#%06x", color);
                String hoisted = role.isHoisted() ? "Yes" : "No";
                String mentionable = role.isMentionable() ? "Yes" : "No";
                String permissions = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));
                String position = "#" + role.getPosition() + 1;

                channel.sendMessageFormat("Role %s was deleted by %s! Info:\nColor:%s\nHoisted:%s\nMentionable:%s\nPermissions:%s\nPosition:%s",
                        roleName,
                        userName,
                        hex,
                        hoisted,
                        mentionable,
                        permissions,
                        position
                ).queue();
            });
        });
    }

    private static Optional<TextChannel> getLogChannelAndValidate(Guild guild, Event event) {
        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
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

        public void setLastEdited(long lastEdited) {
            this.lastEdited = lastEdited;
        }
    }
}
