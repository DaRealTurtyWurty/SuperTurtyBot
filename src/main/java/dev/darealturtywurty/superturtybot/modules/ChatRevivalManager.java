package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.util.TopicCommand;
import dev.darealturtywurty.superturtybot.commands.util.WouldYouRatherCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

// TODO: Rewrite
public class ChatRevivalManager extends ListenerAdapter {
    public static final ChatRevivalManager INSTANCE = new ChatRevivalManager();
    private static final Map<Long, Pair<ScheduledExecutorService, ScheduledFuture<?>>> GUILD_EXECUTOR_MAP = new HashMap<>();

    private ChatRevivalManager() {}

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage())
            return;

        Guild guild = event.getGuild();
        GuildConfig config = getGuildConfig(guild);
        if (!config.isChatRevivalEnabled())
            return;

        Pair<ScheduledExecutorService, ScheduledFuture<?>> executor = GUILD_EXECUTOR_MAP.computeIfAbsent(guild.getIdLong(), id -> new Pair<>(Executors.newSingleThreadScheduledExecutor(), null));
        if (executor.getSecond() != null)
            executor.getSecond().cancel(true);

        GUILD_EXECUTOR_MAP.put(guild.getIdLong(), new Pair<>(executor.getFirst(), executor.getFirst().scheduleAtFixedRate(() -> {
            TextChannel channel = guild.getTextChannelById(config.getChatRevivalChannel());
            if (channel == null) {
                Pair<ScheduledExecutorService, ScheduledFuture<?>> executorAndFuture = GUILD_EXECUTOR_MAP.remove(guild.getIdLong());
                if (executorAndFuture != null) {
                    executorAndFuture.getSecond().cancel(true);
                    executorAndFuture.getFirst().shutdown();
                }

                return;
            }

            channel.getHistory().retrievePast(100).queue(msgs -> {
                List<Message> messages = msgs.stream().sorted((m1, m2) -> Long.compare(m2.getTimeCreated().toInstant().toEpochMilli(), m1.getTimeCreated().toInstant().toEpochMilli())).toList();
                if(messages.isEmpty()) {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        channel.sendMessage(TopicCommand.getRandomTopic()).queue();
                    } else {
                        channel.sendMessage(WouldYouRatherCommand.getRandomQuestion()).queue(msg -> {
                            msg.addReaction(Emoji.fromUnicode("U+1F170")).queue();
                            msg.addReaction(Emoji.fromUnicode("U+1F171")).queue();
                        });
                    }

                    return;
                }

                for (Message message : messages) {
                    if(message.getAuthor().isBot() || message.getAuthor().isSystem() || message.isWebhookMessage())
                        continue;

                    long timeAgo = System.currentTimeMillis() - message.getTimeCreated().toInstant().toEpochMilli();
                    if (timeAgo < TimeUnit.SECONDS.toMillis(config.getChatRevivalTime()))
                        break;

                    if (ThreadLocalRandom.current().nextBoolean()) {
                        channel.sendMessage(TopicCommand.getRandomTopic()).queue();
                    } else {
                        channel.sendMessage(WouldYouRatherCommand.getRandomQuestion()).queue(msg -> {
                            msg.addReaction(Emoji.fromUnicode("U+1F170")).queue();
                            msg.addReaction(Emoji.fromUnicode("U+1F171")).queue();
                        });
                    }

                    break;
                }
            });
        }, 0, config.getChatRevivalTime(), TimeUnit.SECONDS)));
    }

    private static GuildConfig getGuildConfig(Guild guild) {
        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        return config;
    }
}
