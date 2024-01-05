package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordleProfile {
    private long user;
    private List<WordleStreakData> streaks;

    public WordleProfile(final long user) {
        this(user, new ArrayList<>());
    }
}
