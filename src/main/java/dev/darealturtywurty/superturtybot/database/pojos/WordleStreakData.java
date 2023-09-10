package dev.darealturtywurty.superturtybot.database.pojos;

public class WordleStreakData {
    private long guild;
    private int streak;
    private int bestStreak;
    private boolean hasPlayedToday;

    public WordleStreakData(long guild, int streak, int bestStreak, boolean hasPlayedToday) {
        this.guild = guild;
        this.streak = streak;
        this.bestStreak = bestStreak;
        this.hasPlayedToday = hasPlayedToday;
    }

    public WordleStreakData() {
        this(0, 0, 0, false);
    }

    public long getGuild() {
        return guild;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public boolean isHasPlayedToday() {
        return hasPlayedToday;
    }

    public void setHasPlayedToday(boolean hasPlayedToday) {
        this.hasPlayedToday = hasPlayedToday;
    }
}
