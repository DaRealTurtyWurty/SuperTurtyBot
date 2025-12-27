package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public record VideoPairSelector(long minVideoAgeMs, boolean filterShorts, long minViewDiff, double minViewDiffRatio) {
    public Optional<VideoPair> pickPair(List<YoutubeVideo> videos) {
        List<YoutubeVideo> filtered = filterVideos(videos);
        if (filtered.size() < 2)
            return Optional.empty();

        filtered.sort(Comparator.comparingLong(YoutubeVideo::viewCount));
        List<VideoPairCandidate> candidates = getVideoPairCandidates(filtered);
        if (candidates.isEmpty())
            return pickWidestPair(filtered);

        candidates.sort(Comparator.comparingLong(VideoPairCandidate::diff));
        int poolSize = Math.min(10, candidates.size());
        int pick = ThreadLocalRandom.current().nextInt(poolSize);
        return Optional.of(candidates.get(pick).pair());
    }

    private @NotNull List<VideoPairCandidate> getVideoPairCandidates(List<YoutubeVideo> filtered) {
        List<VideoPairCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < filtered.size() - 1; i++) {
            YoutubeVideo first = filtered.get(i);
            YoutubeVideo second = filtered.get(i + 1);
            long diff = Math.abs(first.viewCount() - second.viewCount());
            if (!meetsViewDiffThreshold(first.viewCount(), second.viewCount(), diff))
                continue;

            candidates.add(new VideoPairCandidate(new VideoPair(first, second), diff));
        }

        return candidates;
    }

    private List<YoutubeVideo> filterVideos(List<YoutubeVideo> videos) {
        if (videos.isEmpty())
            return videos;

        Instant cutoff = Instant.now().minus(minVideoAgeMs, ChronoUnit.MILLIS);
        List<YoutubeVideo> filtered = new ArrayList<>();
        for (YoutubeVideo video : videos) {
            if (video.isLive())
                continue;

            if (video.publishedAt().isAfter(cutoff))
                continue;

            if (filterShorts && video.durationSeconds() > 0 && video.durationSeconds() <= 60)
                continue;

            filtered.add(video);
        }

        return filtered;
    }

    private Optional<VideoPair> pickWidestPair(List<YoutubeVideo> videos) {
        YoutubeVideo widestFirst = null;
        YoutubeVideo widestSecond = null;
        long widestDiff = Long.MIN_VALUE;
        for (int i = 0; i < videos.size() - 1; i++) {
            YoutubeVideo first = videos.get(i);
            YoutubeVideo second = videos.get(i + 1);
            long diff = Math.abs(first.viewCount() - second.viewCount());
            if (diff > widestDiff) {
                widestDiff = diff;
                widestFirst = first;
                widestSecond = second;
            }
        }

        if (widestFirst == null)
            return Optional.empty();
        return Optional.of(new VideoPair(widestFirst, widestSecond));
    }

    private boolean meetsViewDiffThreshold(long firstViews, long secondViews, long diff) {
        long maxViews = Math.max(firstViews, secondViews);
        if (maxViews <= 0)
            return false;

        double ratio = diff / (double) maxViews;
        return diff >= minViewDiff || ratio >= minViewDiffRatio;
    }

    private record VideoPairCandidate(VideoPair pair, long diff) {
    }
}
