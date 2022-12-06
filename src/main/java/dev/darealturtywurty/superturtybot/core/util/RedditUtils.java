package dev.darealturtywurty.superturtybot.core.util;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.squareup.moshi.JsonDataException;

import dev.darealturtywurty.superturtybot.Environment;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.EmbedBuilder;

public final class RedditUtils {
    public static final RedditClient REDDIT;
    
    static {
        final var oAuthCreds = Credentials.userless(Environment.INSTANCE.redditClientId(),
            Environment.INSTANCE.redditClientSecret(), UUID.randomUUID());
        final var userAgent = new UserAgent("bot", "dev.darealturtywurty.superturtybot", "1.0.0-alpha",
            "TurtyWurty");
        REDDIT = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oAuthCreds);
        REDDIT.setLogHttp(false);
    }
    
    private RedditUtils() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }
    
    @Nullable
    public static EmbedBuilder constructEmbed(boolean requireMedia, String... subreddits) {
        if (subreddits.length < 1)
            return null;
        
        final SubredditReference subreddit = getRandomSubreddit(subreddits);
        RootCommentNode post = findValidPost(subreddit, subreddits);
        
        var embed = new EmbedBuilder();
        embed.setTitle(String.valueOf(Charsets.UTF_8.encode(post.getSubject().getTitle())));
        embed.setDescription(post.getSubject().getBody());
        
        String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        if (requireMedia && (mediaURL == null || mediaURL.isBlank())) {
            post = findValidPost(subreddit, subreddits);
            if (post == null)
                return null;
            
            mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
                : post.getSubject().getUrl();
            
            if (mediaURL == null || mediaURL.isBlank())
                return null;
        }
        
        if (mediaURL != null && !mediaURL.isBlank() && verifyVideo(mediaURL)) {
            mediaURL = StringUtils.replaceHTMLCodes(mediaURL);
            if (mediaURL.contains("redgifs") || mediaURL.contains("xvideos") || mediaURL.contains("xhamster")
                || mediaURL.contains("xxx") || mediaURL.contains("porn") || mediaURL.contains("nsfw")
                || mediaURL.contains("gfycat") || mediaURL.contains("/watch.") || mediaURL.contains("reddit.com")
                || mediaURL.contains("twitter") || mediaURL.contains("hub") || mediaURL.contains("imgur")) {
                embed = new EmbedBuilder();
                embed.setTitle(mediaURL);
                return embed;
            }
            
            embed.setImage(mediaURL);
            embed.appendDescription("\nMedia not loading? [Click Me](" + mediaURL + ")");
        }
        
        embed.setTimestamp(Instant.now());
        return embed;
    }
    
    @Nullable
    public static RootCommentNode findValidPost(SubredditReference subreddit, String... subreddits) {
        RootCommentNode post = null;
        int attempts = 0;
        while (post == null) {
            post = getRandomPost(subreddit);
            
            if (attempts % 5 == 0 && attempts != 0 && post == null) {
                subreddit = getRandomSubreddit(subreddits);
            } else if (attempts >= 15 && post == null)
                return null;
            
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
        }).filter(Objects::nonNull).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Given list of subreddits does not contain any that are valid!\nSubreddits: '"
                    + String.join(", ", subreddits) + "'"));
    }
    
    @NotNull
    public static SubredditReference getSubreddit(String name) {
        return REDDIT.subreddit(name);
    }
    
    public static boolean verifyVideo(String url) {
        return !url.endsWith("mp4") && !url.endsWith("mov") && !url.endsWith("wmv") && !url.endsWith("avi")
            && !url.endsWith("flv") && !url.endsWith("webm") && !url.endsWith("mkv");
    }
}
