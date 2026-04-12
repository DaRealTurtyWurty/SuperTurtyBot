package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import java.util.List;

public record DashboardWarningHistoryResponse(
        DashboardWarningUserSummary user,
        List<DashboardWarningRecord> warnings
) {
}
