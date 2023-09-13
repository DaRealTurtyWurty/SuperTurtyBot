package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;

import java.util.ArrayList;
import java.util.List;

public class WordleProfile {
    private long user;
    private List<WordleStreakData> streaks;

    public WordleProfile(long user, List<WordleStreakData> streaks) {
        this.user = user;
        this.streaks = streaks;
    }

    public WordleProfile(long user) {
        this(user, new ArrayList<>());
    }

    public WordleProfile() {
        this(0);
    }

    public long getUser() {
        return user;
    }

    public List<WordleStreakData> getStreaks() {
        return streaks;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public void setStreaks(List<WordleStreakData> streaks) {
        this.streaks = streaks;
    }
}
