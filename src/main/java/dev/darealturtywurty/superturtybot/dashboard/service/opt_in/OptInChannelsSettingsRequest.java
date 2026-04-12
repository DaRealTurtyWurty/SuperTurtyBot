package dev.darealturtywurty.superturtybot.dashboard.service.opt_in;

import java.util.List;

public record OptInChannelsSettingsRequest(
        List<String> optInChannelIds
) {
}
