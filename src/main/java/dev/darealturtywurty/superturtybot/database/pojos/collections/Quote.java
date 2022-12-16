package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.Data;

@Data
public class Quote {
    private long guild;
    private long user;

    private String text;
    private long timestamp;
    private long addedBy;

    public Quote(final long guild, final long user, final String text, final long timestamp, final long addedBy) {
        this.guild = guild;
        this.user = user;
        this.text = text;
        this.timestamp = timestamp;
        this.addedBy = addedBy;
    }

    public Quote() {
        this(0, 0, "", 0, 0);
    }
}
