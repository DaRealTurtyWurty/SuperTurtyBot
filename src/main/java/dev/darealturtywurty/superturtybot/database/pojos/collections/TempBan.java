package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempBan {
    private String id;
    private long guild;
    private long user;
    private long moderator;
    private String reason;
    private int deleteDays;
    private long expiresAt;
    private long createdAt;
}
