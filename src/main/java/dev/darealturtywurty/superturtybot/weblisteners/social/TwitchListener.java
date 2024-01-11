package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.reactor.ReactorEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.TwitchNotifier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public class TwitchListener {
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static TwitchClient twitchClient;
    
    public static void initialize(JDA jda) {
        if (IS_INITIALIZED.get())
            return;

        IS_INITIALIZED.set(true);

        Environment.INSTANCE.twitchOAuthToken().ifPresentOrElse(token -> {
            twitchClient = TwitchClientBuilder.builder().withDefaultEventHandler(ReactorEventHandler.class)
                    .withDefaultAuthToken(new OAuth2Credential("twitch", token))
                    .withEnableHelix(true)
                    .build();

            twitchClient.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> handleGoLive(jda, event));
        }, () -> Constants.LOGGER.error("Twitch OAuth Token has not been set!"));
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }
    
    public static boolean subscribeChannel(String channel) {
        if (Database.getDatabase().twitchNotifier.countDocuments(Filters.eq("channel", channel)) == 0) {
            TwitchListener.twitchClient.getClientHelper().enableStreamEventListener(channel);
            return true;
        }

        return false;
    }

    public static void unsubscribe(String channel) {
        if (Database.getDatabase().twitchNotifier.countDocuments(Filters.eq("channel", channel)) == 0) {
            TwitchListener.twitchClient.getClientHelper().disableStreamEventListener(channel);
        }
    }

    private static void handleGoLive(JDA jda, ChannelGoLiveEvent event) {
        final Stream stream = event.getStream();
        final String channelName = stream.getUserName();
        final String gameName = stream.getGameName();
        final String title = stream.getTitle();
        final String url = "https://twitch.tv/" + channelName;
        for (TwitchNotifier notifier : Database.getDatabase().twitchNotifier.find(Filters.eq("channel", channelName))) {
            final Guild guild = jda.getGuildById(notifier.getGuild());
            if (guild == null)
                continue;

            final TextChannel discordChannel = guild.getTextChannelById(notifier.getDiscordChannel());
            if (discordChannel == null)
                continue;

            discordChannel.sendMessage(notifier.getMention() + " **" + channelName + "** is now live!\nGame: **"
                    + (gameName.isBlank() ? "Just Chatting" : gameName) + "**!\nTitle: " + title + "\n" + url).queue();
        }
    }
}
