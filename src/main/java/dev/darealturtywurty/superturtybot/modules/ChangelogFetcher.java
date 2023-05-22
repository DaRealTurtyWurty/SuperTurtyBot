package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChangelogFetcher {
    public static final ChangelogFetcher INSTANCE = new ChangelogFetcher();
    private final Path lastStartTimePath = Paths.get(System.getProperty("user.dir"), "lastStartTime.txt");

    private final List<String> changelog = new ArrayList<>();
    private final long startTime, lastStartTime;

    private ChangelogFetcher() {
        this.startTime = System.currentTimeMillis();
        this.lastStartTime = fetchLastStartTime();
        fetchChangelog();
        saveLastStartTime();

        Constants.LOGGER.info(this.changelog.size() + " git changes found!");
    }

    private long fetchLastStartTime() {
        if (Files.notExists(lastStartTimePath)) {
            try {
                Files.writeString(lastStartTimePath, "0", StandardOpenOption.CREATE_NEW);
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to create 'lastStartTime.txt' file", exception);
            }
        }

        try {
            return Long.parseLong(Files.readString(lastStartTimePath).trim());
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read 'lastStartTime.txt' file", exception);
        }

        return 0;
    }

    private void fetchChangelog() {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repo = builder.setGitDir(new File(".git")).setMustExist(true).build();
            Git git = new Git(repo);
            Iterable<RevCommit> logs = git.log().setRevFilter(CommitTimeRevFilter.after(this.lastStartTime)).call();
            for (RevCommit commit : logs) {
                String message = commit.getFullMessage();
                if (message.startsWith("Merge")) continue;

                Date date = commit.getAuthorIdent().getWhen();
                String commitMessage = "\\- %s: %s".formatted(TimeFormat.RELATIVE.format(date.toInstant()), message.replace("\n-", "\\-").replace("\n*", "\\*"));
                this.changelog.add(commitMessage);
            }
        } catch (IOException | GitAPIException exception) {
            Constants.LOGGER.error("Failed to fetch git changes", exception);
        }
    }

    private void saveLastStartTime() {
        try {
            Files.writeString(lastStartTimePath, String.valueOf(this.startTime), StandardOpenOption.WRITE);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to save 'lastStartTime.txt' file", exception);
        }
    }

    public List<String> getChangelog() {
        return this.changelog;
    }

    public String getFormattedChangelog() {
        StringBuilder sb = new StringBuilder();
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

    public long getStartTime() {
        return this.startTime;
    }

    public long getLastStartTime() {
        return this.lastStartTime;
    }

    public static String formatMillis(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"")
                .withZone(ZoneId.of("UTC"));
        return formatter.format(instant);
    }

    public String appendChangelog(String startupMessage) {
        if (this.changelog.isEmpty()) return startupMessage;

        startupMessage += " Here is what's changed since we last spoke:%n%s".formatted(this.getFormattedChangelog());
        return startupMessage;
    }
}
