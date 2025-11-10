package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.*;

public class FileConversionManager extends ListenerAdapter {
    public static final FileConversionManager INSTANCE = new FileConversionManager();

    private static final List<String> SUPPORTED_VIDEO_FORMATS =
            List.of("3g2", "3gp", "3gpp", "avi", "cavs", "dv", "dvr", "flv", "m2ts", "m4v", "mkv", "mod",
                    "mov", "mpeg", "mpg", "mts", "mxf", "rm", "rmvb", "swf", "ts", "vob", "wmv", "wtv");

    private static final List<String> SUPPORTED_AUDIO_FORMATS =
            List.of("aac", "aiff", "alac", "amr", "flac", "m4a", "wma", "mp2", "ac3", "aif", "aifc", "au",
                    "caf", "m4b", "oga", "voc", "weba", "wav");

    private static final List<String> SUPPORTED_IMAGE_FORMATS =
            List.of("3fr", "arw", "avif", "bmp", "cr2", "cr3", "crw", "dcr", "dng", "eps", "erf", "heic",
                    "heif", "icns", "ico", "jfif", "mos", "mrw", "nef", "odd", "odg", "orf", "pef", "ppm", "ps", "psd",
                    "raf", "raw", "rw2", "tif", "tiff", "x3f", "xcf", "xps");

    private static final String CONVERT_EMOJI_CODE = "🔜";
    private static final Emoji CONVERT_EMOJI = Emoji.fromFormatted(CONVERT_EMOJI_CODE);

