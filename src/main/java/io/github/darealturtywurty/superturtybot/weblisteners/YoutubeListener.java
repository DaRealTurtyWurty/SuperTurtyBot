package io.github.darealturtywurty.superturtybot.weblisteners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bson.conversions.Bson;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class YoutubeListener {
    private static final String TOPIC_URL = "https://www.youtube.com/xml/feeds/videos.xml?channel_id=%s";
    private static final String SUBSCRIBE_URL = "https://pubsubhubbub.appspot.com/subscribe";
    private static final String CALLBACK_URL = "http://%s:8912/youtube";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final DocumentBuilder documentBuilder;

    public YoutubeListener(JDA jda) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        try {
            this.documentBuilder = factory.newDocumentBuilder();
        } catch (final ParserConfigurationException exception) {
            throw new IllegalStateException("Unable to create document builder!", exception);
        }

        createServer(jda);
        
        // Re-subscribe every 4 days 23 hours
        EXECUTOR.scheduleAtFixedRate(
            () -> Database.getDatabase().youtubeNotifier.find()
                .forEach(notifier -> subscribe(notifier.getGuild(), notifier.getYoutubeChannel())),
            0, 119, TimeUnit.HOURS);
    }
    
    public void subscribe(long guildId, String channelId) {
        subscribe(guildId, channelId, false);
    }

    public void subscribe(long guildId, String channelId, boolean unsubscribe) {
        final Optional<YoutubeNotifier> optional = getYoutubeNotifier(guildId);
        if (optional.isEmpty())
            return;

        final String callbackURL = CALLBACK_URL.formatted(getIP().orElseThrow());
        final String topicURL = TOPIC_URL.formatted(channelId);
        System.out.println(callbackURL);
        System.out.println(topicURL);
        
        final Map<String, String> params = Map.of("hub.callback", callbackURL, "hub.topic", topicURL, "hub.mode",
            unsubscribe ? "unsubscribe" : "subscribe", "hub.verify", "sync");

        final List<String> listParams = params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).toList();
        final String strParams = String.join("&", listParams);
        final Request request = new Request.Builder().method("POST", RequestBody.create(new byte[0]))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .url(YoutubeListener.SUBSCRIBE_URL + "?" + strParams).build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                throw new IllegalStateException("An error has occured " + (unsubscribe ? "unsubcribing" : "subscribing")
                    + " channel ID: " + channelId, exception);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response.code());
            }
        });
    }

    private void createServer(JDA jda) {
        final Javalin app = Javalin.create(config -> {
            final var server = new Server();
            final var connector = new ServerConnector(server);
            connector.setHost(getIP().orElse("0.0.0.0"));
            connector.setPort(8912);
            server.setConnectors(new Connector[] { connector });
            config.server(() -> server);
        });

        app.post("/youtube", context -> {
            final Map<String, String> params = context.pathParamMap();
            System.out.println(params);
            if (params.containsKey("hub.challenge")) {
                final byte[] response = params.get("hub.challenge").getBytes(StandardCharsets.UTF_8);
                context.res.setStatus(HttpURLConnection.HTTP_OK);
                context.result(response);
                return;
            }
            
            final Optional<Video> parsed = parseVideo(context.bodyAsInputStream());
            final Video video = parsed.orElseThrow(
                () -> new IllegalStateException("No valid video was provided!\n\n ```xml\n" + context.body() + "```"));

            final List<YoutubeNotifier> notifiers = new ArrayList<>();
            Database.getDatabase().youtubeNotifier.find(Filters.eq("videoId", video.videoId)).forEach(notifiers::add);
            notifiers.stream().filter(notifier -> notifier.getYoutubeChannel().equals(video.channel().id())
                && !notifier.getStoredVideos().contains(video.videoId())).forEach(notifier -> {
                    final Guild guild = jda.getGuildById(notifier.getGuild());

                    notifier.getStoredVideos().add(video.videoId());
                    final Bson filter = getFilter(guild.getIdLong());
                    Database.getDatabase().youtubeNotifier.updateOne(filter,
                        Updates.set("storedVideos", notifier.getStoredVideos()));

                    final long videoChannel = notifier.getChannel();
                    final TextChannel channel = guild.getTextChannelById(videoChannel);
                    if (channel == null || !channel.canTalk())
                        return;
                    channel.sendMessage(notifier.getMention() + " A new video was just released by **"
                        + video.channel().name() + "** called **" + video.title() + "**!\n" + video.url()).queue();
                });
        });

        app.start(8912);

        ShutdownHooks.register(app::close);
    }

    private Optional<Video> parseVideo(InputStream input) {
        Document document;
        try {
            document = this.documentBuilder.parse(input);
        } catch (SAXException | IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
        
        final Node entry = document.getElementsByTagName("entry").item(0);
        final NodeList entryChildren = entry.getChildNodes();
        final String id = entryChildren.item(0).getNodeValue();
        final String videoId = entryChildren.item(1).getNodeValue();
        final String channelId = entryChildren.item(2).getNodeValue();
        final String title = entryChildren.item(3).getNodeValue();
        final String url = (String) entryChildren.item(4).getUserData("href");
        final Node author = entryChildren.item(5);
        final String channelName = author.getFirstChild().getNodeValue();
        final String channelURL = author.getLastChild().getNodeValue();
        final String publishedAt = entryChildren.item(6).getNodeValue();
        final String updatedAt = entryChildren.item(7).getNodeValue();
        return Optional.of(new Video(id, videoId, title, url, new Video.Channel(channelId, channelName, channelURL),
            LocalDate.parse(publishedAt), LocalDate.parse(updatedAt)));
    }

    public static Optional<String> getIP() {
        try {
            final URL whatismyip = new URL("http://checkip.amazonaws.com");
            try (final var reader = new BufferedReader(new InputStreamReader(whatismyip.openStream()))) {
                return Optional.ofNullable(reader.readLine());
            }
        } catch (final IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }
    
    private static Bson getFilter(long guildId) {
        return Filters.eq("guild", guildId);
    }
    
    private static Optional<YoutubeNotifier> getYoutubeNotifier(long guildId) {
        final Bson filter = getFilter(guildId);
        final YoutubeNotifier notifier = Database.getDatabase().youtubeNotifier.find(filter).first();
        if (notifier == null)
            return Optional.empty();
        
        return Optional.of(notifier);
    }
    
    private static record Video(String id, String videoId, String title, String url, Channel channel,
        LocalDate publishedAt, LocalDate updatedAt) {
        private static record Channel(String id, String name, String url) {
        }
    }
}
