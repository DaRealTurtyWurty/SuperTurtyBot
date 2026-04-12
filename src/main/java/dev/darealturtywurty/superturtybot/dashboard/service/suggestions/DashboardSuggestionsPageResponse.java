package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

import java.util.List;

public record DashboardSuggestionsPageResponse(
        String suggestionsChannelId,
        int page,
        int pageSize,
        long totalCount,
        int totalPages,
        List<DashboardSuggestionRecord> suggestions
) {
}
