package dev.darealturtywurty.superturtybot.modules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.model.Filters;

import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ThreadManager extends ListenerAdapter {
    public static final ThreadManager INSTANCE = new ThreadManager();
    
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD)
            return;
        
        final Guild guild = event.getGuild();
        final GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
            .first();
        
        if (config == null || !config.isShouldModeratorsJoinThreads())
            return;
        
        final Set<Member> moderators = new HashSet<>();
        guild.getRoles().stream()
            .filter(
                role -> role.hasPermission(Permission.MANAGE_THREADS) || role.hasPermission(Permission.MESSAGE_MANAGE))
            .map(guild::findMembersWithRoles).forEach(task -> task.onSuccess(moderators::addAll));
        final var strBuilder = new StringBuilder();
        moderators.stream().map(Member::getAsMention).forEach(strBuilder::append);
        
        final ThreadChannel thread = event.getChannel().asThreadChannel();
        thread.sendMessage("Beans").queue(
            message -> message.editMessage(strBuilder).queueAfter(2, TimeUnit.SECONDS, msg -> msg.delete().queue()));
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isFromThread() || event.isWebhookMessage() || event.getAuthor().isBot()
            || event.getAuthor().isSystem())
            return;
        
        final Guild guild = event.getGuild();
        final GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
            .first();
        if (config == null)
            return;
        
        final List<Long> channels = GuildConfig.getChannels(config.getAutoThreadChannels());
        if (channels.isEmpty() || !channels.contains(event.getChannel().getIdLong()))
            return;
        
        final String content = event.getMessage().getContentRaw();
        event.getMessage()
            .createThreadChannel(
                content.length() > Channel.MAX_NAME_LENGTH ? content.substring(0, Channel.MAX_NAME_LENGTH) : content)
            .queue();
    }
}
