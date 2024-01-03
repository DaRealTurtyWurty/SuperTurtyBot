package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LatexCommand extends CoreCommand {
    public LatexCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "latex", "The latex to convert to an image", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Converts the provided latex into a latex image";
    }

    @Override
    public String getName() {
        return "latex";
    }

    @Override
    public String getRichName() {
        return "Latex";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String latex = event.getOption("latex", null, OptionMapping::getAsString);
        if (latex == null || latex.isBlank()) {
            reply(event, "❌ You must provide some latex to convert!", false, true);
            return;
        }

        reply(event, "https://latex.codecogs.com/png.latex?%5Cdpi%7B300%7D%20%5Cbg_white%20" + latex);
    }
}
