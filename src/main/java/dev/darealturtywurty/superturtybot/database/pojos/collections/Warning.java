package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warning {
    private long guild;
    private long user;
    
    private String reason;
    private long warner;
    private long warnedAt;
    private String uuid;
    
    public Warning(long guildId, long userId, String reason, long warnerId) {
        this(guildId, userId, reason, warnerId, System.currentTimeMillis(), UUID.randomUUID().toString());
    }
}
