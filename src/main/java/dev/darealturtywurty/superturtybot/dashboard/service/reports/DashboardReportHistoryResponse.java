package dev.darealturtywurty.superturtybot.dashboard.service.reports;

import java.util.List;

public record DashboardReportHistoryResponse(
        DashboardReportUserSummary user,
        List<DashboardReportRecord> reports
) {
}
