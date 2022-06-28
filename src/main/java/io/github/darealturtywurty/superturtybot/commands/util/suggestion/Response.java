package io.github.darealturtywurty.superturtybot.commands.util.suggestion;

import java.awt.Color;

import io.github.darealturtywurty.superturtybot.commands.util.suggestion.Response.Type;

@SuppressWarnings("unused")
public record Response(Type type, String content, long responderId, long respondedAt) {
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
