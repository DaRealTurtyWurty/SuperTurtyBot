package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suggestion {
    private long guild;
    private long user;
    private long message;
    
    private long createdAt;
    private List<SuggestionResponse> responses;
    
    public Suggestion(long guildId, long authorId, long messageId, long createdAt) {
        this();
        this.guild = guildId;
        this.user = authorId;
        this.message = messageId;
        this.createdAt = createdAt;
    }
}
