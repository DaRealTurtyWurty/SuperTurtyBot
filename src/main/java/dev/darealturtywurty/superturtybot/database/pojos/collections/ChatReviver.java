package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatReviver {
    private long guild;
    private long lastRunTime;
    private List<String> usedDrawings;

    public ChatReviver(long guild) {
        this(guild, 0L, new ArrayList<>());
    }

    public long nextRunTime(int hoursBetweenRuns) {
        long intervalMillis = TimeUnit.HOURS.toMillis(Math.max(1L, hoursBetweenRuns));
        if (this.lastRunTime <= 0L)
            return intervalMillis;

        long nextRunAt = this.lastRunTime + intervalMillis;
        return Math.max(0L, nextRunAt - System.currentTimeMillis());
    }
}
