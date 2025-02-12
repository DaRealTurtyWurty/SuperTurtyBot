package dev.darealturtywurty.superturtybot.database.pojos.collections;

import com.lukaspradel.steamapi.data.json.appnews.Newsitem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SteamNotifier {
    private long guild;
    private long channel;
    private int appId;
    private String mention;

    private Newsitem previousData;

    public SteamNotifier(long guild, long channel, int appId, String mention) {
        this.guild = guild;
        this.channel = channel;
        this.appId = appId;
        this.mention = mention;
    }
}
