package dev.darealturtywurty.superturtybot.commands.moderation.ticket;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.modules.modmail.ModmailManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public abstract class ModmailSubcommand extends SubcommandCommand {
    protected ModmailSubcommand(String name, String description) {
        super(name, description);
    }

    protected static Member requireGuildMember(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return null;
        }

        return event.getMember();
    }

    protected static boolean validateModerator(SlashCommandInteractionEvent event) {
        Member member = requireGuildMember(event);
        if (member == null)
            return false;

        if (!ModmailManager.isModerator(member)) {
            reply(event, "❌ You do not have permission to manage tickets!", false, true);
            return false;
        }

        return true;
    }

    protected static OptionData reasonOption(boolean required, String description) {
        return new OptionData(OptionType.STRING, "reason", description, required);
    }
}
