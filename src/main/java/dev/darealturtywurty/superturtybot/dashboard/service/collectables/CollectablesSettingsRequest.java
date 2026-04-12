package dev.darealturtywurty.superturtybot.dashboard.service.collectables;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class CollectablesSettingsRequest {
    private String collectorChannelId;
    private boolean collectingEnabled;
    private boolean collectableTypesRestricted;
    private List<String> enabledCollectableTypeIds;
    private Map<String, List<String>> disabledCollectablesByType;
}
