package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class SavedSongs {
    private long user;
    private Map<String, String> songs;

    public SavedSongs() {
        this(-1L, new HashMap<>());
    }

    public SavedSongs(long user) {
        this(user, new HashMap<>());
    }
}
