package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import java.util.List;

public record DashboardWarningDetailResponse(
        DashboardWarningRecord warning,
        DashboardWarningUserSummary user,
        List<DashboardWarningRecord> relatedWarnings
) {
}
