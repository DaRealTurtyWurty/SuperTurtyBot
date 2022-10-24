package dev.darealturtywurty.superturtybot.database.pojos.collections;

public class Report {
    private long guild;
    private long reported;
    private long reporter;
    private String reason;

    public Report() {
        this(0, 0, 0, "");
    }

    public Report(long guild, long reported, long reporter, String reason) {
        this.guild = guild;
        this.reported = reported;
        this.reporter = reporter;
        this.reason = reason;
    }

    public long getGuild() {
        return this.guild;
    }

    public long getReported() {
        return this.reported;
    }

    public long getReporter() {
        return this.reporter;
    }

    public String getReason() {
        return this.reason;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public void setReported(long reported) {
        this.reported = reported;
    }

    public void setReporter(long reporter) {
        this.reporter = reporter;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
