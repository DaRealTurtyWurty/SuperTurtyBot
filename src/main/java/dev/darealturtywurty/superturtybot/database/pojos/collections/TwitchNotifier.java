package dev.darealturtywurty.superturtybot.database.pojos.collections;

public class TwitchNotifier {
    private long guild;
    private String channel;

    private long discordChannel;
    private String mention;

    public TwitchNotifier() {
        this(0L, "", 0L, "");
    }
    
    public TwitchNotifier(long guild, String channel, long discordChannel, String mention) {
        this.guild = guild;
        this.channel = channel;

        this.discordChannel = discordChannel;
        this.mention = mention;
    }
    
    public String getChannel() {
        return this.channel;
    }
    
    public long getDiscordChannel() {
        return this.discordChannel;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public String getMention() {
        return this.mention;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public void setDiscordChannel(long discordChannel) {
        this.discordChannel = discordChannel;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setMention(String mention) {
        this.mention = mention;
    }
}
