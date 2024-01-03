package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordleStreakData {
    private long guild;
    private int streak;
    private int bestStreak;
    private boolean hasPlayedToday;
}
