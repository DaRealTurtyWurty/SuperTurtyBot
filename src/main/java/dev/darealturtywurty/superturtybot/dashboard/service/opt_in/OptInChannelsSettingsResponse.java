package dev.darealturtywurty.superturtybot.dashboard.service.opt_in;

import java.util.List;

public record OptInChannelsSettingsResponse(
        List<String> optInChannelIds
) {
}
