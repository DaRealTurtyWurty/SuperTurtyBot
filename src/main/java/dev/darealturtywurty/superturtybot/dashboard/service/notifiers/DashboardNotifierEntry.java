package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import java.util.List;

public record DashboardNotifierEntry(
        String type,
        String kind,
        String targetLabel,
        String targetValue,
        String channelId,
        String channelName,
        String mention,
        List<String> details
) {
}
