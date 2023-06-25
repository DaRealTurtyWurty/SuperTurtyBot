package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SavedSongs {
    private long user;

    private Map<String, String> songs;

    public SavedSongs() {
        this(-1L, new HashMap<>());
    }

    public SavedSongs(final long user, final Map<String, String> songs) {
        this.user = user;
        this.songs = songs;
    }

    public SavedSongs(long user) {
        this(user, new HashMap<>());
    }

    public Map<String, String> getSongs() {
        return this.songs;
    }

    public long getUser() {
        return this.user;
    }

    public void setSongs(final HashMap<String, String> songs) {
        this.songs = songs;
    }

    public void setUser(final long user) {
        this.user = user;
    }
}
