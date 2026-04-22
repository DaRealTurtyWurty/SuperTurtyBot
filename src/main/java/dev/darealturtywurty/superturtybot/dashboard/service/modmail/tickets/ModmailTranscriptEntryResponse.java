package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

import java.util.List;

public record ModmailTranscriptEntryResponse(
        String messageId,
        String authorId,
        String authorTag,
        String authorAvatarUrl,
        boolean bot,
        String content,
        List<ModmailLinkPreviewResponse> previews,
        List<String> attachments,
        List<String> embeds,
        List<String> stickers,
        long createdAt,
        long editedAt
) {
}
