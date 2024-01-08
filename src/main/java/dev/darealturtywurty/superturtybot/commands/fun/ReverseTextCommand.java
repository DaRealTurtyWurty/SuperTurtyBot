package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ReverseTextCommand extends CoreCommand {
    public ReverseTextCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "text", "The text to put in reverse", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Puts the given piece of text in reverse";
    }

    @Override
    public String getHowToUse() {
        return "/reversetext [text]";
    }

    @Override
    public String getName() {
        return "reversetext";
    }

    @Override
    public String getRichName() {
        return "Reverse Text";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String text = event.getOption("text", "", OptionMapping::getAsString);
        reply(event, new StringBuilder(text).reverse().toString(), false);
    }
}
