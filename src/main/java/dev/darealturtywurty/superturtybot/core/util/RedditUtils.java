package dev.darealturtywurty.superturtybot.core.util;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import masecla.reddit4j.client.Reddit4J;
import masecla.reddit4j.client.UserAgentBuilder;
import masecla.reddit4j.exceptions.AuthenticationException;
import masecla.reddit4j.objects.RedditPost;
import masecla.reddit4j.objects.Sorting;
import masecla.reddit4j.objects.subreddit.RedditSubreddit;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class RedditUtils {
    private static Reddit4J REDDIT;

    static {
        if (Environment.INSTANCE.redditClientId().isPresent() && Environment.INSTANCE.redditClientSecret().isPresent()) {
            REDDIT = Reddit4J.rateLimited()
                    .setClientId(Environment.INSTANCE.redditClientId().get())
                    .setClientSecret(Environment.INSTANCE.redditClientSecret().get())
                    .setUserAgent(new UserAgentBuilder()
                            .appname("TurtyBot")
                            .author("TurtyWurty")
                            .version("1.0")
                            .build());

            try {
                REDDIT.userlessConnect();
            } catch (IOException | InterruptedException | AuthenticationException exception) {
                Constants.LOGGER.error("Failed to connect to Reddit!", exception);
                REDDIT = null;
            }
        } else {
            REDDIT = null;
        }
    }

    private RedditUtils() {
        throw new IllegalAccessError("Cannot access private constructor!");
    }

    public static Optional<Reddit4J> getReddit() {
        return Optional.ofNullable(REDDIT);
    }

    // TODO: Probably don't use this library cuz it has no support for media
    public static @Nullable Either<EmbedBuilder, Collection<String>> constructEmbed(boolean requireMedia, String... subreddits) {
        if (subreddits.length < 1) return null;

        final RedditSubreddit subreddit = getRandomSubreddit(subreddits);
        Optional<RedditPost> optPost = getRandomPost(subreddit.getFullName());

        int attempts = 0;
        while (optPost.isEmpty()) {
            optPost = findValidPost(subreddit, subreddits);
            if (attempts++ > 10) {
                return Either.right(List.of("Failed to find a valid post!"));
            }
        }

        RedditPost post = optPost.get();

        final var builder = new EmbedBuilder()
                .setTitle(post.getTitle())
                .setFooter("Posted by u/" + post.getAuthor() + " in r/" + post.getSubreddit() + " • " + post.getScore() + " upvotes")
                .setTimestamp(Instant.ofEpochSecond(post.getCreated()))
                .setColor(0xFF4500);

        if (post.getSelftext() != null && !post.getSelftext().isEmpty()) {
            builder.setDescription(post.getSelftext());
        }

        System.out.println(post.getMedia());
        return Either.left(builder);
//        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
//            if (verifyVideo(post.getMedia())) {
//                builder.setImage(post.getMedia());
//            } else if (isEmbedVideo(post.getMedia())) {
//                builder.setDescription(post.getMedia());
//            } else {
//                builder.setImage(post.getMedia());
//            }
//        } else if (post.getPreview() != null) {
//            final var preview = post.getPreview();
//            if (preview.getImages() != null && !preview.getImages().isEmpty()) {
//                final var image = preview.getImages().get(0);
//                if (image.getSource() != null) {
//                    builder.setImage(image.getSource().getUrl());
//                }
//            }
//        }
//
//        return Either.left(builder);
    }

    public static Optional<RedditPost> findValidPost(RedditSubreddit subreddit, String... subreddits) {
        Optional<RedditPost> post = Optional.empty();
        int attempts = 0;
        while (post.isEmpty()) {
            post = getRandomPost(subreddit.getFullName());

            if (attempts % 5 == 0 && attempts != 0 && post.isEmpty()) {
                subreddit = getRandomSubreddit(subreddits);
            } else if (attempts >= 15 && post.isEmpty()) return Optional.empty();

            attempts++;
        }

        return post;
    }

    public static @NotNull Optional<RedditPost> getRandomPost(@NotNull String subreddit) {
        try {
            List<RedditPost> posts = REDDIT.getSubredditPosts(subreddit, Sorting.HOT)
                    .limit(100)
                    .submit();

            return Optional.ofNullable(posts.get(ThreadLocalRandom.current().nextInt(posts.size())));
        } catch (AuthenticationException | InterruptedException | IOException exception) {
            Constants.LOGGER.error("Failed to get random post from subreddit: {}", subreddit, exception);
            return Optional.empty();
        }
    }

    public static @NotNull RedditSubreddit getRandomSubreddit(String... subreddits) throws IllegalArgumentException {
        return Stream.of(subreddits)
                .filter(Objects::nonNull)
                .skip(ThreadLocalRandom.current().nextInt(subreddits.length))
                .map(subreddit -> {
                    try {
                        return getSubreddit(subreddit);
                    } catch (final NullPointerException | IOException | InterruptedException exception) {
                        Constants.LOGGER.error("Subreddit: {} cannot be accessed!", subreddit);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("List of subreddits does not contain any that are valid!\n" +
                                "Subreddits: '" + String.join(", ", subreddits) + "'"));
    }

    public static @NotNull RedditSubreddit getSubreddit(String name) throws IOException, InterruptedException {
        return REDDIT.getSubreddit(name);
    }

    public static boolean verifyVideo(String url) {
        return !url.endsWith("mp4") && !url.endsWith("mov") && !url.endsWith("wmv") && !url.endsWith(
                "avi") && !url.endsWith("flv") && !url.endsWith("webm") && !url.endsWith("mkv");
    }

    public static boolean isEmbedVideo(String url) {
        return url.contains("redgifs") || url.contains("xvideos") || url.contains("xhamster") ||
                url.contains("xxx") || url.contains("porn") || url.contains("nsfw") || url.contains("gfycat") ||
                url.contains("/watch.") || url.contains("reddit.com") || url.contains("twitter") ||
                url.contains("hub") || url.contains("imgur") || url.contains("tiktok") || url.contains("youtube");
    }
}
