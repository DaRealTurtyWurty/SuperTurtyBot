package io.github.darealturtywurty.superturtybot.database.pojos.collections;

public class Showcase {
    private long guild;
    private long channel;
    private long message;
    private long user;

    private int stars;
    private long starboardMessage;
    
    public Showcase() {
        this(0, 0, 0, 0, 0);
    }
    
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
    
    public long getChannel() {
        return this.channel;
    }
    
    public long getGuild() {
        return this.guild;
    }

    public long getMessage() {
        return this.message;
    }

    public long getStarboardMessage() {
        return this.starboardMessage;
    }

    public int getStars() {
        return this.stars;
    }

    public long getUser() {
        return this.user;
    }

    public void setChannel(long channel) {
        this.channel = channel;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public void setMessage(long message) {
        this.message = message;
    }

    public void setStarboardMessage(long starboardMessage) {
        this.starboardMessage = starboardMessage;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public void setUser(long user) {
        this.user = user;
    }
}
