package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.commands.moderation.ticket.ModmailBlockSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.ticket.ModmailCloseSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.ticket.ModmailCreateSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.ticket.ModmailUnblockSubcommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ModmailCommand extends CoreCommand {
    public ModmailCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new ModmailCreateSubcommand(),
                new ModmailCloseSubcommand(),
                new ModmailBlockSubcommand(),
                new ModmailUnblockSubcommand()
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getAccess() {
        return "Everyone for create, moderators for close/block/unblock";
    }

    @Override
    public String getDescription() {
        return "Create and manage private modmail tickets";
    }

    @Override
    public String getHowToUse() {
        return """
                /modmail create <message>
                /modmail close [reason]
                /modmail block <user> [reason]
                /modmail unblock <user>""";
    }

    @Override
    public String getName() {
        return "modmail";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "❌ You must provide a valid subcommand!", false, true);
    }
}
