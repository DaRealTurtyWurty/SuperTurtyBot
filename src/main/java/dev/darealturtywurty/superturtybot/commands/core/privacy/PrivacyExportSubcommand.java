package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyDataService;
import net.dv8tion.jda.api.utils.FileUpload;
import java.util.List;

public final class PrivacyExportSubcommand extends SubcommandCommand {
    private final PrivacyOperations operations;

    public PrivacyExportSubcommand(PrivacyOperations operations) {
        super("export", "Download a private ZIP of your stored records across servers");
        this.operations = operations;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (!operations.tryBegin(userId)) {
            reply(event, "A privacy operation is already running for your account. Please wait.", false, true);
            return;
        }
        if (!operations.tryAcquireExport()) {
            operations.finish(userId);
            reply(event, "Exports are busy. Please try again shortly.", false, true);
            return;
        }
        if (!operations.tryBeginExportCooldown(userId)) {
            operations.releaseExport();
            operations.finish(userId);
            reply(event, "Please wait one minute between operations.exports.", false, true);
            return;
        }
        event.deferReply(true).queue(hook -> Thread.ofVirtual().name("privacy-export").start(() -> {
            try {
                byte[] data = PrivacyOperations.service().exportUser(userId);
                hook.editOriginal("Your private data export is attached. Keep it somewhere safe. "
                    + "The included scope notes explain exclusions and how to request additional records.")
                    .setAllowedMentions(List.of())
                    .setAttachments(FileUpload.fromData(data, "turtybot-data.zip"))
                    .queue(message -> {
                    }, error -> hook.editOriginal("The export was prepared but Discord could not "
                        + "deliver the attachment. Try again later or email turtybot@gmail.com.").queue());
            } catch (PrivacyDataService.ExportTooLargeException exception) {
                hook.editOriginal(exception.getMessage()).queue();
            } catch (Exception exception) {
                // Never log exported contents or database exception details, which can contain personal data.
                Constants.LOGGER.warn("Privacy export failed ({})", exception.getClass().getSimpleName());
                hook.editOriginal("The export could not complete. No data was deleted. "
                    + "Try again later or email turtybot@gmail.com.").queue();
            } finally {
                operations.releaseExport();
                operations.finish(userId);
            }
        }), error -> {
            operations.releaseExport();
            operations.finish(userId);
            operations.clearExportCooldown(userId);
        });
    }
}
