package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GistManager extends ListenerAdapter {
    private static GitHubClient GITHUB;
    private static GistService GIST;

    static {
        Environment.INSTANCE.githubOAuthToken().ifPresentOrElse(token -> {
            GITHUB = new GitHubClient().setOAuth2Token(token);
            GIST = new GistService(GITHUB);
        }, () -> Constants.LOGGER.error("GitHub OAuth Token has not been set!"));
    }

    private static final List<String> ACCEPTED_EXTENSIONS = List.of("txt", "gradle", "log", "java", "txt", "kt",
            "groovy", "js", "json", "kts");

    public static final GistManager INSTANCE = new GistManager();

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (GITHUB == null || GIST == null || !event.getEmoji().getAsReactionCode().equals("📝"))
            return;

        if (!event.isFromGuild() || event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem())
            return;

        final Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        if (!config.isShouldCreateGists())
            return;

        event.retrieveMessage().queue(message -> {
            final List<Attachment> attachments = getValidAttachments(message.getAttachments());
            if (attachments.isEmpty())
                return;

            final var gistFiles = new HashMap<String, GistFile>();
            final CompletableFuture<Map<String, GistFile>> futureMap = new CompletableFuture<>();
            final var counter = new AtomicInteger();
            for (final Attachment attachment : attachments) {
                try {
                    final Path path = Files.createTempDirectory("TurtyBot").resolve(attachment.getFileName());
                    final var proxy = attachment.getProxy();
                    final CompletableFuture<Path> future = proxy.downloadToPath(path);
                    future.thenAccept(file -> {
                        try {
                            final String content = Files.readString(path);
                            if (!content.isBlank()) {
                                gistFiles.put(attachment.getFileName(),
                                        new GistFile().setContent(content).setFilename(attachment.getFileName()));
                            }
                        } catch (final IOException ignored) {
                        }

                        if (counter.incrementAndGet() >= attachments.size()) {
                            futureMap.complete(gistFiles);
                        }
                    }).exceptionally(exception -> {
                        Constants.LOGGER.error("Failed to download attachment!", exception);
                        if (counter.incrementAndGet() >= attachments.size()) {
                            futureMap.complete(gistFiles);
                        }
                        return null;
                    });
                } catch (final IOException ignored) {
                }
            }

            futureMap.thenAccept(files -> {
                if (files.isEmpty())
                    return;

                try {
                    final Gist gist = GIST
                            .createGist(new Gist().setDescription("").setPublic(false).setFiles(gistFiles));
                    final String url = gist.getHtmlUrl();
                    event.getReaction().clearReactions()
                            .queue(v -> message
                                    .reply("Gist created at the request of " + event.getUser().getAsMention() + "!\n" + url)
                                    .mentionRepliedUser(false).queue());
                } catch (final IOException exception) {
                    message.reply("There has been an error creating a gist for this file!").mentionRepliedUser(false)
                            .queue();
                    Constants.LOGGER.error("Failed to create gist!", exception);
                }
            });
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (GITHUB == null || GIST == null)
            return;

        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot()
                || event.getAuthor().isSystem())
            return;

        final Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        if (!config.isShouldCreateGists())
            return;

        final List<Attachment> attachments = event.getMessage().getAttachments();
        if (getValidAttachments(attachments).isEmpty())
            return;

        event.getMessage().addReaction(Emoji.fromFormatted("📝")).queue();
    }

    private static List<Attachment> getValidAttachments(List<Attachment> attachments) {
        if (attachments.isEmpty())
            return List.of();

        final List<Attachment> retVal = new ArrayList<>();
        for (final Attachment attachment : attachments) {
            if (!isValidAttachment(attachment)) {
                continue;
            }

            retVal.add(attachment);
        }

        return retVal;
    }

    private static boolean isValidAttachment(Attachment attachment) {
        return !attachment.isImage() && !attachment.isVideo() && attachment.getFileExtension() != null
                && ACCEPTED_EXTENSIONS.contains(attachment.getFileExtension().toLowerCase(Locale.ROOT));
    }
}
