package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;

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
            Constants.LOGGER.info(new File("").getAbsoluteFile().toString());
            Constants.LOGGER.info(formatMillis(lastStartTime));
            Process process = new ProcessBuilder("git", "log", "--pretty=format:%s", "--since=" + formatMillis(lastStartTime)).directory(new File("").getAbsoluteFile()).start();

            // print command and directory
            Constants.LOGGER.info(process.info().command().toString());
            Constants.LOGGER.info(process.info().commandLine().toString());
            Constants.LOGGER.info(process.info().arguments().toString());

            // Convert InputStream to ReadableByteChannel
            ReadableByteChannel channel = Channels.newChannel(process.getInputStream());

            // Create a ByteBuffer to read from the channel
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // Create a CharsetDecoder to decode the ByteBuffer
            CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

            // Read from the ReadableByteChannel and decode it into a String that is added to the changelog
            while (channel.read(buffer) != -1) {
                buffer.flip();

                CharBuffer decoded = decoder.decode(buffer);
                this.changelog.add(decoded.toString());

                buffer.clear();
            }

            // Tell the process to wait
            process.waitFor();
        } catch (IOException | InterruptedException exception) {
            Constants.LOGGER.error("Failed to fetch changelog", exception);
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
            if (sb.length() + entry.length() <= 2000 - 273) {
                sb.append("\\- ").append(entry.trim()).append(System.lineSeparator());
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
