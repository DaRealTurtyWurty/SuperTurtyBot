package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.dv8tion.jda.api.entities.Message;
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
import java.util.List;
import java.util.Locale;

public class FileConversionManager extends ListenerAdapter {
    public static final FileConversionManager INSTANCE = new FileConversionManager();

    private static final List<String> SUPPORTED_VIDEO_FORMATS =
            List.of("3g2", "3gp", "3gpp", "avi", "cavs", "dv", "dvr", "flv", "m2ts", "m4v", "mkv", "mod",
                    "mov", "mpeg", "mpg", "mts", "mxf", "rm", "rmvb", "swf", "ts", "vob", "wmv", "wtv");

    private static final List<String> SUPPORTED_AUDIO_FORMATS =
            List.of("aac", "aiff", "alac", "amr", "flac", "m4a", "wma", "mp2", "ac3", "aif", "aifc", "au",
                    "caf", "m4b", "oga", "voc", "weba");

    private static final List<String> SUPPORTED_IMAGE_FORMATS =
            List.of("3fr", "arw", "avif", "bmp", "cr2", "cr3", "crw", "dcr", "dng", "eps", "erf", "heic",
                    "heif", "icns", "ico", "jfif", "mos", "mrw", "nef", "odd", "odg", "orf", "pef", "ppm", "ps", "psd",
                    "raf", "raw", "rw2", "tif", "tiff", "x3f", "xcf", "xps");

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    public FileConversionManager() {
        try {
            this.ffmpeg = new FFmpeg();
            this.ffprobe = new FFprobe();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize FFmpeg!", exception);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
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

        message.addReaction(Emoji.fromFormatted("🔜")).queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(event.getUser() != null && event.getUser().isBot()) return;
        EmojiUnion emoji = event.getEmoji();
        if(!emoji.getAsReactionCode().equals("🔜")) return;

        event.retrieveMessage().queue(message -> {
            List<Message.Attachment> attachments = message.getAttachments();
            if (attachments.isEmpty())
                return;

            for (Message.Attachment attachment : attachments) {
                // determine if the file is a video
                String contentType = attachment.getContentType();
                if (contentType == null)
                    continue;

                contentType = contentType.toLowerCase(Locale.ROOT);

                String fileExtension = attachment.getFileExtension();
                if (fileExtension == null)
                    continue;

                fileExtension = fileExtension.toLowerCase(Locale.ROOT);

                String fileName = attachment.getFileName().split("\\.")[0];

                if (contentType.startsWith("video")) {
                    // determine if the file is a supported format
                    if (SUPPORTED_VIDEO_FORMATS.contains(fileExtension)) {
                        String output = System.getProperty("user.dir") + "/output/%s.mp4".formatted(fileName);
                        convertFFmpeg(message, attachment, output);
                    }
                } else if (contentType.startsWith("audio")) {
                    if (SUPPORTED_AUDIO_FORMATS.contains(fileExtension)) {
                        String output = System.getProperty("user.dir") + "/output/%s.mp3".formatted(fileName);

                        convertFFmpeg(message, attachment, output);
                    }
                }
//                else if (contentType.startsWith("image")) {
//                    if (SUPPORTED_IMAGE_FORMATS.contains(fileExtension)) {
//                        var builder = new FFmpegBuilder()
//                                .setInput(attachment.getUrl())
//                                .addOutput("output/%s.jpg".formatted(fileName))
//                                .done();
//
//                        var executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
//                        executor.createJob(builder).run();
//                    }
//                }
            }
        });
    }

    private void convertFFmpeg(Message message, Message.Attachment attachment, String output) {
        // download file
        Path inputFile = Path.of(System.getProperty("user.dir"), "input", attachment.getFileName());
        if (Files.notExists(inputFile.getParent())) {
            try {
                Files.createDirectories(inputFile.getParent());
            } catch (IOException ignored) {}
        }

        Path outputFile = Path.of(output);
        if (Files.notExists(outputFile.getParent())) {
            try {
                Files.createDirectories(outputFile.getParent());
            } catch (IOException ignored) {}
        }

        attachment.getProxy().downloadToPath(inputFile).thenAccept(file -> {
            var builder = new FFmpegBuilder()
                    .setInput(inputFile.toAbsolutePath().toString())
                    .addOutput(outputFile.toAbsolutePath().toString())
                    .done();

            var executor = new FFmpegExecutor(this.ffmpeg, this.ffprobe);
            executor.createJob(builder, progress -> {
                if (progress.isEnd()) {
                    message.clearReactions(Emoji.fromFormatted("🔜")).queue();

                    try {
                        // read output as input stream
                        byte[] bytes = Files.readAllBytes(outputFile);

                        // upload to discord
                        FileUpload upload = FileUpload.fromData(bytes, outputFile.getFileName().toString());
                        message.reply("✅ I have converted your file!")
                                .mentionRepliedUser(false)
                                .addFiles(upload)
                                .queue(ignored -> {
                                    // delete files
                                    try {
                                        Files.deleteIfExists(inputFile);
                                        Files.deleteIfExists(outputFile);
                                    } catch (IOException exception) {
                                        Constants.LOGGER.error("Failed to delete files!", exception);
                                    }
                                });

                        message.addReaction(Emoji.fromFormatted("✅")).queue();
                    } catch (IOException ignored) {
                        message.addReaction(Emoji.fromFormatted("❌")).queue();
                        message.reply("❌ Failed to convert file!").mentionRepliedUser(false).queue();

                        // delete files
                        try {
                            Files.deleteIfExists(inputFile);
                            Files.deleteIfExists(outputFile);
                        } catch (IOException exception2) {
                            Constants.LOGGER.error("Failed to delete files!", exception2);
                        }
                    }
                }
            }).run();
        });
    }
}
