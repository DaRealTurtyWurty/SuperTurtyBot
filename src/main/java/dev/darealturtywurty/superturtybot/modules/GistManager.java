package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.privacy.ExternalSharingConfirmation;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyPreferences;
import dev.darealturtywurty.superturtybot.modules.privacy.TrackedGistService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GistManager extends ListenerAdapter {
    private static final int MAX_UPLOAD_BYTES = 8 * 1024 * 1024;
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

    public static GistService getGistService() {
        if (GIST == null)
            throw new IllegalStateException("GitHub Gists are not configured");

        return GIST;
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (GITHUB == null || GIST == null || !event.getEmoji().getAsReactionCode().equals("📝"))
            return;

        if (!event.isFromGuild() || event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem())
            return;

        long requesterId = event.getUserIdLong();
        if (PrivacyPreferences.isOptedOut(requesterId))
            return;

        final Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        if (!config.isShouldCreateGists())
            return;

        event.retrieveMessage().queue(message -> {
            long authorId = message.getAuthor().getIdLong();
            if (message.getAuthor().isBot()
                || message.getAuthor().isSystem() || message.isWebhookMessage()
                || PrivacyPreferences.isOptedOut(authorId) || PrivacyPreferences.isOptedOut(requesterId))
                return;

            final List<Attachment> attachments = getValidAttachments(message.getAttachments());
            if (attachments.isEmpty())
                return;

            message.reply(ExternalSharingConfirmation.INSTANCE.create(requesterId, authorId,
                "**Upload these attachments to GitHub Gist?**\nThis sends the filenames and contents of the "
                    + attachments.size() + " supported text attachments on [this message](" + message.getJumpUrl()
                    + ") to GitHub. The Gist will be **unlisted, not private**: anyone with its link can read or "
                    + "copy it. Its link will be posted in this channel. Deleting the Discord message does not "
                    + "delete the Gist. [GitHub privacy policy](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).",
                hook -> publishGist(message, requesterId, hook))).mentionRepliedUser(false).queue();
        });
    }

    private static void publishGist(Message original, long requesterId, InteractionHook hook) {
        PrivacyPreferences.requireOptionalData(requesterId);
        PrivacyPreferences.requireOptionalData(original.getAuthor().getIdLong());
        Message current = original.getChannel().retrieveMessageById(original.getIdLong()).complete();
        if (!ExternalSharingConfirmation.isUnchanged(original, current)
            || !GuildData.getOrCreateGuildData(current.getGuild()).isShouldCreateGists()) {
            hook.editOriginal("The message changed or Gists were disabled. Nothing was uploaded; start again.").queue();
            return;
        }

        long authorId = current.getAuthor().getIdLong();
        PrivacyPreferences.requireOptionalData(authorId);
        try {
            Map<String, GistFile> files = readFiles(getValidAttachments(current.getAttachments()));
            if (files.isEmpty()) {
                hook.editOriginal("There are no non-empty supported text files to upload.").queue();
                return;
            }

            Message latest = original.getChannel().retrieveMessageById(original.getIdLong()).complete();
            if (!ExternalSharingConfirmation.isUnchanged(original, latest)
                || !GuildData.getOrCreateGuildData(latest.getGuild()).isShouldCreateGists()) {
                hook.editOriginal("The message changed. Nothing was uploaded; start again.").queue();
                return;
            }

            Gist gist = new TrackedGistService(Database.getDatabase().mongoDatabase).create(authorId, requesterId,
                current.getGuild().getIdLong(), current.getChannel().getIdLong(), current.getIdLong(), files);
            hook.editOriginal("Gist created at the request of <@" + requesterId + ">:\n" + gist.getHtmlUrl())
                .setAllowedMentions(List.of()).queue();
        } catch (IOException exception) {
            hook.editOriginal("The files must be readable UTF-8 text with unique filenames and no more than "
                + "8 MiB combined. Nothing was uploaded; check the files and try again.").queue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            hook.editOriginal("The upload was interrupted before submission. Please try again.").queue();
        } catch (ExecutionException | TimeoutException exception) {
            hook.editOriginal("An attachment could not be downloaded. Nothing was uploaded; try again.").queue();
        }
    }

    private static Map<String, GistFile> readFiles(List<Attachment> attachments)
        throws IOException, InterruptedException, ExecutionException,
        TimeoutException {
        Map<String, GistFile> files = new LinkedHashMap<>();
        int remaining = MAX_UPLOAD_BYTES;
        for (Attachment attachment : attachments) {
            if (attachment.getSize() > remaining)
                throw new IOException("Upload limit exceeded");

            try (InputStream stream = attachment.getProxy().download().get(30, TimeUnit.SECONDS)) {
                byte[] bytes = stream.readNBytes(remaining + 1);
                if (bytes.length > remaining)
                    throw new IOException("Upload limit exceeded");

                remaining -= bytes.length;
                String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
                if (!content.isBlank()) {
                    String filename = attachment.getFileName();
                    if (files.containsKey(filename))
                        throw new IOException("Duplicate filenames");

                    files.put(filename, new GistFile().setContent(content).setFilename(filename));
                }
            }
        }

        return files;
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
            if (!isValidAttachment(attachment))
                continue;

            retVal.add(attachment);
        }

        return retVal;
    }

    private static boolean isValidAttachment(Attachment attachment) {
        return !attachment.isImage() && !attachment.isVideo() && attachment.getFileExtension() != null
            && ACCEPTED_EXTENSIONS.contains(attachment.getFileExtension().toLowerCase(Locale.ROOT));
    }
}
