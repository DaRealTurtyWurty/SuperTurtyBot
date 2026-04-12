package dev.darealturtywurty.superturtybot.dashboard.service.tags;

import java.util.List;

public record DashboardTagsPageResponse(
        int page,
        int pageSize,
        long totalCount,
        int totalPages,
        List<DashboardTagRecord> tags
) {
}
