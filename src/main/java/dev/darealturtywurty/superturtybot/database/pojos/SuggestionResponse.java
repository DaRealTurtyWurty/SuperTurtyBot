package dev.darealturtywurty.superturtybot.database.pojos;

import java.awt.Color;

public class SuggestionResponse {
    private String type;
    private String content;
    private long responder;
    private long respondedAt;
    
    public SuggestionResponse() {
        this.type = "";
        this.content = "";
    }
    
    public SuggestionResponse(Type type, String content, long responder, long respondedAt) {
        this.type = type.name();
        this.content = content;
        this.responder = responder;
        this.respondedAt = respondedAt;
    }
    
    public Type asType() {
        return Type.valueOf(this.type);
    }
    
    public String getContent() {
        return this.content;
    }
    
    public long getRespondedAt() {
        return this.respondedAt;
    }
    
    public long getResponder() {
        return this.responder;
    }
    
    public String getType() {
        return this.type;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }
    
    public void setResponder(long responder) {
        this.responder = responder;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public enum Type {
        APPROVED("Approved", Color.GREEN), DENIED("Denied", Color.RED), CONSIDERED("Considered", Color.ORANGE);
        
        public final String richName;
        public final Color color;
        
        Type(String richName, Color color) {
            this.richName = richName;
            this.color = color;
        }
    }
}
