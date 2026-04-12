package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

public record ModmailTicketSummaryResponse(
        long ticketNumber,
        long userId,
        String userDisplayName,
        String userAvatarUrl,
        long channelId,
        String channelName,
        long categoryId,
        String categoryName,
        boolean open,
        String source,
        long openedAt,
        long closedAt,
        long closedById,
        String closedByName,
        String closeReason,
        int transcriptChunkCount,
        int transcriptMessageCount
) {
}
