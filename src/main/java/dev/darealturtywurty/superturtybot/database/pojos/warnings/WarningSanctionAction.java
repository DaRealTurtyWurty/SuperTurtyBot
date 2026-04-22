package dev.darealturtywurty.superturtybot.database.pojos.warnings;

import net.dv8tion.jda.api.Permission;

import java.util.EnumSet;

public enum WarningSanctionAction {
    TIMEOUT("timeout", "Timeout", EnumSet.of(Permission.MODERATE_MEMBERS)),
    KICK("kick", "Kick", EnumSet.of(Permission.KICK_MEMBERS)),
    TEMPBAN("tempban", "Temporary Ban", EnumSet.of(Permission.BAN_MEMBERS)),
    BAN("ban", "Ban", EnumSet.of(Permission.BAN_MEMBERS));

    private final String key;
    private final String displayName;
    private final EnumSet<Permission> requiredPermissions;

    WarningSanctionAction(String key, String displayName, EnumSet<Permission> requiredPermissions) {
        this.key = key;
        this.displayName = displayName;
        this.requiredPermissions = requiredPermissions;
    }

    public String getKey() {
        return this.key;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public EnumSet<Permission> getRequiredPermissions() {
        return this.requiredPermissions;
    }

    public static WarningSanctionAction fromKey(String key) {
        if (key == null || key.isBlank())
            return null;

        for (WarningSanctionAction action : values()) {
            if (action.key.equalsIgnoreCase(key))
                return action;
        }

        return null;
    }
}
