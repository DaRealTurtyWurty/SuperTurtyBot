package dev.darealturtywurty.superturtybot.commands.util.roblox;

import lombok.Data;

@Data
public class RobloxFriendData {
    private boolean isOnline;
    private boolean isDeleted;
    private int friendFrequentScore;
    private int friendFrequentRank;
    private boolean hasVerifiedBadge;
    private String description;
    private String created;
    private boolean isBanned;
    private String externalAppDisplayName;
    private long id;
    private String name;
    private String displayName;
}