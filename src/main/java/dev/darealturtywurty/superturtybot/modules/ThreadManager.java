package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.thread.ThreadRevealedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ThreadManager extends ListenerAdapter {
    public static final ThreadManager INSTANCE = new ThreadManager();

    @Override
    public void onThreadRevealed(@NotNull ThreadRevealedEvent event) {
        final ThreadChannel thread = event.getThread();
        final Guild guild = thread.getGuild();
        setupThread(guild, thread);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor()
                .isBot() || event.getAuthor().isSystem() || event.getMessage().getContentRaw()
                .isBlank() || event.getMessage().getType().isSystem()) return;

        if(event.isFromThread() && event.getMessage().getIdLong() == event.getChannel().getIdLong()) {
            final Guild guild = event.getGuild();
            final ThreadChannel thread = event.getChannel().asThreadChannel();
            thread.addThreadMember(guild.getSelfMember()).queue(RestAction.getDefaultSuccess(),
                    throwable -> Constants.LOGGER.error("Unable to add myself to a thread!", throwable));

            setupThread(guild, thread);
            return;
        }

        if(event.isFromThread())
            return;

        final Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        final List<Long> channels = GuildData.getLongs(config.getAutoThreadChannels());
        if (channels.isEmpty() || !channels.contains(event.getChannel().getIdLong())) return;

        final String content = event.getMessage().getContentRaw();
        event.getMessage().createThreadChannel(
                        content.length() > Channel.MAX_NAME_LENGTH ? content.substring(0, Channel.MAX_NAME_LENGTH) : content)
                .queue(RestAction.getDefaultSuccess(), RestAction.getDefaultSuccess());
    }

    private void setupThread(Guild guild, ThreadChannel thread) {

        GuildData config = GuildData.getOrCreateGuildData(guild);

        if (!config.isShouldModeratorsJoinThreads())
            return;

        long ownerId = guild.getOwnerIdLong();
        thread.sendMessage("Beans").setAllowedMentions(List.of()).queue(message -> {
            var mentions = new StringBuilder("<@" + ownerId + ">");
            guild.getRoles().stream()
                    .filter(role -> role.hasPermission(Permission.MANAGE_THREADS) || role.hasPermission(Permission.MESSAGE_MANAGE))
                    .map(guild::findMembersWithRoles)
                    .forEach(task -> task.onSuccess(members -> {
                        members.stream().map(Member::getAsMention).forEach(mentions::append);

                        message.editMessage(mentions).queueAfter(2, TimeUnit.SECONDS,
                                msg -> msg.delete().queueAfter(2, TimeUnit.SECONDS,
                                        RestAction.getDefaultSuccess(),
                                        throwable -> Constants.LOGGER.error("Failed to delete message!", throwable)),
                                throwable -> Constants.LOGGER.error("Failed to send message to thread!", throwable));
                    }).onError(throwable -> Constants.LOGGER.error("Failed to get members with roles!", throwable)));
        }, throwable -> Constants.LOGGER.error("Failed to send message to thread!", throwable));
    }
}
