package dev.darealturtywurty.superturtybot.dashboard.service.counting;

import java.util.List;

public record DashboardCountingSettingsResponse(
        int maxCountingSuccession,
        List<DashboardCountingChannelInfo> channels,
        List<DashboardCountingModeInfo> availableModes
) {
}
