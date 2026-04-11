package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private long guild;
    private long reported;
    private long reporter;
    private String reason;
    private long reportedAt;

    public Report(long guild, long reported, long reporter, String reason) {
        this(guild, reported, reporter, reason, System.currentTimeMillis());
    }
}
