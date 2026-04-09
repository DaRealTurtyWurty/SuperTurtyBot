package dev.darealturtywurty.superturtybot.commands.moderation.ticket;

import dev.darealturtywurty.superturtybot.modules.modmail.ModmailManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class ModmailUnblockSubcommand extends ModmailSubcommand {
    public ModmailUnblockSubcommand() {
        super("unblock", "Remove a ticket creation block from a user");
        addOption(OptionType.USER, "user", "The user to unblock", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateModerator(event))
            return;

        if (event.getGuild() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        User target = event.getOption("user", OptionMapping::getAsUser);
        if (target == null) {
            reply(event, "❌ You must provide a valid user to unblock.", false, true);
            return;
        }

        if (!ModmailManager.unblockUser(event.getGuild().getIdLong(), target.getIdLong())) {
            reply(event, "❌ That user is not currently blocked from creating tickets.", false, true);
            return;
        }

        reply(event, "✅ " + target.getAsMention() + " can create tickets again.", false, true);
    }
}
