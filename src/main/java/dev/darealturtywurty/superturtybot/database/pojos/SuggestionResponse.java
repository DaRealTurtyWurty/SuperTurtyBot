package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    private String type;
    private String content;
    private long responder;
    private long respondedAt;
    
    public Type asType() {
        return Type.valueOf(this.type);
    }

    public enum Type {
        APPROVED("Approved", Color.GREEN),
        DENIED("Denied", Color.RED),
        CONSIDERED("Considered", Color.ORANGE);
        
        public final String richName;
        public final Color color;
        
        Type(String richName, Color color) {
            this.richName = richName;
            this.color = color;
        }
    }
}
