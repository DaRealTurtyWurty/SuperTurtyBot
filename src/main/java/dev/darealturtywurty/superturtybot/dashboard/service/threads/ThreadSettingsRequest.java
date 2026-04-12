package dev.darealturtywurty.superturtybot.dashboard.service.threads;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ThreadSettingsRequest {
    private boolean shouldModeratorsJoinThreads;
    private List<String> autoThreadChannelIds = new ArrayList<>();
}
