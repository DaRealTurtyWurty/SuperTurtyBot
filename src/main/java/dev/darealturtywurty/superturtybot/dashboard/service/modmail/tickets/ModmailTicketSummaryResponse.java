package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

public record ModmailTicketSummaryResponse(
        long ticketNumber,
        String userId,
        String userDisplayName,
        String userAvatarUrl,
        String channelId,
        String channelName,
        String categoryId,
        String categoryName,
        boolean open,
        String source,
        long openedAt,
        long closedAt,
        String closedById,
        String closedByName,
        String closeReason,
        int transcriptChunkCount,
        int transcriptMessageCount
) {
}
