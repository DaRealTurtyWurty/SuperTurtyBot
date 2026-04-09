package dev.darealturtywurty.superturtybot.commands.moderation.ticket;

import dev.darealturtywurty.superturtybot.modules.modmail.ModmailManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class ModmailBlockSubcommand extends ModmailSubcommand {
    public ModmailBlockSubcommand() {
        super("block", "Block a user from creating future tickets");
        addOption(OptionType.USER, "user", "The user to block from creating tickets", true);
        addOption(reasonOption(false, "Why this user is being blocked"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateModerator(event))
            return;

        Member member = event.getMember();
        if (member == null || event.getGuild() == null)
            return;

        User target = event.getOption("user", OptionMapping::getAsUser);
        if (target == null) {
            reply(event, "❌ You must provide a valid user to block.", false, true);
            return;
        }

        String reason = event.getOption("reason", "", OptionMapping::getAsString);
        ModmailManager.blockUser(event.getGuild().getIdLong(), target.getIdLong(), member.getIdLong(), reason);
        reply(event, "✅ " + target.getAsMention() + " can no longer create tickets in this server.", false, true);
    }
}
