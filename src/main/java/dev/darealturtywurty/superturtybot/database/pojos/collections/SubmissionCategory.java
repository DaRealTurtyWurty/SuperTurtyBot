package dev.darealturtywurty.superturtybot.database.pojos.collections;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.Submission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionCategory {
    private List<Submission> submissions = new ArrayList<>();
    private String name;
    private String description;
    private long guild = - 1L;
    private boolean isAnonymous;
    private boolean isNSFW;
    private boolean isMedia;
    private boolean allowMultipleSubmissions = true;

    public SubmissionCategory(String name, String description, long guild, boolean isAnonymous, boolean isNSFW, boolean isMedia, boolean allowMultipleSubmissions) {
        this.name = name;
        this.description = description;
        this.guild = guild;
        this.isAnonymous = isAnonymous;
        this.isNSFW = isNSFW;
        this.isMedia = isMedia;
        this.allowMultipleSubmissions = allowMultipleSubmissions;
    }

    public SubmissionCategory(String name, String description, boolean isAnonymous, boolean isNSFW, boolean isMedia, boolean allowMultipleSubmissions) {
        this.name = name;
        this.description = description;
        this.isAnonymous = isAnonymous;
        this.isNSFW = isNSFW;
        this.isMedia = isMedia;
        this.allowMultipleSubmissions = allowMultipleSubmissions;
    }

    public boolean isGuildSpecific() {
        return this.guild != -1L;
    }

    public void addSubmission(Submission submission) {
        this.submissions.add(submission);
        Database.getDatabase().submissionCategories.updateOne(
                Filters.eq("name", this.name),
                Updates.addToSet("submissions", submission));
    }

    public void removeSubmission(Submission submission) {
        this.submissions.remove(submission);
    }

    public Optional<Submission> findSubmission(long userId) {
        if (this.isAnonymous)
            return Optional.empty();

        return this.submissions.stream()
                .filter(submission -> submission.getUserId() == userId)
                .findFirst();
    }
}
