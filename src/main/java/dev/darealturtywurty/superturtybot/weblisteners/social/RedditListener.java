package dev.darealturtywurty.superturtybot.weblisteners.social;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class RedditListener {
    private static final RssReader READER = new RssReader();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        try {
            final Stream<Item> stream = READER.read("https://www.reddit.com/r/memes/new/.rss");
            final TextChannel channel = jda.getTextChannelById(1033381901530046574L);
            stream.forEach(item -> channel.sendMessage(item.getTitle().orElse("no bitches")).queue());
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }
    
    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }
}
