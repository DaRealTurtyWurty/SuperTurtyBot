package dev.darealturtywurty.superturtybot.dashboard.service.modmail;

import java.util.List;

public record ModmailSettingsResponse(
        List<String> moderatorRoleIds,
        String ticketCreatedMessage
) {
}
