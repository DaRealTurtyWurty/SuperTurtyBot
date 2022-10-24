package dev.darealturtywurty.superturtybot.modules;

import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.forum.update.GenericForumTagUpdateEvent;
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.GenericEmojiUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildTimeoutEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
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

public class LoggingManager extends ListenerAdapter {
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        super.onChannelCreate(event);
    }
    
    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        super.onChannelDelete(event);
    }
    
    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
        super.onEmojiAdded(event);
    }
    
    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
        super.onEmojiRemoved(event);
    }
    
    @Override
    public void onGenericChannelUpdate(GenericChannelUpdateEvent<?> event) {
        super.onGenericChannelUpdate(event);
    }
    
    @Override
    public void onGenericEmojiUpdate(GenericEmojiUpdateEvent event) {
        super.onGenericEmojiUpdate(event);
    }
    
    @Override
    public void onGenericForumTagUpdate(GenericForumTagUpdateEvent event) {
        super.onGenericForumTagUpdate(event);
    }
    
    @Override
    public void onGenericGuildStickerUpdate(GenericGuildStickerUpdateEvent event) {
        super.onGenericGuildStickerUpdate(event);
    }
    
    @Override
    public void onGenericGuildUpdate(GenericGuildUpdateEvent event) {
        super.onGenericGuildUpdate(event);
    }
    
    @Override
    public void onGenericRoleUpdate(GenericRoleUpdateEvent event) {
        super.onGenericRoleUpdate(event);
    }
    
    @Override
    public void onGuildBan(GuildBanEvent event) {
        super.onGuildBan(event);
    }
    
    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        super.onGuildInviteCreate(event);
    }
    
    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        super.onGuildInviteDelete(event);
    }
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        super.onGuildMemberJoin(event);
    }
    
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        super.onGuildMemberRemove(event);
    }
    
    @Override
    public void onGuildStickerAdded(GuildStickerAddedEvent event) {
        super.onGuildStickerAdded(event);
    }
    
    @Override
    public void onGuildStickerRemoved(GuildStickerRemovedEvent event) {
        super.onGuildStickerRemoved(event);
    }
    
    @Override
    public void onGuildTimeout(GuildTimeoutEvent event) {
        super.onGuildTimeout(event);
    }
    
    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        super.onGuildUnban(event);
    }
    
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        super.onGuildVoiceUpdate(event);
    }
    
    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        super.onMessageBulkDelete(event);
    }
    
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        super.onMessageDelete(event);
    }
    
    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        super.onMessageReactionRemoveAll(event);
    }
    
    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        super.onMessageUpdate(event);
    }
    
    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        super.onRoleCreate(event);
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        super.onRoleDelete(event);
    }
}
