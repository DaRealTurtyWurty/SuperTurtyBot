package io.github.darealturtywurty.superturtybot.weblisteners;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bson.conversions.Bson;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeListener {
    private static final String TOPIC_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    
    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();
    
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    
    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void runExecutor(JDA jda) {
        if (IS_RUNNING.get())
            return;

        IS_RUNNING.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> Database.getDatabase().youtubeNotifier.find().forEach(notifier -> {
            final String url = TOPIC_URL.formatted(notifier.getYoutubeChannel());
            Constants.HTTP_CLIENT.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException exception) {
                    throw new IllegalStateException(
                        "Failed response from channel '" + notifier.getYoutubeChannel() + "'", exception);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful())
                        throw new IllegalStateException(
                            "Failed response from channel '" + notifier.getYoutubeChannel() + "'");

                    parseVideo(response.body().byteStream()).ifPresent(video -> {
                        final List<YoutubeNotifier> notifiers = new ArrayList<>();
                        Database.getDatabase().youtubeNotifier.find().forEach(found -> {
                            final Guild guild = jda.getGuildById(notifier.getGuild());
                            if (found.getYoutubeChannel().equals(video.channel().id())
                                && !found.getStoredVideos().contains(video.videoId()) && guild != null) {
                                notifiers.add(found);
                            }
                        });
                        
                        notifiers.forEach(notifier -> {
                            final Guild guild = jda.getGuildById(notifier.getGuild());
                            if (guild == null)
                                return;

                            notifier.getStoredVideos().add(video.videoId());
                            final Bson filter = getFilter(guild.getIdLong());
                            Database.getDatabase().youtubeNotifier.updateOne(filter,
                                Updates.set("storedVideos", notifier.getStoredVideos()));

                            final long videoChannel = notifier.getChannel();
                            final TextChannel channel = guild.getTextChannelById(videoChannel);
                            if (channel == null || !channel.canTalk())
                                return;
                            channel
                                .sendMessage(notifier.getMention() + " A new video was just released by **"
                                    + video.channel().name() + "** called **" + video.title() + "**!\n" + video.url())
                                .queue();
                        });
                    });
                }
            });
        }), 0, 1, TimeUnit.MINUTES);
    }
    
    private static Bson getFilter(long guildId) {
        return Filters.eq("guild", guildId);
    }
    
    private static Optional<Video> parseVideo(InputStream input) {
        Document document;
        try {
            DocumentBuilder documentBuilder;
            try {
                documentBuilder = DOCUMENT_FACTORY.newDocumentBuilder();
            } catch (final ParserConfigurationException exception) {
                throw new IllegalStateException("Unable to create document builder!", exception);
            }

            document = documentBuilder.parse(input);
        } catch (SAXException | IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
        
        final NodeList feed = document.getElementsByTagName("feed").item(0).getChildNodes();
        final Node entry = feed.item(15);
        final NodeList entryChildren = entry.getChildNodes();
        final String id = entryChildren.item(1).getTextContent();
        final String videoId = entryChildren.item(3).getTextContent();
        final String channelId = entryChildren.item(5).getTextContent();
        final String title = entryChildren.item(7).getTextContent();
        final String url = entryChildren.item(9).getAttributes().getNamedItem("href").getTextContent();
        final Node author = entryChildren.item(11);
        final String channelName = author.getChildNodes().item(1).getTextContent();
        final String channelURL = author.getChildNodes().item(3).getTextContent();
        final String publishedAt = entryChildren.item(13).getTextContent();
        final String updatedAt = entryChildren.item(15).getTextContent();
        
        final var publishDate = LocalDate.parse(publishedAt.split("T")[0]);
        final var updateDate = LocalDate.parse(updatedAt.split("T")[0]);
        
        return Optional.of(new Video(id, videoId, title, url, new Video.Channel(channelId, channelName, channelURL),
            publishDate, updateDate));
    }
    
    private static record Video(String id, String videoId, String title, String url, Channel channel,
        LocalDate publishedAt, LocalDate updatedAt) {
        private static record Channel(String id, String name, String url) {
        }
    }
}
