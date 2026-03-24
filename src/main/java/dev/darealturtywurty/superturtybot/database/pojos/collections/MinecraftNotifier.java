package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinecraftNotifier {
    private long guild;
    private long channel;
    private String mention;
    private List<String> storedArticles;

    public MinecraftNotifier(long guild, long channel, String mention) {
        this(guild, channel, mention, new ArrayList<>());
    }
}
