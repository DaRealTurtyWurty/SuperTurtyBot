package dev.darealturtywurty.superturtybot.dashboard.http;

public record DashboardHealthResponse(
        String status,
        String environment,
        String botStatus,
        long startedAt,
        int configOptionCount,
        String publicUrl
) {
}
