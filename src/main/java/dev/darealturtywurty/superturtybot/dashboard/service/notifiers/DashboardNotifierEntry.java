package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import java.util.List;
import dev.darealturtywurty.superturtybot.weblisteners.social.NotifierDiagnostics;

public record DashboardNotifierEntry(
    String type,
    String kind,
    String targetLabel,
    String targetValue,
    String channelId,
    String channelName,
    String mention,
    List<String> details,
    NotifierDiagnostics.Status diagnostics
) {
}
