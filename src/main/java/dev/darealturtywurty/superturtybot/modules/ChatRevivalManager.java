package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ChatReviver;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// TODO: Topic and WYR
public class ChatRevivalManager extends ListenerAdapter {
    public static final ChatRevivalManager INSTANCE = new ChatRevivalManager();
    private static final Map<Long, ScheduledExecutorService> GUILD_EXECUTOR_MAP = new HashMap<>();

    private ChatRevivalManager() {
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        Guild guild = event.getGuild();
        GuildConfig config = getGuildConfig(guild);
        if (!config.isChatRevivalEnabled())
            return;

        long chatRevivalChannel = config.getChatRevivalChannel();
        if (chatRevivalChannel == 0)
            return;

        GuildChannel channel = guild.getGuildChannelById(chatRevivalChannel);
        if (channel == null)
            return;

        if (channel.getType() != ChannelType.TEXT)
            return;

        TextChannel textChannel = (TextChannel) channel;
        if (!textChannel.canTalk())
            return;

        if (GUILD_EXECUTOR_MAP.containsKey(guild.getIdLong()))
            return;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        GUILD_EXECUTOR_MAP.put(guild.getIdLong(), executor);

        ChatReviver chatReviver = getChatReviver(guild);
        executor.scheduleAtFixedRate(
                () -> drawingChatRevival(guild, textChannel, chatReviver),
                chatReviver.nextDrawingTime(),
                TimeUnit.DAYS.toMillis(1),
                TimeUnit.MILLISECONDS
        );

        ShutdownHooks.register(executor::shutdown);
    }

    private void drawingChatRevival(Guild guild, TextChannel channel, ChatReviver chatReviver) {
        try {
            List<String> availableWords = getAvailableWords(chatReviver);
            if (availableWords.isEmpty()) {
                channel.sendMessage("❌ Uh oh! There appears to be no words left to be able to draw!").queue();
                Constants.LOGGER.error("❌ No words left to draw!");
                return;
            }

            String word = availableWords.get(ThreadLocalRandom.current().nextInt(availableWords.size()));
            if (word == null || word.isBlank()) {
                channel.sendMessage("❌ Uh oh! There appears to be no words left to be able to draw!").queue();
                Constants.LOGGER.error("❌ No words left to draw!");
                return;
            }

            word = WordUtils.capitalize(word.trim().toLowerCase(Locale.ROOT));

            chatReviver.getUsedDrawings().add(word);
            chatReviver.setLastDrawingTime(System.currentTimeMillis());
            Database.getDatabase().chatRevivers.replaceOne(Filters.eq("guild", guild.getIdLong()), chatReviver);

            channel.sendMessage("🎨 The word for today's drawing is: **" + word + "**! Happy drawing! 🎨").queue();
        } catch (IOException exception) {
            channel.sendMessage("❌ Uh oh! Something went wrong! Unable to do the drawing today! Please report this to the bot owner!").queue();
            Constants.LOGGER.error("❌ Unable to get available words for drawing!", exception);
        }
    }

    private static GuildConfig getGuildConfig(Guild guild) {
        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        return config;
    }

    private static ChatReviver getChatReviver(Guild guild) {
        ChatReviver chatReviver = Database.getDatabase().chatRevivers.find(Filters.eq("guild", guild.getIdLong())).first();
        if (chatReviver == null) {
            chatReviver = new ChatReviver(guild.getIdLong());
            Database.getDatabase().chatRevivers.insertOne(chatReviver);
        }

        return chatReviver;
    }

    private static List<String> getDrawingWords() throws IOException {
        InputStream stream = TurtyBot.class.getResourceAsStream("/objects.txt");
        if (stream == null)
            throw new IOException("Could not find objects.txt file!");

        List<String> lines = IOUtils.readLines(stream, StandardCharsets.UTF_8);
        IOUtils.closeQuietly(stream);
        return lines;
    }

    private static List<String> getAvailableWords(ChatReviver chatReviver) throws IOException {
        List<String> words = getDrawingWords();
        words.removeAll(chatReviver.getUsedDrawings());
        return words;
    }
}
