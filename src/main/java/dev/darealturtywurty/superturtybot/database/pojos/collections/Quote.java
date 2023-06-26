package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.Data;

@Data
public class Quote {
    private long guild;
    private long user;

    private String text;
    private long timestamp;
    private long addedBy;
    private long timestampAdded;

    public Quote(final long guild, final long user, final String text, final long timestamp, final long addedBy, final long timestampAdded) {
        this.guild = guild;
        this.user = user;
        this.text = text;
        this.timestamp = timestamp;
        this.addedBy = addedBy;
        this.timestampAdded = timestampAdded;
    }

    public Quote(final long guild, final long user, final String text, final long timestamp, final long addedBy) {
        this.guild = guild;
        this.user = user;
        this.text = text;
        this.timestamp = timestamp;
        this.addedBy = addedBy;
        this.timestampAdded = System.currentTimeMillis();
    }

    public Quote() {
        this(0L, 0L, "", 0L, 0L, 0L);
    }
}
