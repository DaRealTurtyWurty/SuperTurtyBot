package dev.darealturtywurty.superturtybot.modules.collectable;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.object.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import dev.darealturtywurty.superturtybot.registry.Registry;
import lombok.Getter;
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

public class CollectableGameCollector<T extends Collectable> extends ListenerAdapter implements Registerable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final List<CollectableGameInstance<T>> gameInstances = new ArrayList<>();
    private final Map<Long, Boolean> scheduledGuilds = new HashMap<>();
    private final Map<Long, List<Long>> guildMessageMap = new HashMap<>();
    @Getter
    private final Registry<T> registry;
    private String name;
    @Getter
    private final String displayName;

    private boolean hasDoneInitialLoad = false;

    protected CollectableGameCollector(Registry<T> registry, String name, String displayName) {
        this.registry = registry;
        this.name = name;
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild() || event.isWebhookMessage())
            return;

        Guild guild = event.getGuild();
        GuildData data = GuildData.getOrCreateGuildData(guild);

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

            CollectableGameInstance<T> instance = gameInstances.stream().filter(gameInstance -> gameInstance.isSameMessage(guild.getIdLong(), channel.getIdLong(), message.getIdLong())).findFirst().orElse(null);
            if (instance == null)
                return;

            T collectable = instance.collectable();
            User user = event.getAuthor();

            UserCollectables userCollectables = Database.getDatabase().userCollectables.find(Filters.eq("user", user.getIdLong())).first();
            if (userCollectables == null) {
                userCollectables = new UserCollectables(user.getIdLong());
                Database.getDatabase().userCollectables.insertOne(userCollectables);
            }

            Answer answer = collectable.getAnswer();
            String content = event.getMessage().getContentRaw();

            UserCollectables.Collectables userCollectablesOfType = userCollectables.getCollectables(collectable.getCollectionType());
            if (userCollectablesOfType.hasCollectable(collectable)) {
                message.reply("❌ You have already collected " + collectable.getEmoji() + " `" + collectable.getRichName() + "`!").mentionRepliedUser(true)
                        .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            if (answer.matches(content)) {
                userCollectablesOfType.collect(collectable);
                Database.getDatabase().userCollectables.replaceOne(Filters.eq("user", user.getIdLong()), userCollectables);
                CoreCommand.reply(event, "✅ You have successfully collected " + collectable.getEmoji() + " `" + collectable.getRichName() + "`!");

                gameInstances.remove(instance);
                message.editMessage("\n\n**This collectable has been collected by " + user.getAsMention() + "!**").queue();

                guildMessageMap.get(guild.getIdLong()).removeIf(time -> time < message.getTimeCreated().toInstant().toEpochMilli());
            }
        });
    }

    public T getRandomWeightedCollectable() {
        List<T> collectables = new ArrayList<>(this.registry.getRegistry().values());
        WeightedRandomBag<T> bag = new WeightedRandomBag<>();

        for (T collectable : collectables) {
            int weight = collectable.getRarity().calculateWeight();
            bag.addEntry(collectable, weight);
        }

        return bag.getRandom();
    }

    private void handleScheduling(MessageReceivedEvent event, Guild guild) {
        if (scheduledGuilds.getOrDefault(guild.getIdLong(), false))
            return;

        if (gameInstances.stream().anyMatch(gameInstance -> gameInstance.isSameChannel(guild.getIdLong(), event.getChannel().getIdLong())))
            return;

        if (!hasDoneInitialLoad) {
            scheduledGuilds.put(guild.getIdLong(), true);
            scheduleCollectable(event, guild);

            hasDoneInitialLoad = true;
            return;
        }

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

        long delay = (long) (2 + (1 - messageDensity) * 10);
        executor.schedule(() -> {
            scheduledGuilds.put(guild.getIdLong(), false);
            scheduleCollectable(event, guild);
        }, delay, TimeUnit.HOURS);
    }

    private void scheduleCollectable(MessageReceivedEvent event, Guild guild) {
        T collectable = getRandomWeightedCollectable();
        var embed = new EmbedBuilder()
                .setTitle("🎉 A " + collectable.getRarity().getName() + " " + collectable.getEmoji() + " **" + collectable.getRichName() + "** has appeared!")
                .setDescription("Reply to this message with the answer to the following question to collect it:\n**" + collectable.getQuestion() + "**")
                .setTimestamp(Instant.now())
                .setAuthor("Part of the " + displayName + " Collection", null, event.getJDA().getSelfUser().getAvatarUrl())
                .setColor(collectable.getRarity().getColor())
                .build();
        event.getChannel().sendMessageEmbeds(embed).queue(message ->
                gameInstances.add(new CollectableGameInstance<>(
                        guild.getIdLong(), event.getChannel().getIdLong(), message.getIdLong(),
                        collectable)));
    }
}


