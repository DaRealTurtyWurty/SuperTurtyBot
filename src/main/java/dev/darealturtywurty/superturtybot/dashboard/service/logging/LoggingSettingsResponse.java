package dev.darealturtywurty.superturtybot.dashboard.service.logging;

public record LoggingSettingsResponse(
        String loggingChannelId,
        String modLoggingChannelId,
        boolean logChannelCreate,
        boolean logChannelDelete,
        boolean logChannelUpdate,
        boolean logEmojiAdded,
        boolean logEmojiRemoved,
        boolean logEmojiUpdate,
        boolean logForumTagUpdate,
        boolean logStickerUpdate,
        boolean logGuildUpdate,
        boolean logRoleUpdate,
        boolean logBan,
        boolean logUnban,
        boolean logInviteCreate,
        boolean logInviteDelete,
        boolean logMemberJoin,
        boolean logMemberRemove,
        boolean logStickerAdded,
        boolean logStickerRemove,
        boolean logTimeout,
        boolean logMessageBulkDelete,
        boolean logMessageDelete,
        boolean logMessageUpdate,
        boolean logRoleCreate,
        boolean logRoleDelete
) {
}
