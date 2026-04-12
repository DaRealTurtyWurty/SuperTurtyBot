package dev.darealturtywurty.superturtybot.dashboard.service.quotes;

public record DashboardQuoteRecord(
        int number,
        String text,
        String userId,
        String userDisplayName,
        String userAvatarUrl,
        String addedById,
        String addedByDisplayName,
        String addedByAvatarUrl,
        String channelId,
        String messageId,
        String messageUrl,
        long timestamp
) {
}
