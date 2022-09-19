package io.github.darealturtywurty.superturtybot;

import java.net.URISyntaxException;

import javax.security.auth.login.LoginException;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import io.github.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import io.github.darealturtywurty.superturtybot.commands.util.suggestion.SuggestionManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandHook;
import io.github.darealturtywurty.superturtybot.core.console.NoHupListener;
import io.github.darealturtywurty.superturtybot.modules.AutoModerator;
import io.github.darealturtywurty.superturtybot.modules.StarboardManager;
import io.github.darealturtywurty.superturtybot.modules.counting.CountingManager;
import io.github.darealturtywurty.superturtybot.registry.Registerer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class TurtyBot {
    public static void main(String[] args) throws LoginException, URISyntaxException, InterruptedException {
        final var jdaBuilder = JDABuilder.createDefault(args.length < 1 ? Environment.INSTANCE.botToken() : args[0]);
        configureBuilder(jdaBuilder);
        JDA jda = jdaBuilder.build();
        loadRegisterers();
        
        jda = jda.awaitReady();
        Thread.sleep(1000);
        new NoHupListener().start(jda);
    }

    private static void configureBuilder(JDABuilder builder) {
        // Set the current activity to watching me!
        builder.setActivity(Activity.of(Environment.INSTANCE.activityType(), Environment.INSTANCE.activity()));

        // We want to ensure that guild messages, DMs, members, emojis andvoice states are enabled.
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT);

        // Cache all members. This makes it easier to do any kind of retrieval.
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);

        // Filter all member chunking.
        builder.setChunkingFilter(ChunkingFilter.ALL);

        // Disable caching of activity. We can retrieve this when needed.
        builder.disableCache(CacheFlag.ACTIVITY);

        // We dont need to cache overrides and voice states, its an unnecessary load.
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES);

        // Ensure that the emotes, voice states and role tags are being cached
        builder.enableCache(CacheFlag.EMOJI, CacheFlag.VOICE_STATE, CacheFlag.ROLE_TAGS);

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
    }

    @SuppressWarnings("deprecation")
    private static void loadRegisterers() {
        final var reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(""))
            .setScanners(Scanners.SubTypes, Scanners.TypesAnnotated)
            .filterInputsBy(new FilterBuilder().includePackage("io")));
        reflections.getTypesAnnotatedWith(Registerer.class).forEach(clazz -> {
            try {
                clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
}
