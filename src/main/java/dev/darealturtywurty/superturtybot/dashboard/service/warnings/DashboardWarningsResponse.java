package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import java.util.List;

public record DashboardWarningsResponse(
        WarningsSettingsResponse settings,
        List<DashboardWarningRecord> warnings
) {
}
