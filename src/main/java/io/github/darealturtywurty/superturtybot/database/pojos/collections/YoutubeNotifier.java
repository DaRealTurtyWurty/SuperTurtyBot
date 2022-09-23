package io.github.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

public class YoutubeNotifier {
    private long guild;
    private long channel;
    private String youtubeChannel;
    
    private String mention;

    private List<String> storedVideos;

    public YoutubeNotifier() {
        this(0L, 0L, "", "");
    }

    public YoutubeNotifier(long guild, long channel, String youtubeChannel, String mention) {
        this.guild = guild;
        this.channel = channel;
        this.youtubeChannel = youtubeChannel;
        
        this.mention = mention;
        this.storedVideos = new ArrayList<>();
    }

    public long getChannel() {
        return this.channel;
    }

    public long getGuild() {
        return this.guild;
    }
    
    public String getMention() {
        return this.mention;
    }
    
    public List<String> getStoredVideos() {
        return this.storedVideos;
    }
    
    public String getYoutubeChannel() {
        return this.youtubeChannel;
    }
    
    public void setChannel(long channel) {
        this.channel = channel;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setMention(String mention) {
        this.mention = mention;
    }
    
    public void setStoredVideos(List<String> storedVideos) {
        this.storedVideos = storedVideos;
    }
    
    public void setYoutubeChannel(String youtubeChannel) {
        this.youtubeChannel = youtubeChannel;
    }
}
