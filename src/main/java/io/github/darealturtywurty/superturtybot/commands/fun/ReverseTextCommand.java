package io.github.darealturtywurty.superturtybot.commands.fun;

import java.util.List;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
    public String getName() {
        return "reversetext";
    }
    
    @Override
    public String getRichName() {
        return "Reverse Text";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String text = event.getOption("text").getAsString();
        event.deferReply().setContent(new StringBuilder(text).reverse().toString()).mentionRepliedUser(false).queue();
    }
}
