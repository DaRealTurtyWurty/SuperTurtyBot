package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StickyMessage {
    private long guild;
    private long channel;
    private long owner;
    private String content;
    private String embed;
    private long postedMessage;
    private long updatedAt;

    public StickyMessage(long guild, long channel, long owner, String content, String embed) {
        this(guild, channel, owner, content, embed, 0L, System.currentTimeMillis());
    }

    public boolean hasEmbed() {
        return this.embed != null && !this.embed.isBlank();
    }

    public boolean hasText() {
        return this.content != null && !this.content.isBlank();
    }
}
