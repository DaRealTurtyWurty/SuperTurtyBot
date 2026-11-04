package dev.darealturtywurty.superturtybot.core.util;

import com.squareup.moshi.JsonDataException;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.OAuthData;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.NoopTokenStore;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class RedditUtils {
    public static RedditClient REDDIT;

    static {
        if (Environment.INSTANCE.redditClientId().isPresent()
            && Environment.INSTANCE.redditClientSecret().isPresent()) {
            final var userAgent = new UserAgent("bot",
                "dev.darealturtywurty.superturtybot" + (CommandHook.isDevMode() ? ".dev" : ""), "1.0", "TurtyWurty");
            final var adapter = new OkHttpNetworkAdapter(userAgent, createHttpClient());
            REDDIT = createRedditClient(adapter);
            if (REDDIT != null) {
                REDDIT.setLogHttp(true);
            }
        }
    }

    private RedditUtils() {
        throw new IllegalAccessError("Cannot access private constructor!");
    }

    @Nullable
    public static RootCommentNode findValidPost(SubredditReference subreddit, String... subreddits) {
        final LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(subreddit.getSubreddit());

        final List<String> remaining = new ArrayList<>(Arrays.asList(subreddits));
        Collections.shuffle(remaining);
        candidates.addAll(remaining);

        for (final String candidate : candidates) {
            final RootCommentNode post = getRandomPost(getSubreddit(candidate));
            if (post != null)
                return post;
        }

        return null;
    }

    @Nullable
    public static RootCommentNode getRandomPost(SubredditReference subreddit) {
        try {
            final Submission submission = getRandomSubmission(subreddit);
            return submission == null ? null : submission.toReference(REDDIT).comments();
        } catch (NetworkException | JsonDataException | ApiException exception) {
            Constants.LOGGER.error("Failed to get random post!", exception);
            return null;
        }
    }

    @Nullable
    public static Submission getRandomSubmission(SubredditReference subreddit) {
        final List<Submission> posts = subreddit.posts()
            .sorting(SubredditSort.HOT)
            .limit(50)
            .build()
            .accumulateMerged(1);

        if (posts.isEmpty()) {
            Constants.LOGGER.warn("Reddit returned no hot posts for r/{}", subreddit.getSubreddit());
            return null;
        }

        return posts.get(ThreadLocalRandom.current().nextInt(posts.size()));
    }

    @Nullable
    private static RedditClient createRedditClient(OkHttpNetworkAdapter adapter) {
        final String clientId = Environment.INSTANCE.redditClientId().get();
        final String clientSecret = Environment.INSTANCE.redditClientSecret().get();
        final Optional<String> username = Environment.INSTANCE.redditUsername();
        final Optional<String> refreshToken = Environment.INSTANCE.redditRefreshToken();
        final Optional<String> password = Environment.INSTANCE.redditPassword();

        if (username.isPresent() && refreshToken.isPresent()) {
            Constants.LOGGER.info("Using Reddit refresh token authentication for user '{}'.", username.get());
            final Credentials credentials = Credentials.webapp(clientId, clientSecret,
                Environment.INSTANCE.redditRedirectUrl().orElse("http://localhost"));
            final OAuthData initialOAuthData = OAuthData.create("", List.of(), refreshToken.get(), new Date(0));
            return new RedditClient(adapter, initialOAuthData, credentials, new NoopTokenStore(), username.get());
        }

        if (username.isPresent() && password.isPresent()) {
            Constants.LOGGER.info("Using Reddit script authentication for user '{}'.", username.get());
            final Credentials credentials = Credentials.script(username.get(), password.get(), clientId, clientSecret);
            return OAuthHelper.automatic(adapter, credentials);
        }

        if (username.isPresent() || refreshToken.isPresent() || password.isPresent()) {
            Constants.LOGGER.warn(
                "Incomplete Reddit authenticated configuration provided. Falling back to userless authentication.");
        }

        Constants.LOGGER.info("Using Reddit userless authentication.");
        final Credentials credentials = Credentials.userless(clientId, clientSecret, UUID.randomUUID());
        return OAuthHelper.automatic(adapter, credentials);
    }

    @NotNull
    private static OkHttpClient createHttpClient() {
        final var builder = new OkHttpClient.Builder();
        if (Environment.INSTANCE.redditProxyHost().isPresent() && Environment.INSTANCE.redditProxyPort().isPresent()) {
            final var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                Environment.INSTANCE.redditProxyHost().get(),
                Environment.INSTANCE.redditProxyPort().get()));
            builder.proxy(proxy);
        }

        builder.connectTimeout(30, TimeUnit.SECONDS);
        return builder.build();
    }

    @NotNull
    public static SubredditReference getRandomSubreddit(String... subreddits) {
        return Stream.of(subreddits).skip(ThreadLocalRandom.current().nextInt(subreddits.length)).map(subreddit -> {
            final SubredditReference reference = getSubreddit(subreddit);
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
}
