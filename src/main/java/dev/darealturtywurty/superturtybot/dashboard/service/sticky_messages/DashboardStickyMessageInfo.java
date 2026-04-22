package dev.darealturtywurty.superturtybot.dashboard.service.sticky_messages;

public record DashboardStickyMessageInfo(
        String channelId,
        String channelName,
        boolean connected,
        String content,
        boolean hasEmbed,
        String ownerDisplayName,
        String ownerId,
        String postedMessage,
        long updatedAt
) {
}
