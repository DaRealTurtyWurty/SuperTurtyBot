package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

import java.util.List;

public record DashboardSuggestionRecord(
        int number,
        String messageId,
        String messageUrl,
        String userId,
        String userDisplayName,
        String userAvatarUrl,
        String content,
        String mediaUrl,
        DashboardSuggestionMediaPreviewResponse mediaPreview,
        long createdAt,
        String status,
        List<DashboardSuggestionResponseEntry> responses
) {
}
