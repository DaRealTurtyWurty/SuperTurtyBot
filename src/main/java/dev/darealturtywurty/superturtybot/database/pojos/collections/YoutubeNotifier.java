package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeNotifier {
    private long guild;
    private long channel;
    private String youtubeChannel;
    
    private String mention;
    private List<String> storedVideos;

    public YoutubeNotifier(long guild, long channel, String youtubeChannel, String mention) {
        this(guild, channel, youtubeChannel, mention, new ArrayList<>());
    }
}
