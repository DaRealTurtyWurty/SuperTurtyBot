package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import dev.darealturtywurty.superturtybot.Environment;

import java.util.concurrent.TimeUnit;

public record YoutubePromotionConfig(int randomVideoCount, int maxFetchAttempts, int answerTimeoutSeconds,
                                     long cacheTtlMs, long minVideoAgeMs, long minViewDiff,
                                     double minViewDiffRatio, boolean filterShorts, String videosApiUrl) {
    private static final int DEFAULT_RANDOM_VIDEO_COUNT = 50;
    private static final int DEFAULT_MAX_FETCH_ATTEMPTS = 4;
    private static final int DEFAULT_ANSWER_TIMEOUT_SECONDS = 20;
    private static final long DEFAULT_CACHE_TTL_MS = TimeUnit.HOURS.toMillis(6);
    private static final long DEFAULT_MIN_VIDEO_AGE_MS = TimeUnit.HOURS.toMillis(24);
    private static final long DEFAULT_MIN_VIEW_DIFF = 50_000L;
    private static final double DEFAULT_MIN_VIEW_DIFF_RATIO = 0.2;
    private static final boolean DEFAULT_FILTER_SHORTS = true;
    private static final String DEFAULT_YOUTUBE_VIDEOS_API = "https://www.googleapis.com/youtube/v3/videos";

    public static YoutubePromotionConfig fromEnvironment(Environment environment) {
        int randomVideoCount = environment.youtubePromotionRandomVideoCount().orElse(DEFAULT_RANDOM_VIDEO_COUNT);
        int maxFetchAttempts = environment.youtubePromotionMaxFetchAttempts().orElse(DEFAULT_MAX_FETCH_ATTEMPTS);
        int answerTimeoutSeconds = environment.youtubePromotionAnswerTimeoutSeconds().orElse(DEFAULT_ANSWER_TIMEOUT_SECONDS);
        long cacheTtlMs = environment.youtubePromotionCacheTtlHours()
                .map(TimeUnit.HOURS::toMillis)
                .orElse(DEFAULT_CACHE_TTL_MS);
        long minVideoAgeMs = environment.youtubePromotionMinVideoAgeHours()
                .map(TimeUnit.HOURS::toMillis)
                .orElse(DEFAULT_MIN_VIDEO_AGE_MS);
        long minViewDiff = environment.youtubePromotionMinViewDiff().orElse(DEFAULT_MIN_VIEW_DIFF);
        double minViewDiffRatio = environment.youtubePromotionMinViewDiffRatio().orElse(DEFAULT_MIN_VIEW_DIFF_RATIO);
        boolean filterShorts = environment.youtubePromotionFilterShorts().orElse(DEFAULT_FILTER_SHORTS);
        String videosApiUrl = environment.youtubeVideosApiUrl().orElse(DEFAULT_YOUTUBE_VIDEOS_API);

        return new YoutubePromotionConfig(randomVideoCount, maxFetchAttempts, answerTimeoutSeconds, cacheTtlMs,
                minVideoAgeMs, minViewDiff, minViewDiffRatio, filterShorts, videosApiUrl);
    }
}
