package dev.darealturtywurty.superturtybot.dashboard.service.quotes;

import java.util.List;

public record DashboardQuotesPageResponse(
        int page,
        int pageSize,
        long totalCount,
        int totalPages,
        List<DashboardQuoteRecord> quotes
) {
}
