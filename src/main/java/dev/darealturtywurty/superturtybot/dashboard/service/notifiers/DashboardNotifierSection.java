package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import java.util.List;

public record DashboardNotifierSection(
        String key,
        String title,
        String description,
        int count,
        List<DashboardNotifierEntry> entries
) {
}
