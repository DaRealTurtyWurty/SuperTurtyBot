package dev.darealturtywurty.superturtybot.weblisteners.social;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.reactor.ReactorEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.mongodb.client.model.Filters;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.database.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class TwitchListener {
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static TwitchClient twitchClient;
    
    public static void initialize(JDA jda) {
        if (IS_INITIALIZED.get())
            return;

        IS_INITIALIZED.set(true);
        
        twitchClient = TwitchClientBuilder.builder().withDefaultEventHandler(ReactorEventHandler.class)
            .withDefaultAuthToken(new OAuth2Credential("twitch", Environment.INSTANCE.twitchOAuthToken()))
            .withEnableHelix(true).build();

        twitchClient.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> handleGoLive(jda, event));
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }
    
    public static boolean subscribeChannel(Guild guild, String channel) {
        return TwitchListener.twitchClient.getClientHelper().enableStreamEventListener(channel) != null;
    }

    public static void unsubscribe(Guild guild, String channel) {
        TwitchListener.twitchClient.getClientHelper().disableStreamEventListener(channel);
    }

    private static void handleGoLive(JDA jda, ChannelGoLiveEvent event) {
        final String channel = event.getChannel().getName();
        final Stream stream = event.getStream();
        final String channelName = stream.getUserName();
        final String gameName = stream.getGameName();
        final String title = stream.getTitle();
        final String url = "https://twitch.tv/" + channelName;
        Database.getDatabase().twitchNotifier.find(Filters.eq("channel", channel)).forEach(notifier -> {
            final Guild guild = jda.getGuildById(notifier.getGuild());
            if (guild == null) {
                Database.getDatabase().twitchNotifier
                    .deleteOne(Filters.and(Filters.eq("guild", notifier.getGuild()), Filters.eq("channel", channel)));
                return;
            }

            final TextChannel discordChannel = guild.getTextChannelById(notifier.getDiscordChannel());
            if (discordChannel == null)
                return;

            discordChannel.sendMessage(notifier.getMention() + " **" + channelName + "** is now live!\nGame: **"
                + (gameName.isBlank() ? "Just Chatting" : gameName) + "**!\nTitle: " + title + "\n" + url).queue();
        });
    }
}
