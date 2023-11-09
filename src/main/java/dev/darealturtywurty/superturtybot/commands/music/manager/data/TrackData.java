package dev.darealturtywurty.superturtybot.commands.music.manager.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public class TrackData {
    private long userId;
    private @Nullable String playlist;

    public TrackData(long userId) {
        this(userId, null);
    }
}
