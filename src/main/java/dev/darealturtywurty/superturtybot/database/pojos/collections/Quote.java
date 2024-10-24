package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quote {
    private long guild;
    private long channel = -1L;
    private long message = -1L;

    private long user;
    private long addedBy;

    private String text;
    private long timestamp;
}
