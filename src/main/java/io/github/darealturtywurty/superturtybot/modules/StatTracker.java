package io.github.darealturtywurty.superturtybot.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StatTracker extends ListenerAdapter {
    private static final Map<Long, GuildStats> GUILD_MEMBER_MAP = new HashMap<>();
    public static final StatTracker INSTANCE = new StatTracker();
    
    private StatTracker() {
    }
    
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        final var stats = new GuildStats();
        event.getGuild().loadMembers().onSuccess(members -> {
            stats.members = members.size();
            GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
        });
    }
    
    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        GUILD_MEMBER_MAP.remove(event.getGuild().getIdLong());
    }
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.members++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        
        stats.members--;
        if (stats.members < 1) {
            GUILD_MEMBER_MAP.remove(event.getGuild().getIdLong());
            return;
        }
        
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        final var stats = new GuildStats();
        event.getGuild().loadMembers().onSuccess(members -> {
            stats.members = members.size();
            GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
        });
    }
    
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild())
            return;
        
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.messagesDeleted++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild())
            return;
        
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.reactionsAdded++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (!event.isFromGuild())
            return;
        
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.reactionsRemoved++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;
        
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.messagesSent++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild())
            return;
        
        final var stats = GUILD_MEMBER_MAP.containsKey(event.getGuild().getIdLong())
            ? GUILD_MEMBER_MAP.get(event.getGuild().getIdLong())
            : new GuildStats();
        stats.messagesEdited++;
        GUILD_MEMBER_MAP.put(event.getGuild().getIdLong(), stats);
    }
    
    public static void messageCtxCommandRan(Guild guild) {
        final var stats = GUILD_MEMBER_MAP.containsKey(guild.getIdLong()) ? GUILD_MEMBER_MAP.get(guild.getIdLong())
            : new GuildStats();
        stats.messageCtxCommandsRan++;
        GUILD_MEMBER_MAP.put(guild.getIdLong(), stats);
    }
    
    public static void normalCommandRan(Guild guild) {
        final var stats = GUILD_MEMBER_MAP.containsKey(guild.getIdLong()) ? GUILD_MEMBER_MAP.get(guild.getIdLong())
            : new GuildStats();
        stats.normalCommandsRan++;
        GUILD_MEMBER_MAP.put(guild.getIdLong(), stats);
    }
    
    public static void slashCommandRan(Guild guild) {
        final var stats = GUILD_MEMBER_MAP.containsKey(guild.getIdLong()) ? GUILD_MEMBER_MAP.get(guild.getIdLong())
            : new GuildStats();
        stats.slashCommandsRan++;
        GUILD_MEMBER_MAP.put(guild.getIdLong(), stats);
    }
    
    public static void userCtxCommandRan(Guild guild) {
        final var stats = GUILD_MEMBER_MAP.containsKey(guild.getIdLong()) ? GUILD_MEMBER_MAP.get(guild.getIdLong())
            : new GuildStats();
        stats.userCtxCommandsRan++;
        GUILD_MEMBER_MAP.put(guild.getIdLong(), stats);
    }
    
    public static class DataRetriever {
        public static int getGuildCount() {
            return GUILD_MEMBER_MAP.size();
        }
        
        public static Set<Long> getGuildIDs() {
            return Set.copyOf(GUILD_MEMBER_MAP.keySet());
        }
        
        @Nullable
        public static GuildStats getGuildStats(Guild guild) {
            return getGuildStats(guild.getIdLong());
        }
        
        @Nullable
        public static GuildStats getGuildStats(long id) {
            return GUILD_MEMBER_MAP.containsKey(id) ? GUILD_MEMBER_MAP.get(id) : null;
        }
        
        public static long getMemberCount() {
            final var adder = new LongAdder();
            GUILD_MEMBER_MAP.values().parallelStream().map(stats -> stats.members).forEach(adder::add);
            return adder.longValue();
        }
        
        public static long tallyStats(Integer... stats) {
            final var adder = new LongAdder();
            Stream.of(stats).parallel().forEach(adder::add);
            return adder.longValue();
        }
    }
    
    public static class GuildStats {
        private int members, messagesSent, messagesEdited, messagesDeleted, reactionsAdded, reactionsRemoved,
            slashCommandsRan, normalCommandsRan, messageCtxCommandsRan, userCtxCommandsRan;
        
        public int getMembers() {
            return this.members;
        }
        
        public int getMessageCtxCommandsRan() {
            return this.messageCtxCommandsRan;
        }
        
        public int getMessagesDeleted() {
            return this.messagesDeleted;
        }
        
        public int getMessagesEdited() {
            return this.messagesEdited;
        }
        
        public int getMessagesSent() {
            return this.messagesSent;
        }
        
        public int getNormalCommandsRan() {
            return this.normalCommandsRan;
        }
        
        public int getReactionsAdded() {
            return this.reactionsAdded;
        }
        
        public int getReactionsRemoved() {
            return this.reactionsRemoved;
        }
        
        public int getSlashCommandsRan() {
            return this.slashCommandsRan;
        }
        
        public int getUserCtxCommandsRan() {
            return this.userCtxCommandsRan;
        }
    }
}
