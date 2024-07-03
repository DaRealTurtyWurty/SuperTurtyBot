package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.modules.AIMessageResponder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class TokensCommand extends CoreCommand {
    public TokensCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Shows how many tokens you have left.";
    }

    @Override
    public String getName() {
        return "tokens";
    }

    @Override
    public String getRichName() {
        return "Tokens";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        int tokens = AIMessageResponder.INSTANCE.getTokens(event.getUser());
        reply(event, "✅ You have %d tokens left!".formatted(500 - tokens));
    }
}
