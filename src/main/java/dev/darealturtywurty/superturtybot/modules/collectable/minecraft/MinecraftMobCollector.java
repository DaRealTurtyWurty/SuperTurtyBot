package dev.darealturtywurty.superturtybot.modules.collectable.minecraft;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.collectable.Answer;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameInstance;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinecraftMobCollector extends ListenerAdapter {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final List<CollectableGameInstance<MinecraftMobCollectable>> gameInstances = new ArrayList<>();
    private final Map<Long, Boolean> scheduledGuilds = new HashMap<>();
    private final Map<Long, List<Long>> guildMessageMap = new HashMap<>();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild() || event.isWebhookMessage())
            return;

        Guild guild = event.getGuild();
        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (data == null) {
            data = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(data);
            return;
        }

        long collectorChannel = data.getCollectorChannel();
        if (!data.isCollectingEnabled() || collectorChannel != event.getChannel().getIdLong())
            return;

        List<Long> messageTimes = guildMessageMap.computeIfAbsent(guild.getIdLong(), k -> new ArrayList<>());
        messageTimes.removeIf(time -> time < System.currentTimeMillis() - TimeUnit.HOURS.toMillis(8));
        messageTimes.add(event.getMessage().getTimeCreated().toInstant().toEpochMilli());

        handleAnswer(event, guild);
        handleScheduling(event, guild);
    }

    private void handleAnswer(MessageReceivedEvent event, Guild guild) {
        MessageChannelUnion channel = event.getChannel();
        MessageReference reference = event.getMessage().getMessageReference();
        if (reference == null || reference.getGuildIdLong() != guild.getIdLong() || reference.getChannelIdLong() != channel.getIdLong() || reference.getMessageIdLong() == 0L)
            return;

        reference.resolve().queue(message -> {
            if (message.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong())
                return;

            CollectableGameInstance<MinecraftMobCollectable> instance = gameInstances.stream().filter(gameInstance -> gameInstance.isSameMessage(guild.getIdLong(), channel.getIdLong(), message.getIdLong())).findFirst().orElse(null);
            if (instance == null)
                return;

            MinecraftMobCollectable collectable = instance.collectable();
            User user = event.getAuthor();

            UserCollectables userCollectables = Database.getDatabase().userCollectables.find(Filters.eq("user", user.getIdLong())).first();
            if (userCollectables == null) {
                userCollectables = new UserCollectables(user.getIdLong());
                Database.getDatabase().userCollectables.insertOne(userCollectables);
            }

            Answer answer = collectable.getAnswer();
            String content = event.getMessage().getContentRaw();

            UserCollectables.Collectables minecraftMobCollectables = userCollectables.getCollectables(UserCollectables.CollectionType.MINECRAFT_MOBS);
            if (minecraftMobCollectables.hasCollectable(collectable)) {
                CoreCommand.reply(event, "❌ You already have this collectable!");
                return;
            }

            if (answer.matches(content)) {
                minecraftMobCollectables.collect(collectable);
                Database.getDatabase().userCollectables.replaceOne(Filters.eq("user", user.getIdLong()), userCollectables);
                CoreCommand.reply(event, "✅ You have successfully collected a `" + collectable.getRichName() + "`!");

                gameInstances.remove(instance);
                message.editMessage("\n\n**This collectable has been collected by " + user.getAsMention() + "!**").queue();

                guildMessageMap.get(guild.getIdLong()).removeIf(time -> time < message.getTimeCreated().toInstant().toEpochMilli());
            }
        });
    }

    // this method should schedule a game instance every 2-12 hours (depending on the message density). if the density is too low, it should not schedule a game instance.
    private void handleScheduling(MessageReceivedEvent event, Guild guild) {
        if (scheduledGuilds.getOrDefault(guild.getIdLong(), false))
            return;

        if (gameInstances.stream().anyMatch(gameInstance -> gameInstance.isSameChannel(guild.getIdLong(), event.getChannel().getIdLong())))
            return;

        List<Long> messageTimes = guildMessageMap.get(guild.getIdLong());
        if (messageTimes == null || messageTimes.isEmpty())
            return;

        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - messageTimes.getFirst();
        if (timeDifference < 7200000)
            return;

        float messageDensity = messageTimes.size() / (timeDifference / 3600000f);
        messageDensity = Math.min(1, Math.max(0, messageDensity / 10));
        if (messageDensity < 0.1)
            return;

        scheduledGuilds.put(guild.getIdLong(), true);
        executor.schedule(() -> {
            scheduledGuilds.put(guild.getIdLong(), false);
            MinecraftMobCollectable collectable = MinecraftMobRegistry.getRandomWeightedMob();

            var embed = new EmbedBuilder()
                    .setTitle("🎉 A " + collectable.getRarity().getName() + " " + collectable.getEmoji() + " **" + collectable.getRichName() + "** has appeared!")
                    .setDescription("Reply to this message with the answer to the following question to collect it:\n**" + collectable.getQuestion() + "**")
                    .setTimestamp(Instant.now())
                    .setColor(collectable.getRarity().getColor())
                    .build();
            event.getChannel().sendMessageEmbeds(embed).queue(message -> {
                gameInstances.add(new CollectableGameInstance<>(guild.getIdLong(), event.getChannel().getIdLong(), message.getIdLong(), collectable));
            });
        }, (long) (2 + (1 - messageDensity) * 10), TimeUnit.HOURS);
    }
}
