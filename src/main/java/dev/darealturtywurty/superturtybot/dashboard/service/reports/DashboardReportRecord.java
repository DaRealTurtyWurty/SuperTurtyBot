package dev.darealturtywurty.superturtybot.dashboard.service.reports;

public record DashboardReportRecord(
        String reporterId,
        String reporterDisplayName,
        String reporterAvatarUrl,
        String reason,
        long reportedAt
) {
}
