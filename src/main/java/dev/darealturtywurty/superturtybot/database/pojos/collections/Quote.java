package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quote {
    private long guild;
    private long user;

    private String text;
    private long timestamp;
    private long addedBy;

    public Quote(final long guild, final long user, final String text, final long addedBy) {
        this.guild = guild;
        this.user = user;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.addedBy = addedBy;
    }
}
