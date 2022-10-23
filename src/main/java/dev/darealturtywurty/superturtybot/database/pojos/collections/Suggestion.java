package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

import dev.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;

public class Suggestion {
    private long guild;
    private long user;
    private long message;
    
    private long createdAt;
    private List<SuggestionResponse> responses;
    
    public Suggestion() {
        this.guild = 0;
        this.user = 0;
        this.message = 0;
        
        this.createdAt = 0;
        this.responses = new ArrayList<>();
    }
    
    public Suggestion(long guildId, long authorId, long messageId, long createdAt) {
        this();
        this.guild = guildId;
        this.user = authorId;
        this.message = messageId;
        this.createdAt = createdAt;
    }
    
    public long getCreatedAt() {
        return this.createdAt;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public long getMessage() {
        return this.message;
    }
    
    public List<SuggestionResponse> getResponses() {
        return this.responses;
    }
    
    public long getUser() {
        return this.user;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setMessage(long message) {
        this.message = message;
    }
    
    public void setResponses(List<SuggestionResponse> responses) {
        this.responses = responses;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
}
