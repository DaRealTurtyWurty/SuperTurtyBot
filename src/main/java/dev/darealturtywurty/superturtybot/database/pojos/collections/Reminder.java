package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
    private String id;
    private long guild;
    private long user;
    private String reminder;
    private long channel;
    private long time;
    private long createdAt;
}
