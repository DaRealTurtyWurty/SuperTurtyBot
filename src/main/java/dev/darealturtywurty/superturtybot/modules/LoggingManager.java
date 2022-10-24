package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.GenericRoleUpdateEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GenericGuildStickerUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Optional;

public class LoggingManager extends ListenerAdapter {
    public static final LoggingManager INSTANCE = new LoggingManager();

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Channel %s was created", event.getChannel().getAsMention()).queue();
        });
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Channel %s(%d) was deleted", event.getChannel().getName(), event.getChannel().getIdLong()).queue();
        });
    }

    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Emoji %s was added", event.getEmoji().getAsMention()).queue();
        });
    }

    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Emoji %s was removed", event.getEmoji().getName()).queue();
        });
    }

    @Override
    public void onGenericChannelUpdate(GenericChannelUpdateEvent<?> event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Channel %s(%d) was updated", event.getChannel().getName(), event.getChannel().getIdLong()).queue();
        });
    }

    @Override
    public void onGenericEmojiUpdate(GenericEmojiUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Emoji %s was updated", event.getEmoji().getName()).queue();
        });
    }

    @Override
    public void onGenericForumTagUpdate(GenericForumTagUpdateEvent event) {
        Guild guild = event.getChannel().getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Forum tag %s was updated", event.getTag().getName()).queue();
        });
    }

    @Override
    public void onGenericGuildStickerUpdate(GenericGuildStickerUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Sticker %s was updated", event.getSticker().getName()).queue();
        });
    }

    @Override
    public void onGenericGuildUpdate(GenericGuildUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Guild %s was updated", guild.getName()).queue();
        });
    }

    @Override
    public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Role %s was updated", event.getRole().getName()).queue();
        });
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s was banned", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Invite %s was created", event.getInvite().getUrl()).queue();
        });
    }

    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Invite %s was deleted", event.getUrl()).queue();
        });
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s joined", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s left", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onGuildStickerAdded(GuildStickerAddedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Sticker %s was added", event.getSticker().getName()).queue();
        });
    }

    @Override
    public void onGuildStickerRemoved(GuildStickerRemovedEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Sticker %s was removed", event.getSticker().getName()).queue();
        });
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s was timed out", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("User %s was unbanned", event.getUser().getAsMention()).queue();
        });
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Bulk delete of %d messages", event.getMessageIds().size()).queue();
        });
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Message %s was deleted", event.getMessageId()).queue();
        });
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("All reactions were removed from message %s", event.getMessageId()).queue();
        });
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Message %s was updated", event.getMessageId()).queue();
        });
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Role %s was created", event.getRole().getName()).queue();
        });
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        getLogChannelAndValidate(guild, event).ifPresent(channel -> {
            channel.sendMessageFormat("Role %s was deleted", event.getRole().getName()).queue();
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
}
