package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

public record DashboardWarningRecord(
        String uuid,
        String userId,
        String userDisplayName,
        String userAvatarUrl,
        String warnerId,
        String warnerDisplayName,
        String warnerAvatarUrl,
        String reason,
        long warnedAt
) {
}