    private final Set<Long> conversionsInProgress = ConcurrentHashMap.newKeySet();
    private final ExecutorService conversionExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            runnable -> {
                final Thread thread = new Thread(runnable, "FileConversionManager");
                thread.setDaemon(true);
                return thread;
            });

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final boolean available;

    public FileConversionManager() {
        FFmpeg tmpFfmpeg = null;
        FFprobe tmpFfprobe = null;
        boolean initSuccess = true;
        try {
            tmpFfmpeg = new FFmpeg();
            tmpFfprobe = new FFprobe();
        } catch (IOException exception) {
            initSuccess = false;
            Constants.LOGGER.warn("FFmpeg binaries not available; file conversion disabled.", exception);
        }

        this.ffmpeg = tmpFfmpeg;
        this.ffprobe = tmpFfprobe;
        this.available = initSuccess;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.conversionExecutor.shutdown();
            try {
                if (!this.conversionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.conversionExecutor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "FileConversionManager-Shutdown"));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!this.available || !event.isFromGuild() || event.getAuthor().isBot())
            return;

        Message message = event.getMessage();

        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.isEmpty())
            return;

        boolean hasSupportedFormat = false;
        for (Message.Attachment attachment : attachments) {
            // if it's not a supported format, skip it
            String fileExtension = attachment.getFileExtension();
            if (fileExtension == null)
                continue;

            fileExtension = fileExtension.toLowerCase(Locale.ROOT);

            if (!SUPPORTED_VIDEO_FORMATS.contains(fileExtension) && !SUPPORTED_AUDIO_FORMATS.contains(fileExtension)
                    && !SUPPORTED_IMAGE_FORMATS.contains(fileExtension))
                continue;

            hasSupportedFormat = true;
        }

        if (!hasSupportedFormat)
            return;

        message.addReaction(CONVERT_EMOJI).queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!this.available || event.getUser() == null || event.getUser().isBot() || !event.isFromGuild())
            return;

        EmojiUnion emoji = event.getEmoji();
        if (!emoji.getAsReactionCode().equals(CONVERT_EMOJI_CODE))
            return;

        event.retrieveMessage().queue(message -> {
            final Member reactor = event.getMember();
            if (reactor == null || reactor.getUser().isBot())
                return;

            final User authorUser = message.getAuthor();
            if (authorUser.isBot())
                return;

            final boolean isAuthor = authorUser.getIdLong() == reactor.getIdLong();
            final boolean canForce = reactor.hasPermission(Permission.MESSAGE_MANAGE);
            if (!isAuthor && !canForce)
                return;

            if (!this.conversionsInProgress.add(message.getIdLong()))
                return;

            CompletableFuture.runAsync(() -> processAttachments(message, reactor.getIdLong()), this.conversionExecutor)
                    .whenComplete((unused, throwable) -> {
                        this.conversionsInProgress.remove(message.getIdLong());
                        message.removeReaction(CONVERT_EMOJI).queue(success -> {
                        }, failure -> {
                        });

                        if (throwable != null) {
                            Constants.LOGGER.error("Failed to convert attachment for {}", message.getIdLong(), throwable);
                            message.reply("❌ Something went wrong while converting your attachment. Please try again later.")
                                    .mentionRepliedUser(false)
                                    .queue();
                        }
                    });
        });
    }

    private void processAttachments(Message message, long requesterId) {
        final List<Message.Attachment> attachments = message.getAttachments();
        boolean foundConvertible = false;
        boolean convertedAny = false;

        for (Message.Attachment attachment : attachments) {
            final ConversionType conversionType = determineConversionType(attachment);
            if (conversionType == null)
                continue;

            foundConvertible = true;
            try {
                convertAttachment(message, attachment, conversionType);
                convertedAny = true;
            } catch (final IOException exception) {
                Constants.LOGGER.error("Failed to convert attachment {}", attachment.getFileName(), exception);
                message.reply("❌ Failed to convert `%s`: %s".formatted(attachment.getFileName(), exception.getMessage()))
                        .mentionRepliedUser(false)
                        .queue();
            }
        }

        if (!foundConvertible) {
            message.reply("ℹ️ I couldn't find any attachments in a supported format to convert.")
                    .mentionRepliedUser(false)
                    .queue();
        } else {
            if (convertedAny) {
                message.getAuthor();
                if (message.getAuthor().getIdLong() != requesterId) {
                    message.reply("<@%d> converted your attachments.".formatted(requesterId))
                            .mentionRepliedUser(false)
                            .queue();
                }
            }
        }
    }

    private void convertAttachment(Message message, Message.Attachment attachment, ConversionType type) throws IOException {
        final Path tempDir = Files.createTempDirectory("turtybot_conversion");
        final String extension = attachment.getFileExtension() != null ? attachment.getFileExtension().toLowerCase(Locale.ROOT) : "tmp";
        final Path inputFile = tempDir.resolve("input." + extension);
        final Path outputFile = tempDir.resolve("output" + type.outputExtension());

        try {
            attachment.getProxy().downloadToPath(inputFile).join();
        } catch (Exception exception) {
            cleanup(tempDir);
            throw new IOException("Failed to download attachment.", exception);
        }

        final FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile.toAbsolutePath().toString())
                .overrideOutputFiles(true);

        final FFmpegOutputBuilder outputBuilder = builder.addOutput(outputFile.toAbsolutePath().toString())
                .setFormat(type.outputFormat());

        switch (type) {
            case VIDEO -> outputBuilder.setVideoCodec("libx264")
                    .setAudioCodec("aac")
                    .setAudioBitRate(128_000)
                    .setVideoFrameRate(30, 1)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);
            case AUDIO -> outputBuilder.disableVideo()
                    .setAudioCodec("libmp3lame")
                    .setAudioBitRate(192_000);
            case IMAGE -> outputBuilder.disableAudio();
        }

        outputBuilder.done();

        final var executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
        try {
            executor.createJob(builder).run();
        } catch (RuntimeException exception) {
            cleanup(tempDir);
            throw new IOException("FFmpeg failed to convert file.", exception);
        }

        byte[] fileBytes = Files.readAllBytes(outputFile);

        final long maxFileSize = message.isFromGuild() ? message.getGuild().getMaxFileSize() : Message.MAX_FILE_SIZE;
        final long outputSize = fileBytes.length;
        message.reply("⏳ Converted `%s` (%s → %s)..."
                .formatted(attachment.getFileName(), humanReadableSize(attachment.getSize()), humanReadableSize(outputSize)))
                .mentionRepliedUser(false)
                .queue();
        if (outputSize > maxFileSize) {
            cleanup(tempDir);
            message.reply("⚠️ Converted file `%s` is %s which exceeds this server's upload limit of %s."
                    .formatted(attachment.getFileName(), humanReadableSize(outputSize), humanReadableSize(maxFileSize)))
                    .mentionRepliedUser(false)
                    .queue();
            return;
        }

        final String safeBaseName = sanitizeFileName(attachment.getFileName());
        final String deliveredName = safeBaseName + type.outputExtension();
        final FileUpload upload = FileUpload.fromData(outputFile, deliveredName);

        message.reply("✅ Converted `%s` to **.%s**.".formatted(attachment.getFileName(), type.outputExtension().substring(1)))
                .mentionRepliedUser(false)
                .addFiles(upload)
                .queue(msg -> cleanup(tempDir), error -> {
                    cleanup(tempDir);
                    Constants.LOGGER.error("Failed to upload converted file {}", deliveredName, error);
                    message.reply("❌ Discord rejected the converted file `%s`: %s".formatted(deliveredName, error.getMessage()))
                            .mentionRepliedUser(false)
                            .queue();
                });
    }

    private ConversionType determineConversionType(Message.Attachment attachment) {
        final String extension = attachment.getFileExtension() != null
                ? attachment.getFileExtension().toLowerCase(Locale.ROOT)
                : "";

        final String contentType = attachment.getContentType() != null
                ? attachment.getContentType().toLowerCase(Locale.ROOT)
                : "";

        if (contentType.startsWith("video") || SUPPORTED_VIDEO_FORMATS.contains(extension))
            return ConversionType.VIDEO;
        if (contentType.startsWith("audio") || SUPPORTED_AUDIO_FORMATS.contains(extension))
            return ConversionType.AUDIO;
        if (contentType.startsWith("image") || SUPPORTED_IMAGE_FORMATS.contains(extension))
            return ConversionType.IMAGE;

        return null;
    }

    private void cleanup(Path directory) {
        try (var stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private String sanitizeFileName(String original) {
        final String baseName = original.contains(".") ? original.substring(0, original.lastIndexOf('.')) : original;
        final String sanitized = baseName.replaceAll("[^a-zA-Z0-9-_]", "_");
        return sanitized.isBlank() ? "converted" : sanitized;
    }

    private String humanReadableSize(long bytes) {
        final String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return "%.2f %s".formatted(size, units[unitIndex]);
    }

    private enum ConversionType {
        VIDEO(".mp4", "mp4"),
        AUDIO(".mp3", "mp3"),
        IMAGE(".png", "png");

        private final String outputExtension;
        private final String outputFormat;

        ConversionType(String outputExtension, String outputFormat) {
            this.outputExtension = outputExtension;
            this.outputFormat = outputFormat;
        }

        public String outputExtension() {
            return this.outputExtension;
        }

        public String outputFormat() {
            return this.outputFormat;
        }
    }
}
