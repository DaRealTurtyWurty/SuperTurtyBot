package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.SteamAppNews;
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

    private SteamAppNews previousData;
}
