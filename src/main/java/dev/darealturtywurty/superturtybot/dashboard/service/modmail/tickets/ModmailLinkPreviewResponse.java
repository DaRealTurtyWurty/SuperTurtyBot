package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

public record ModmailLinkPreviewResponse(
        String url,
        String title,
        String description,
        String siteName,
        String imageUrl,
        String type
) {
}
