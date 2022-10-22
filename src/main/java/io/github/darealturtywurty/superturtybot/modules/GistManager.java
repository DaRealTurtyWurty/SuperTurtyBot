package io.github.darealturtywurty.superturtybot.modules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GistManager extends ListenerAdapter {
    private static final GitHubClient GITHUB = new GitHubClient()
        .setOAuth2Token(Environment.INSTANCE.githubOAuthToken());
    private static final GistService GIST = new GistService(GITHUB);
    private static final List<String> ACCEPTED_EXTENSIONS = List.of("txt", "gradle", "log", "java", "txt", "kt",
        "groovy", "js", "json", "kts");

    public static final GistManager INSTANCE = new GistManager();
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild() || event.getUser().isBot() || event.getUser().isSystem())
            return;

        final Guild guild = event.getGuild();
        final GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
            .first();
        if (config == null || !config.shouldCreateGists())
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
                        if (Files.exists(file) && Files.isRegularFile(path) && Files.isReadable(path)) {
                            try {
                                final String content = Files.readString(path);
                                if (!content.isBlank()) {
                                    gistFiles.put(attachment.getFileName(),
                                        new GistFile().setContent(content).setFilename(attachment.getFileName()));
                                }
                            } catch (final IOException ignored) {
                            }
                        }
                        
                        if (counter.incrementAndGet() >= attachments.size()) {
                            futureMap.complete(gistFiles);
                        }
                    }).exceptionally(exception -> {
                        exception.printStackTrace();
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
                    exception.printStackTrace();
                }
            });
        });
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot()
            || event.getAuthor().isSystem())
            return;
        
        final Guild guild = event.getGuild();
        final GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
            .first();
        if (config == null || !config.shouldCreateGists())
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
        return !attachment.isImage() && !attachment.isVideo()
            && ACCEPTED_EXTENSIONS.contains(attachment.getFileExtension().toLowerCase());
    }
}
