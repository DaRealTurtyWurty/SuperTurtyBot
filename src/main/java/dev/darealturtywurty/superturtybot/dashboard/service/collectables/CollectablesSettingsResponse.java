package dev.darealturtywurty.superturtybot.dashboard.service.collectables;

import java.util.List;

public record CollectablesSettingsResponse(
        String collectorChannelId,
        boolean collectingEnabled,
        boolean collectableTypesRestricted,
        List<String> enabledCollectableTypeIds,
        List<DashboardCollectableCollection> collections
) {
}
