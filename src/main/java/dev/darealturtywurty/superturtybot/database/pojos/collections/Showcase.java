package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Showcase {
    private long guild;
    private long channel;
    private long message;
    private long user;

    private int stars;
    private long starboardMessage;
    
    public Showcase(long guildId, long channelId, long messageId, long userId) {
        this(guildId, channelId, messageId, userId, 1);
    }

    public Showcase(long guildId, long channelId, long messageId, long userId, int stars) {
        this.guild = guildId;
        this.channel = channelId;
        this.message = messageId;
        this.user = userId;
        
        this.stars = stars;
        this.starboardMessage = 0;
    }
}
