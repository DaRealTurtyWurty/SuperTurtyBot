package dev.darealturtywurty.superturtybot.dashboard.service.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoggingSettingsRequest {
    private String loggingChannelId;
    private String modLoggingChannelId;
    private boolean logChannelCreate;
    private boolean logChannelDelete;
    private boolean logChannelUpdate;
    private boolean logEmojiAdded;
    private boolean logEmojiRemoved;
    private boolean logEmojiUpdate;
    private boolean logForumTagUpdate;
    private boolean logStickerUpdate;
    private boolean logGuildUpdate;
    private boolean logRoleUpdate;
    private boolean logBan;
    private boolean logUnban;
    private boolean logInviteCreate;
    private boolean logInviteDelete;
    private boolean logMemberJoin;
    private boolean logMemberRemove;
    private boolean logStickerAdded;
    private boolean logStickerRemove;
    private boolean logTimeout;
    private boolean logMessageBulkDelete;
    private boolean logMessageDelete;
    private boolean logMessageUpdate;
    private boolean logRoleCreate;
    private boolean logRoleDelete;
}
