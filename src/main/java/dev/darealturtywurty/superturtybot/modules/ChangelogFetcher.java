package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Getter;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class ChangelogFetcher {
    public static final ChangelogFetcher INSTANCE = new ChangelogFetcher();

    private final List<String> changelog = new ArrayList<>();
    private final long startTime;

    private ChangelogFetcher() {
        this.startTime = System.currentTimeMillis();
        fetchChangelog();
        saveLastStartTime();

        Constants.LOGGER.info("{} git changes found!", this.changelog.size());
    }

    private void fetchChangelog() {
        Path gitPath = Paths.get(".git");
        boolean deleteAfter = false;
        if(Files.notExists(gitPath) || !Files.isDirectory(gitPath)) {
            gitPath = Paths.get("../SuperTurtyBotGit");
            if(Files.notExists(gitPath) || !Files.isDirectory(gitPath)) {
                try {
                    Files.createDirectories(gitPath);
                } catch (IOException exception) {
                    Constants.LOGGER.error("Failed to create git directory", exception);
                    return;
                }

                deleteAfter = true;

                try(Git ignored = Git.cloneRepository()
                        .setURI("https://github.com/DaRealTurtyWurty/SuperTurtyBot.git")
                        .setDirectory(gitPath.toFile())
                        .call()) {
                    Constants.LOGGER.info("Cloned git repository");
                } catch (GitAPIException exception) {
                    Constants.LOGGER.error("Failed to clone git repository", exception);
                    return;
                }
            }
        }

        try(Git git = Git.open(gitPath.toFile())) {
            Iterable<RevCommit> logs = git.log().setRevFilter(CommitTimeRevFilter.after(TurtyBot.getLastStartTime())).call();
            for (RevCommit commit : logs) {
                String message = commit.getShortMessage();

                if (isDependabotCommit(commit.getAuthorIdent())) {
                    try {
                        Constants.LOGGER.debug("Found dependabot commit: {}", message);

                        String dependency = message.split("from")[0]
                                .replace("Bumps ", "")
                                .replace("Merges ", "")
                                .replace("Updates ", "")
                                .trim();

                        String fromVersion = message.split("from")[1].split("to")[0].trim();
                        String toVersion = message.split("to")[1].split("- ")[0].trim();

                        message = "Updated %s from %s to %s".formatted(dependency, fromVersion, toVersion);
                    } catch (IndexOutOfBoundsException exception) {
                        message = "Updated a dependency";
                    }
                } else if (message.startsWith("Merge")) continue;

                Date date = commit.getAuthorIdent().getWhen();
                String commitMessage = "\\- %s: %s".formatted(TimeFormat.RELATIVE.format(date.toInstant()), message.replace("\n-", "\\-").replace("\n*", "\\*"));
                this.changelog.add(commitMessage);
            }
        } catch (GitAPIException exception) {
            Constants.LOGGER.error("A git error occurred fetching the changelog", exception);
        } catch (IOException exception) {
            Constants.LOGGER.error("An IO error occurred fetching the changelog", exception);
        }

        if(deleteAfter) {
            try {
                FileUtils.deleteDirectory(gitPath.toFile());
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to delete git directory", exception);
            }
        }
    }

    // format:
    // Bumps com.github.oshi:oshi-core from 6.4.6 to 6.4.7.- Release notes- Changelog- Commits
    // Bumps net.dv8tion:JDA from 5.0.0-beta.16 to 5.0.0-beta.17.- Release notes- Commits
    private static boolean isDependabotCommit(PersonIdent author) {
        return author.getName().equals("dependabot[bot]") || author.getName().equals("dependabot-preview[bot]");
    }

    private void saveLastStartTime() {
        try {
            Files.writeString(TurtyBot.getLastStartTimePath(), String.valueOf(this.startTime), StandardOpenOption.WRITE);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to save last start time", exception);
        }
    }

    public String getFormattedChangelog() {
        var sb = new StringBuilder();
        for (String entry : this.changelog) {
            // Check if adding the current entry would exceed the character limit
            if (sb.length() + entry.length() <= 1700) {
                sb.append(entry.trim()).append(System.lineSeparator());
            } else {
                // If adding the current entry would exceed the limit, break the loop
                break;
            }
        }

        return sb.toString();
    }

    public String appendChangelog(String startupMessage) {
        if (this.changelog.isEmpty())
            return startupMessage;

        startupMessage += " Here is what's changed since we last spoke:%n%s".formatted(this.getFormattedChangelog());
        return startupMessage;
    }
}
