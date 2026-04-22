package dev.darealturtywurty.superturtybot.database.pojos.warnings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarningSanctionConfig {
    private String id;
    private String type;
    private int warningCount;
    private long durationMinutes;
    private int deleteMessageDays;

    public WarningSanctionAction getAction() {
        return WarningSanctionAction.fromKey(this.type);
    }
}
