package dev.darealturtywurty.superturtybot.modules.quest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestActivity {
    private long guild;
    private long user;
    private long weekStart;

    private String type;
    private String sourceId;
    private long occurredAt;

    private Document details;
}
