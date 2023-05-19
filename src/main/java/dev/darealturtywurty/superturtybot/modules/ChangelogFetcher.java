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

        System.out.println(this.changelog.size());
    }

    private long fetchLastStartTime() {
        if(Files.notExists(lastStartTimePath)) {
            try {
                Files.writeString(lastStartTimePath, "0", StandardOpenOption.CREATE_NEW);
            } catch(IOException exception) {
                Constants.LOGGER.error("Failed to create 'lastStartTime.txt' file", exception);
            }
        }

        try {
            return Long.parseLong(Files.readString(lastStartTimePath).trim());
        } catch(IOException exception) {
            Constants.LOGGER.error("Failed to read 'lastStartTime.txt' file", exception);
        }

        return 0;
    }

    private void fetchChangelog() {
        try {
            Process process = new ProcessBuilder("git", "log", "--pretty=format:%s", "--since=" + formatMillis(lastStartTime))
                    .directory(new File(System.getProperty("user.dir"))).start();

            // Convert InputStream to ReadableByteChannel
            ReadableByteChannel channel = Channels.newChannel(process.getInputStream());

            // Create a ByteBuffer to read from the channel
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // Create a CharsetDecoder to decode the ByteBuffer
            CharsetDecoder decoder = Charset.defaultCharset().newDecoder();

            // Read from the ReadableByteChannel and decode it into a String that is added to the changelog
            while(channel.read(buffer) != -1) {
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
        } catch(IOException exception) {
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
                Constants.LOGGER.debug("Changelog entry: " + entry.trim());
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

    public static String formatMillis(long milliseconds) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(milliseconds);
        return dateFormat.format(date);
    }
}
