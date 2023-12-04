package dev.darealturtywurty.superturtybot.modules.submission;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SubmissionManager extends ListenerAdapter {
    public static final SubmissionManager INSTANCE = new SubmissionManager();

    private SubmissionManager() {}
}
