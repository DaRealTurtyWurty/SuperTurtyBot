package dev.darealturtywurty.superturtybot.dashboard.service.tags;

public record DashboardTagRecord(
        String name,
        String userId,
        String userDisplayName,
        String userAvatarUrl,
        String contentType,
        String content,
        String rawData
) {
}
