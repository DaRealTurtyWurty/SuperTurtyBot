package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

import java.util.List;

public record ModmailTicketDetailResponse(
        ModmailTicketSummaryResponse ticket,
        String openerMessage,
        List<ModmailTranscriptEntryResponse> transcript
) {
}
