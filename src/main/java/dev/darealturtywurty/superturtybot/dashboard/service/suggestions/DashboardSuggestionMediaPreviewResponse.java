package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

public record DashboardSuggestionMediaPreviewResponse(
        String url,
        String title,
        String description,
        String siteName,
        String imageUrl,
        String type
) {
}
