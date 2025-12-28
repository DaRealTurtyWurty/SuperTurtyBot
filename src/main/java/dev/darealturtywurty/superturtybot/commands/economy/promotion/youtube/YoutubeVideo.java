package dev.darealturtywurty.superturtybot.commands.economy.promotion.youtube;

import java.time.Instant;

public record YoutubeVideo(String id, String title, String thumbnailUrl, long viewCount, long likeCount,
                           Instant publishedAt, long durationSeconds, boolean isLive) {
}
