package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.core.util.MathUtils;
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
    private long lastDrawingTime;
    private List<String> usedDrawings;

    public ChatReviver(long guild) {
        this(guild, 0L, new ArrayList<>());
    }

    public long nextDrawingTime() {
        return MathUtils.clamp(
                this.lastDrawingTime - System.currentTimeMillis(),
                0,
                TimeUnit.DAYS.toMillis(1));
    }
}
