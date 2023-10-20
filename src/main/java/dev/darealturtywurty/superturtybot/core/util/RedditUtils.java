package dev.darealturtywurty.superturtybot.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.moshi.JsonDataException;
import dev.darealturtywurty.superturtybot.Environment;
import kotlin.text.Charsets;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.SubmissionPreview;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class RedditUtils {
    public static RedditClient REDDIT;

    static {
        if(Environment.INSTANCE.redditClientId().isPresent() && Environment.INSTANCE.redditClientSecret().isPresent()) {
            final var oAuthCreds = Credentials.userless(Environment.INSTANCE.redditClientId().get(),
                    Environment.INSTANCE.redditClientSecret().get(), UUID.randomUUID());
            final var userAgent = new UserAgent("bot", "dev.darealturtywurty.superturtybot", "1.0.0-alpha", "TurtyWurty");
            REDDIT = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oAuthCreds);
            REDDIT.setLogHttp(false);
        }
    }

    private RedditUtils() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }

    @Nullable
    public static Either<EmbedBuilder, Collection<String>> constructEmbed(boolean requireMedia, String... subreddits) {
        if (subreddits.length < 1) return null;

        final SubredditReference subreddit = getRandomSubreddit(subreddits);
        RootCommentNode post = findValidPost(subreddit, subreddits);
        int attempts = 0;
        while (post == null) {
            post = findValidPost(subreddit, subreddits);
            if (attempts++ > 10) return null;
        }

        var embed = new EmbedBuilder();
        String title = new String(Charsets.UTF_8.encode(post.getSubject().getTitle()).array());
        embed.setTitle(title.length() > 256 ? title.substring(0, 256) : title);

        String description = post.getSubject().getBody();
        if (description != null) {
            embed.setDescription(description.length() > 4096 ? description.substring(0, 4096) : description);
        }

        if (post.getSubject().getPreview() != null) {
            if(post.getSubject().getPreview().getImages().size() > 1) {
                List<String> images = post.getSubject().getPreview().getImages().stream()
                        .map(SubmissionPreview.ImageSet::getSource).map(SubmissionPreview.Variation::getUrl)
                        .map(url -> url.replace("external-preview", "i").replace("preview", "i")).toList();
                return Either.right(images);
            }
        }

        String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail() : post.getSubject()
                .getUrl();
        if (mediaURL == null || mediaURL.isBlank()) return null;

        if(mediaURL.contains("reddit.com/gallery")) {
            String json = mediaURL.replace("gallery", "comments") + ".json";
            try {
                URLConnection connection = new URL(json).openConnection();
                connection.setRequestProperty("User-Agent", "TurtyWurty");
                JsonObject listing = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                        JsonArray.class).get(0).getAsJsonObject();
                JsonObject data = listing.getAsJsonObject("data");
                JsonArray children = data.getAsJsonArray("children");
                JsonObject childData = children.get(0).getAsJsonObject().getAsJsonObject("data");
                JsonArray galleryData = childData.getAsJsonObject("gallery_data").getAsJsonArray("items");

                List<String> images = new ArrayList<>();
                for (JsonElement galleryDatum : galleryData) {
                    JsonObject galleryDataObject = galleryDatum.getAsJsonObject();
                    String media = galleryDataObject.get("media_id").getAsString();
                    JsonObject mediaMetadata = childData.getAsJsonObject("media_metadata");
                    JsonObject mediaObject = mediaMetadata.getAsJsonObject(media);
                    String type = mediaObject.get("m").getAsString().replace("image/", "");
                    images.add("https://i.redd.it/%s.%s".formatted(media, type));
                }

                return Either.right(images);
            } catch(IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        if (requireMedia && mediaURL.isBlank()) {
            post = findValidPost(subreddit, subreddits);
            if (post == null) return null;

            mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail() : post.getSubject()
                    .getUrl();

            if (mediaURL == null || mediaURL.isBlank()) return null;
        }

        // https://i.redgifs.com/i/respectfulrealisticdungenesscrab.jpg
        // replace with
        // https://www.redgifs.com/watch/respectfulrealisticdungenesscrab
        if(mediaURL.matches("https://i\\.redgifs\\.com/i/.*\\.(jpg|png|gif)")) {
            mediaURL = mediaURL.replace("https://i.redgifs.com/i/", "https://www.redgifs.com/watch/");
            mediaURL = mediaURL.substring(0, mediaURL.lastIndexOf("."));
            return Either.right(Collections.singletonList(mediaURL));
        }

        if (verifyVideo(mediaURL)) {
            mediaURL = StringUtils.replaceHTMLCodes(mediaURL);
            if (mediaURL.contains("redgifs") || mediaURL.contains("xvideos") || mediaURL.contains(
                    "xhamster") || mediaURL.contains("xxx") || mediaURL.contains("porn") || mediaURL.contains(
                    "nsfw") || mediaURL.contains("gfycat") || mediaURL.contains("/watch.") || mediaURL.contains(
                    "reddit.com") || mediaURL.contains("twitter") || mediaURL.contains("hub") || mediaURL.contains(
                    "imgur")) {
                embed = new EmbedBuilder();
                embed.setTitle(mediaURL);
                return Either.left(embed);
            }

            embed.setImage(mediaURL);
            embed.appendDescription("\nMedia not loading? [Click Me](" + mediaURL + ")");
        }

        embed.setTimestamp(Instant.now());
        return Either.left(embed);
    }

    @Nullable
    public static RootCommentNode findValidPost(SubredditReference subreddit, String... subreddits) {
        RootCommentNode post = null;
        int attempts = 0;
        while (post == null) {
            post = getRandomPost(subreddit);

            if (attempts % 5 == 0 && attempts != 0 && post == null) {
                subreddit = getRandomSubreddit(subreddits);
            } else if (attempts >= 15 && post == null) return null;

            attempts++;
        }

        return post;
    }

    @Nullable
    public static RootCommentNode getRandomPost(SubredditReference subreddit) {
        try {
            return subreddit.randomSubmission();
        } catch (NetworkException | JsonDataException | ApiException exception) {
            return null;
        }
    }

    @NotNull
    public static SubredditReference getRandomSubreddit(String... subreddits) {
        return Stream.of(subreddits).skip(ThreadLocalRandom.current().nextInt(subreddits.length)).map(subreddit -> {
            final SubredditReference reference = REDDIT.subreddit(subreddit);
            try {
                reference.about();
                return reference;
            } catch (final ApiException | NullPointerException exception) {
                Constants.LOGGER.error("Subreddit: {} cannot be accessed!", subreddit);
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new IllegalArgumentException(
                "Given list of subreddits does not contain any that are valid!\nSubreddits: '" + String.join(", ",
                        subreddits) + "'"));
    }

    @NotNull
    public static SubredditReference getSubreddit(String name) {
        return REDDIT.subreddit(name);
    }

    public static boolean verifyVideo(String url) {
        return !url.endsWith("mp4") && !url.endsWith("mov") && !url.endsWith("wmv") && !url.endsWith(
                "avi") && !url.endsWith("flv") && !url.endsWith("webm") && !url.endsWith("mkv");
    }
}
