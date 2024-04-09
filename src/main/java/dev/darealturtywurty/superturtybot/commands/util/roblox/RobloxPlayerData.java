package dev.darealturtywurty.superturtybot.commands.util.roblox;

import lombok.Data;

@Data
public class RobloxPlayerData {
    private String[] previousUsernames;
    private boolean hasVerifiedBadge;
    private long id;
    private String name;
    private String displayName;
}