package dev.darealturtywurty.superturtybot.commands.economy.promotion.youtube;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public record VideoCacheRepository(Path databasePath) {
    private static void ensureTables(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS dead_video_ids (video_id TEXT PRIMARY KEY, added_at INTEGER NOT NULL)")) {
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS recent_video_cache (" +
                        "video_id TEXT PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "thumbnail_url TEXT, " +
                        "view_count INTEGER NOT NULL, " +
                        "like_count INTEGER NOT NULL, " +
                        "published_at INTEGER NOT NULL, " +
                        "duration_seconds INTEGER NOT NULL, " +
                        "cached_at INTEGER NOT NULL)")) {
            statement.executeUpdate();
        }
    }

    private static void pruneCache(Connection connection, long cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM recent_video_cache WHERE cached_at < ?")) {
            statement.setLong(1, cutoff);
            statement.executeUpdate();
        }
    }

    private static List<YoutubeVideo> loadCachedVideos(Connection connection, long cutoff) throws SQLException {
        List<YoutubeVideo> videos = new ArrayList<>();
        String sql = "SELECT video_id, title, thumbnail_url, view_count, like_count, published_at, duration_seconds " +
                "FROM recent_video_cache WHERE cached_at >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoff);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String id = resultSet.getString(1);
                    String title = resultSet.getString(2);
                    String thumbnailUrl = resultSet.getString(3);
                    long viewCount = resultSet.getLong(4);
                    long likeCount = resultSet.getLong(5);
                    Instant publishedAt = Instant.ofEpochMilli(resultSet.getLong(6));
                    long durationSeconds = resultSet.getLong(7);
                    videos.add(new YoutubeVideo(id, title, thumbnailUrl, viewCount, likeCount, publishedAt,
                            durationSeconds, false));
                }
            }
        }

        return videos;
    }

    private static void cacheVideos(Connection connection, List<YoutubeVideo> videos, long now) throws SQLException {
        if (videos.isEmpty())
            return;

        String sql = "INSERT OR REPLACE INTO recent_video_cache " +
                "(video_id, title, thumbnail_url, view_count, like_count, published_at, duration_seconds, cached_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (YoutubeVideo video : videos) {
                statement.setString(1, video.id());
                statement.setString(2, video.title());
                statement.setString(3, video.thumbnailUrl());
                statement.setLong(4, video.viewCount());
                statement.setLong(5, video.likeCount());
                statement.setLong(6, video.publishedAt().toEpochMilli());
                statement.setLong(7, video.durationSeconds());
                statement.setLong(8, now);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void markDeadVideoIds(Connection connection, Set<String> deadIds, long now) throws SQLException {
        String sql = "INSERT OR IGNORE INTO dead_video_ids (video_id, added_at) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String id : deadIds) {
                statement.setString(1, id);
                statement.setLong(2, now);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static VideoIdSource resolveVideoIdSource(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName != null) {
                    String normalized = tableName.toLowerCase(Locale.ROOT);
                    if (!normalized.equals("dead_video_ids") && !normalized.equals("recent_video_cache")) {
                        Optional<String> column = findVideoIdColumn(meta, tableName);
                        if (column.isPresent())
                            return new VideoIdSource(tableName, column.get());
                    }
                }
            }
        }

        throw new SQLException("Could not locate a video id table in video_ids.db");
    }

    private static String resolveQuoteString(Connection connection) throws SQLException {
        String quoteString = connection.getMetaData().getIdentifierQuoteString();
        if (quoteString == null)
            return "\"";

        String trimmed = quoteString.trim();
        if (trimmed.isEmpty())
            return "\"";

        return trimmed;
    }

    private static Optional<String> findVideoIdColumn(DatabaseMetaData meta, String tableName) throws SQLException {
        List<String> candidates = List.of("video_id", "videoid", "id");
        try (ResultSet columns = meta.getColumns(null, null, tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (columnName != null) {
                    String normalized = columnName.toLowerCase(Locale.ROOT);
                    if (candidates.contains(normalized))
                        return Optional.of(columnName);
                }
            }
        }

        return Optional.empty();
    }

    private static List<String> fetchRandomVideoIds(Connection connection, VideoIdSource source, String quoteString,
                                                    int limit) throws SQLException {
        List<String> ids = new ArrayList<>();
        String table = quoteIdentifier(source.table(), quoteString);
        String column = quoteIdentifier(source.column(), quoteString);
        String sql = "SELECT " + column + " FROM " + table +
                " WHERE " + column + " NOT IN (SELECT video_id FROM dead_video_ids) " +
                "ORDER BY RANDOM() LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String id = resultSet.getString(1);
                    if (id != null && !id.isBlank())
                        ids.add(id);
                }
            }
        }
        return ids;
    }

    private static String quoteIdentifier(String value, String quoteString) {
        if (quoteString == null || quoteString.isBlank())
            return value;

        return quoteString + value.replace(quoteString, quoteString + quoteString) + quoteString;
    }

    public List<YoutubeVideo> loadCandidates(YoutubeApiClient apiClient, int randomCount, int maxAttempts,
                                             long cacheTtlMs) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath())) {
            ensureTables(connection);
            VideoIdSource source = resolveVideoIdSource(connection);
            String quoteString = resolveQuoteString(connection);
            long now = System.currentTimeMillis();
            long cacheCutoff = now - cacheTtlMs;

            pruneCache(connection, cacheCutoff);
            Map<String, YoutubeVideo> candidates = new HashMap<>();
            for (YoutubeVideo video : loadCachedVideos(connection, cacheCutoff)) {
                candidates.putIfAbsent(video.id(), video);
            }

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                List<String> randomIds = fetchRandomVideoIds(connection, source, quoteString, randomCount);
                if (randomIds.isEmpty())
                    break;

                YoutubeApiClient.VideoFetchResult result = apiClient.fetchVideos(randomIds);
                if (!result.missingIds().isEmpty())
                    markDeadVideoIds(connection, result.missingIds(), now);

                cacheVideos(connection, result.videos(), now);
                for (YoutubeVideo video : result.videos()) {
                    candidates.putIfAbsent(video.id(), video);
                }
            }

            return new ArrayList<>(candidates.values());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load YouTube videos.", exception);
        }
    }

    public void removeFromCache(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty())
            return;

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath())) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < videoIds.size(); i++) {
                if (i > 0) {
                    placeholders.append(',');
                }

                placeholders.append('?');
            }

            String sql = "DELETE FROM recent_video_cache WHERE video_id IN (" + placeholders + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < videoIds.size(); i++) {
                    statement.setString(i + 1, videoIds.get(i));
                }
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear used videos from cache.", exception);
        }
    }

    private record VideoIdSource(String table, String column) {
    }
}
