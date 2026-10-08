package dev.darealturtywurty.superturtybot.dashboard.service.collectables;

import java.util.List;

public record DashboardCollectableCollection(
    String type,
    String displayName,
    String presentation,
    int totalCollectables,
    List<String> disabledCollectables,
    List<DashboardCollectableItem> collectables
) {
}
