package dev.darealturtywurty.superturtybot.commands.fun.relationship;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RelationshipDateCommand extends SubcommandCommand {
    public RelationshipDateCommand() {
        super("date", "Date someone!");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if(!event.isFromGuild()) {
            reply(event, "❌ This command can only be used in servers!", false, true);
            return;
        }


    }
}
