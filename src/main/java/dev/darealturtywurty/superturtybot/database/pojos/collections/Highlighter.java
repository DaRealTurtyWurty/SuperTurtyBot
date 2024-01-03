package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Highlighter {
    private long guild;
    private long user;

    private String text;
    private boolean caseSensitive;
    private long timeAdded;
    private String uuid;

    public Highlighter(long guildId, long userId, String text, boolean caseSensitive) {
        this(guildId, userId, text, caseSensitive, System.currentTimeMillis(), UUID.randomUUID());
    }

    public Highlighter(long guildId, long userId, String text, boolean caseSensitive, long timeAdded, UUID uuid) {
        this.guild = guildId;
        this.user = userId;

        this.text = text;
        this.caseSensitive = caseSensitive;
        this.timeAdded = timeAdded;
        this.uuid = uuid.toString();
    }

    public UUID asUUID() {
        return UUID.fromString(this.uuid);
    }
}
