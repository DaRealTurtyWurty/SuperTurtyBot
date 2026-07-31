package dev.darealturtywurty.superturtybot.dashboard.service.collectables;

import java.util.List;

public record DashboardCollectablesPage(
        String type,
        String displayName,
        String presentation,
        int page,
        int pageSize,
        int totalCount,
        int totalPages,
        List<DashboardCollectableItem> collectables
) {
}
