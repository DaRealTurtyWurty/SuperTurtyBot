package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.*;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class YouTubeListener {
    private static final String TOPIC_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final int MAX_IDS_PER_REQUEST = 50;
    private static final Duration FAILURE_LOG_COOLDOWN = Duration.ofHours(1);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();
    private static final Map<String, Instant> RECENT_FAILURE_LOGS = new ConcurrentHashMap<>();

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void runExecutor(JDA jda) {
        if (IS_RUNNING.get())
            return;

        IS_RUNNING.set(true);

        Bson notifierFilter = Filters.and(
                Filters.exists("youtubeChannel", true),
                Filters.ne("youtubeChannel", ""),
                Filters.exists("channel", true),
                Filters.ne("channel", 0L),
                Filters.exists("guild", true)
        );
        EXECUTOR.scheduleAtFixedRate(() -> {
            Map<String, List<YoutubeNotifier>> channelMap = new HashMap<>();
            for (YoutubeNotifier notifier : Database.getDatabase().youtubeNotifier.find(notifierFilter)) {
                channelMap.computeIfAbsent(notifier.getYoutubeChannel(), k -> new ArrayList<>()).add(notifier);
            }

            for (Map.Entry<String, List<YoutubeNotifier>> entry : channelMap.entrySet()) {
                String channelId = entry.getKey();
                String feedUrl = String.format(TOPIC_URL, channelId);
                List<YoutubeNotifier> notifiers = entry.getValue();

                Request request = new Request.Builder().url(feedUrl).build();

                Constants.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                        logFetchFailure(channelId, "request:" + exception.getClass().getName(),
                                () -> Constants.LOGGER.error("Failed response from channel '{}'", channelId, exception));
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try (ResponseBody body = response.body()) {
                            if (!response.isSuccessful()) {
                                logFetchFailure(channelId, "status:" + response.code(),
                                        () -> Constants.LOGGER.error("Failed response from channel '{}' (status {})",
                                                channelId, response.code()));
                                return;
                            }

                            clearFailureState(channelId);

                            if (body == null) {
                                logFetchFailure(channelId, "empty-body",
                                        () -> Constants.LOGGER.error("Failed response from channel '{}' (empty body)", channelId));
                                return;
                            }

                            List<Video> videos = parseVideos(body.byteStream());
                            if (videos.isEmpty())
                                return;

                            Map<String, VideoDetails> videoDetailsMap = fetchVideoDetails(videos);

                            Map<YoutubeNotifier, Guild> notifierGuildMap = new HashMap<>();

                            for (YoutubeNotifier found : notifiers) {
                                final Guild guild = jda.getGuildById(found.getGuild());
                                for (Video video : videos) {
                                    if (!found.getStoredVideos().contains(video.videoId()) && guild != null) {
                                        notifierGuildMap.put(found, guild);
                                    }
                                }
                            }

                            for (Map.Entry<YoutubeNotifier, Guild> notifierEntry : notifierGuildMap.entrySet()) {
                                YoutubeNotifier notifier = notifierEntry.getKey();
                                Guild guild = notifierEntry.getValue();

                                final long videoChannel = notifier.getChannel();
                                final TextChannel channel = guild.getTextChannelById(videoChannel);
                                if (channel == null || !channel.canTalk()) {
                                    notifier.setChannel(0L);
                                    final Bson filter = getFilter(guild.getIdLong());
                                    Database.getDatabase().youtubeNotifier.updateOne(
                                            Filters.and(filter, Filters.eq("youtubeChannel", channelId)),
                                            Updates.set("channel", 0L));
                                    continue;
                                }

                                for (Video video : videos) {
                                    if (!notifier.getStoredVideos().contains(video.videoId())) {
                                        notifier.getStoredVideos().add(video.videoId());

                                        final Bson filter = getFilter(guild.getIdLong());
                                        Database.getDatabase().youtubeNotifier.updateOne(
                                                Filters.and(filter, Filters.eq("youtubeChannel", channelId)),
                                                Updates.set("storedVideos", notifier.getStoredVideos()));

                                        VideoDetails details = videoDetailsMap.getOrDefault(video.videoId(),
                                                VideoDetails.unknown());
                                        var embed = new EmbedBuilder()
                                                .setTitle(video.title(), video.url())
                                                .setDescription(video.description())
                                                .setImage(video.thumbnailUrl())
                                                .addField("Channel",
                                                        "[" + video.channel().name() + "](" + video.channel().url() + ")",
                                                        true)
                                                .addField("Type", details.type().label(), true)
                                                .addField("Published At", formatDiscordTimestamp(video.publishedAt()), true)
                                                .setFooter("YouTube Video ID: " + video.videoId())
                                                .setTimestamp(Instant.now());
                                        String message = buildNotificationMessage(notifier.getMention(), video, details);
                                        channel.sendMessage(message)
                                                .setAllowedMentions(EnumSet.allOf(Message.MentionType.class))
                                                .setEmbeds(embed.build()).queue();
                                    }
                                }
                            }
                        } catch (final Exception exception) {
                            Constants.LOGGER.error("Failed to process YouTube notifier response for '{}'", channelId,
                                    exception);
                        }
                    }
                });
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private static void logFetchFailure(String channelId, String reason, Runnable logger) {
        String key = channelId + "|" + reason;
        Instant now = Instant.now();
        Instant lastLogged = RECENT_FAILURE_LOGS.get(key);
        if (lastLogged != null && Duration.between(lastLogged, now).compareTo(FAILURE_LOG_COOLDOWN) < 0)
            return;

        RECENT_FAILURE_LOGS.put(key, now);
        logger.run();
    }

    private static void clearFailureState(String channelId) {
        RECENT_FAILURE_LOGS.keySet().removeIf(key -> key.startsWith(channelId + "|"));
    }

    private static Bson getFilter(long guildId) {
        return Filters.eq("guild", guildId);
    }

    private static List<Video> parseVideos(InputStream input) {
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
            Constants.LOGGER.error("Failed to parse XML!", exception);
            return Collections.emptyList();
        }

        final List<Video> videos = new ArrayList<>();

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//*[local-name()='entry']"; // Grabs all 'entry' tags ignoring namespaces
        NodeList entries;
        try {
            entries = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException exception) {
            Constants.LOGGER.error("Failed to evaluate XPath expression!", exception);
            return Collections.emptyList();
        }

        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            Video video = parseVideoEntry(entry);
            Instant cutoff = Instant.ofEpochMilli(TurtyBot.START_TIME).minusMillis(TimeUnit.HOURS.toMillis(1));
            if (video.publishedAt().toInstant(ZoneOffset.UTC).isBefore(cutoff))
                continue;

            videos.add(video);
        }

        return videos;
    }

    private static Video parseVideoEntry(Node entry) {
        String id = getChildByName(entry, "id").map(Node::getTextContent).orElse("");
        String videoId = getChildByName(entry, "yt:videoId").map(Node::getTextContent).orElse("");
        String title = getChildByName(entry, "title").map(Node::getTextContent).orElse("");
        String url = getChildByName(entry, "link")
                .map(node -> node.getAttributes().getNamedItem("href").getTextContent())
                .orElse("");

        String channelId = getChildByName(entry, "yt:channelId").map(Node::getTextContent).orElse("");
        Node authorNode = getChildByName(entry, "author").orElse(null);
        Video.Channel channel = parseChannel(authorNode, channelId).orElse(new Video.Channel(channelId, "", ""));
        LocalDateTime publishedAt = readDateTime(getChildByName(entry, "published").map(Node::getTextContent));
        LocalDateTime updatedAt = readDateTime(getChildByName(entry, "updated").map(Node::getTextContent));

        Optional<Node> mediaGroup = getChildByName(entry, "media:group");
        String thumbnailUrl = mediaGroup.flatMap(group -> getChildByName(group, "media:thumbnail"))
                .map(node -> node.getAttributes().getNamedItem("url").getTextContent())
                .orElse("");
        String description = mediaGroup.flatMap(group -> getChildByName(group, "media:description"))
                .map(Node::getTextContent)
                .orElse("");

        Video.MediaStatistics mediaStatistics = parseMediaStatistics(entry).orElse(new Video.MediaStatistics(0, 0));
        return new Video(id, videoId, title, url, channel, publishedAt, updatedAt,
                thumbnailUrl, description, mediaStatistics);
    }

    private static Optional<Video.MediaStatistics> parseMediaStatistics(Node entry) {
        Optional<Node> group = getChildByName(entry, "media:group");
        if (group.isEmpty())
            return Optional.empty();

        Optional<Node> communityOpt = getChildByName(group.get(), "media:community");

        int likes = communityOpt.flatMap(community -> getChildByName(community, "media:starRating"))
                .map(node -> {
                    String countStr = node.getAttributes().getNamedItem("count").getTextContent();
                    try {
                        return Integer.parseInt(countStr);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .orElse(0);
        int views = communityOpt.flatMap(community -> getChildByName(community, "media:statistics"))
                .map(node -> {
                    String viewsStr = node.getAttributes().getNamedItem("views").getTextContent();
                    try {
                        return Integer.parseInt(viewsStr);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .orElse(0);
        return Optional.of(new Video.MediaStatistics(likes, views));
    }

    private static LocalDateTime readDateTime(Optional<String> dateTimeStr) {
        return dateTimeStr.map(str -> {
                    try {
                        return OffsetDateTime.parse(str).toLocalDateTime();
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to parse date time string: {}", str, exception);
                        return LocalDateTime.MIN;
                    }
                })
                .orElseGet(LocalDateTime::now);
    }

    private static Optional<Video.Channel> parseChannel(Node authorNode, String channelId) {
        if (authorNode == null)
            return Optional.empty();

        String channelName = getChildByName(authorNode, "name").map(Node::getTextContent).orElse("");
        String channelURL = getChildByName(authorNode, "uri").map(Node::getTextContent).orElse("");

        return Optional.of(new Video.Channel(channelId, channelName, channelURL));
    }

    private static Optional<Node> getChildByName(Node parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child == null)
                continue;

            if (child.getNodeName().equals(name))
                return Optional.of(child);
        }

        return Optional.empty();
    }

    private static Map<String, VideoDetails> fetchVideoDetails(List<Video> videos) {
        Optional<String> apiKey = Environment.INSTANCE.youtubeApiKey();
        if (apiKey.isEmpty())
            return Collections.emptyMap();

        List<String> ids = videos.stream().map(Video::videoId).filter(id -> !id.isBlank()).distinct().toList();
        if (ids.isEmpty())
            return Collections.emptyMap();

        Map<String, VideoDetails> results = new HashMap<>();
        String apiUrl = Environment.INSTANCE.youtubeVideosApiUrl()
                .orElse("https://www.googleapis.com/youtube/v3/videos");

        for (int i = 0; i < ids.size(); i += MAX_IDS_PER_REQUEST) {
            List<String> chunk = ids.subList(i, Math.min(i + MAX_IDS_PER_REQUEST, ids.size()));
            HttpUrl url = HttpUrl.parse(apiUrl);
            if (url == null) {
                Constants.LOGGER.error("Invalid YouTube videos API URL: {}", apiUrl);
                return results;
            }

            url = url.newBuilder()
                    .addQueryParameter("part", "contentDetails,snippet,liveStreamingDetails")
                    .addQueryParameter("id", String.join(",", chunk))
                    .addQueryParameter("key", apiKey.get())
                    .build();

            Request request = new Request.Builder().url(url).build();
            try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Constants.LOGGER.error("YouTube video lookup failed (status {})", response.code());
                    continue;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    Constants.LOGGER.error("YouTube video lookup returned an empty body");
                    continue;
                }

                JsonObject json = Constants.GSON.fromJson(body.string(), JsonObject.class);
                JsonArray items = json == null ? null : json.getAsJsonArray("items");
                if (items == null)
                    continue;

                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    if (item == null)
                        continue;

                    String id = item.has("id") ? item.get("id").getAsString() : "";
                    JsonObject snippet = item.has("snippet") ? item.getAsJsonObject("snippet") : null;
                    JsonObject contentDetails = item.has("contentDetails") ? item.getAsJsonObject("contentDetails") : null;
                    JsonObject liveStreamingDetails = item.has("liveStreamingDetails")
                            ? item.getAsJsonObject("liveStreamingDetails")
                            : null;

                    String liveContent = snippet != null && snippet.has("liveBroadcastContent")
                            ? snippet.get("liveBroadcastContent").getAsString()
                            : "";
                    String duration = contentDetails != null && contentDetails.has("duration")
                            ? contentDetails.get("duration").getAsString()
                            : "";
                    String scheduledStartTime = liveStreamingDetails != null && liveStreamingDetails.has("scheduledStartTime")
                            ? liveStreamingDetails.get("scheduledStartTime").getAsString()
                            : "";

                    if (!id.isBlank()) {
                        results.put(id, determineDetails(liveContent, duration, scheduledStartTime));
                    }
                }
            } catch (final Exception exception) {
                Constants.LOGGER.error("Failed to query YouTube video details", exception);
            }
        }

        return results;
    }

    private static VideoDetails determineDetails(String liveBroadcastContent, String duration, String scheduledStart) {
        VideoStatus status = "upcoming".equalsIgnoreCase(liveBroadcastContent)
                ? VideoStatus.SCHEDULED
                : VideoStatus.UPLOADED;

        Instant scheduledAt = null;
        if (status == VideoStatus.SCHEDULED && scheduledStart != null && !scheduledStart.isBlank()) {
            try {
                scheduledAt = OffsetDateTime.parse(scheduledStart).toInstant();
            } catch (final Exception exception) {
                Constants.LOGGER.error("Failed to parse scheduled start time: {}", scheduledStart, exception);
            }
        }

        if ("live".equalsIgnoreCase(liveBroadcastContent) || "upcoming".equalsIgnoreCase(liveBroadcastContent))
            return new VideoDetails(VideoType.STREAM, status, scheduledAt);

        if (duration == null || duration.isBlank())
            return new VideoDetails(VideoType.UNKNOWN, status, scheduledAt);

        try {
            Duration parsed = Duration.parse(duration);
            return new VideoDetails(parsed.getSeconds() <= 60 ? VideoType.SHORT : VideoType.VIDEO, status, scheduledAt);
        } catch (final Exception exception) {
            return new VideoDetails(VideoType.UNKNOWN, status, scheduledAt);
        }
    }

    private static String buildNotificationMessage(String mention, Video video, VideoDetails details) {
        String channelName = video.channel().name();
        if (channelName == null || channelName.isBlank())
            channelName = "Unknown channel";

        String verb = details.status() == VideoStatus.SCHEDULED ? "scheduled" : "uploaded";
        String typeLabel = details.type() == VideoType.UNKNOWN
                ? "video"
                : details.type().label().toLowerCase(Locale.ROOT);
        String scheduledSuffix = "";
        if (details.status() == VideoStatus.SCHEDULED && details.scheduledAt() != null) {
            long epoch = details.scheduledAt().getEpochSecond();
            scheduledSuffix = " for <t:" + epoch + ":F>";
        }

        return String.format("%s %s has %s a new %s%s!", mention, channelName, verb, typeLabel, scheduledSuffix);
    }

    private static String formatDiscordTimestamp(LocalDateTime time) {
        return TimeFormat.DATE_TIME_SHORT.format(time.toInstant(ZoneOffset.UTC));
    }

    private record Video(String id, String videoId, String title, String url, Channel channel,
                         LocalDateTime publishedAt, LocalDateTime updatedAt, String thumbnailUrl, String description,
                         MediaStatistics mediaStatistics) {
        private record Channel(String id, String name, String url) {
        }

        private record MediaStatistics(int likes, int views) {
        }
    }

    private enum VideoType {
        STREAM("Stream"),
        SHORT("Short"),
        VIDEO("Video"),
        UNKNOWN("Unknown");

        private final String label;

        VideoType(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    private enum VideoStatus {
        SCHEDULED,
        UPLOADED
    }

    private record VideoDetails(VideoType type, VideoStatus status, Instant scheduledAt) {
        private static VideoDetails unknown() {
            return new VideoDetails(VideoType.UNKNOWN, VideoStatus.UPLOADED, null);
        }
    }
}
