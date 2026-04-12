package dev.darealturtywurty.superturtybot.dashboard.service.misc;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MiscSettingsRequest {
    private boolean shouldCreateGists;
    private boolean shouldSendStartupMessage;
    private boolean shouldSendChangelog;
    private boolean stickyRolesEnabled;
    private String patronRoleId;
}
