package dev.darealturtywurty.superturtybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import dev.darealturtywurty.superturtybot.commands.core.suggestion.SuggestionManager;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.listener.MusicListener;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.logback.DiscordLogbackAppender;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.EmojiReader;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.modules.*;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobCollector;
import dev.darealturtywurty.superturtybot.modules.counting.CountingManager;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import lombok.Getter;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.ext.java7.PathArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TurtyBot {
    public static final long START_TIME = System.currentTimeMillis();
    public static final EventWaiter EVENT_WAITER = new EventWaiter();
    public static final List<Message.MentionType> DEFAULT_ALLOWED_MENTIONS = List.of(Message.MentionType.ROLE, Message.MentionType.USER, Message.MentionType.CHANNEL, Message.MentionType.EMOJI, Message.MentionType.SLASH_COMMAND);

    @Getter
    private static long lastStartTime = 0L;
    @Getter
    private static Path lastStartTimePath;

    public static void main(String[] args) throws InvalidTokenException {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        Constants.LOGGER.info("Starting TurtyBot...");

        ArgumentParser parser = ArgumentParsers.newFor("TurtyBot")
                .build()
                .defaultHelp(true)
                .description("A multipurpose bot for discord.");

        parser.addArgument("-env", "--environment")
                .type(new PathArgumentType().verifyExists().verifyIsFile().verifyCanRead())
                .setDefault(Path.of("./.env"))
                .help("The path to the environment file.");

        parser.addArgument("-startTime", "--startTime")
                .type(new PathArgumentType().verifyIsFile().verifyCanRead().verifyCanWrite())
                .setDefault(Path.of("./lastStartTime.txt"))
                .help("The path to the file that stores the last start time.");

        parser.addArgument("-emojis", "--emojis")
                .type(new PathArgumentType().verifyExists().verifyIsFile().verifyCanRead())
                .setDefault(Path.of("./emojis.json"))
                .help("The path to the emojis json file.");

        parser.addArgument("-serverIcons", "--serverIcons")
                .type(new PathArgumentType().verifyExists().verifyIsFile().verifyCanRead())
                .setDefault(Path.of("./serverIcons.json"))
                .help("The path to the server icons json file.");

        Namespace namespace = parser.parseArgsOrFail(args);
        Environment.INSTANCE.load(namespace.get("environment"));
        Constants.LOGGER.info("Loaded environment file!");

        Path lastStartTimePath = namespace.get("startTime");
        if (Files.exists(lastStartTimePath)) {
            TurtyBot.lastStartTimePath = lastStartTimePath;

            try {
                lastStartTime = Long.parseLong(Files.readString(lastStartTimePath).trim());
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to read the last start time!", exception);
                lastStartTime = 0L;
            }
        }

        Path emojisPath = namespace.get("emojis");
        EmojiReader.setEmojisPath(emojisPath);

        Path serverIconsPath = namespace.get("serverIcons");
        ServerIconManager.setIconsPath(serverIconsPath);

        DiscordLogbackAppender.setup(Environment.INSTANCE.loggingWebhookId(), Environment.INSTANCE.loggingWebhookToken());

        Environment.INSTANCE.botToken().ifPresentOrElse(token -> {
            final var jdaBuilder = JDABuilder.createDefault(token);
            configureBuilder(jdaBuilder);
            jdaBuilder.build();

            MessageRequest.setDefaultMentions(DEFAULT_ALLOWED_MENTIONS);
            Constants.LOGGER.info("Setup JDA!");
        }, () -> {
            throw new InvalidTokenException();
        });

        loadRegistrars();
        Constants.LOGGER.info("Loaded registries!");
    }

    private static void configureBuilder(JDABuilder builder) {
        // Set the current activity to watching me!
        builder.setActivity(Activity.of(Environment.INSTANCE.activityType(), Environment.INSTANCE.activity().orElse("me!")));

        builder.setAutoReconnect(true);
        builder.setAudioSendFactory(new NativeAudioSendFactory());

        // We want to ensure that guild messages, DMs, members, emojis and voice states are enabled.
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT);

        // Cache all members. This makes it easier to do any kind of retrieval.
        builder.setMemberCachePolicy(MemberCachePolicy.ALL.and(MemberCachePolicy.lru(1000)));

        // Filter all member chunking.
        builder.setChunkingFilter(ChunkingFilter.ALL);

        // Disable caching of activity. We can retrieve this when needed.
        builder.disableCache(CacheFlag.ACTIVITY);

        // We don't need to cache overrides and voice states, it's an unnecessary load.
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES);

        // Ensure that the emotes, voice states, role tags and member overrides are being cached
        builder.enableCache(CacheFlag.EMOJI, CacheFlag.VOICE_STATE, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES);

        // If the guild size is bigger than this amount then only online members will be
        // cached (reduces bandwidth).
        builder.setLargeThreshold(250);

        // Add the command hook so that commands can be registered properly
        builder.addEventListeners(CommandHook.INSTANCE);

        // Add the suggestion manager so that suggestion reactions can be tracked
        builder.addEventListeners(SuggestionManager.INSTANCE);

        // Add the auto moderator so that the bot can handle auto moderations
        builder.addEventListeners(AutoModerator.INSTANCE);

        // Add the levelling manager so that the bot can handle levels
        builder.addEventListeners(LevellingManager.INSTANCE);

        // Add the counting manager so that the bot can listen for message events
        builder.addEventListeners(CountingManager.INSTANCE);

        // Add the starboard manager so that the bot can listen for messages in showcases and respond to reactions
        builder.addEventListeners(StarboardManager.INSTANCE);

        // Add the thread manager so that the bot can create threads automatically and also add moderators to threads
        builder.addEventListeners(ThreadManager.INSTANCE);

        // Add the gist manager so that the bot can create gists for text files upon user adding reaction
        builder.addEventListeners(GistManager.INSTANCE);

        // Add the logging manager so that the bot can log messages to a discord channel
        builder.addEventListeners(LoggingManager.INSTANCE);

        // Add the music manager so that we can leave voice channels when the bot is idle
        builder.addEventListeners(MusicListener.INSTANCE);

        // Add the chat reviver so that we can revive chats
        builder.addEventListeners(ChatRevivalManager.INSTANCE);

        // Add the hello responder so that we can respond to messages that say hello
        builder.addEventListeners(HelloResponseManager.INSTANCE);

        // Add file conversion manager so that we can convert files
        // builder.addEventListeners(FileConversionManager.INSTANCE);

        // Add welcome manager so that we can welcome new members and say goodbye to leaving members
        builder.addEventListeners(WelcomeManager.INSTANCE);

        // Add AI message responder so that we can respond to messages using AI
        builder.addEventListeners(AIMessageResponder.INSTANCE);

        // Add the minecraft mob collector so that we can collect minecraft mobs
        builder.addEventListeners(new MinecraftMobCollector());

        // Add the event waiter so that we can wait for events
        builder.addEventListeners(EVENT_WAITER);
    }

    private static void loadRegistrars() {
        final var reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(""))
                .setScanners(Scanners.SubTypes, Scanners.TypesAnnotated)
                .filterInputsBy(new FilterBuilder().includePackage("io")));
        reflections.getTypesAnnotatedWith(Registerer.class).forEach(clazz -> {
            try {
                clazz.getDeclaredConstructor().newInstance();
            } catch (final Exception exception) {
                Constants.LOGGER.error("Failed to load registerer!", exception);
            }
        });
    }

    public static @Nullable InputStream loadResource(String name) {
        return TurtyBot.class.getResourceAsStream("/" + name);
    }

    public static URL getResource(String name) {
        return TurtyBot.class.getResource(name);
    }

    public static @Nullable BufferedImage loadImage(String filename) {
        InputStream stream = loadResource(filename);
        if(stream == null)
            return null;

        try {
            return ImageIO.read(stream);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load image", exception);
            return null;
        }
    }
}
