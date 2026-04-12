package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

public record DashboardSuggestionResponseEntry(
        String type,
        String content,
        String responderId,
        String responderDisplayName,
        String responderAvatarUrl,
        long respondedAt
) {
}
