package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MinecraftVersions;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TranslateCommand extends SubcommandCommand {

    public TranslateCommand() {
        super("translate", "Translate from one mapping to another");
        addOption(OptionType.STRING, "version", "The minecraft version", true, true);
        addOption(OptionType.STRING, "from", "The mapping to translate from", true, true);
        addOption(OptionType.STRING, "to", "The mapping to translate to", true, true);
        addOption(OptionType.STRING, "mapping", "The mapping to translate", true, true);
        addOption(OptionType.BOOLEAN, "ignore_classes", "Ignore classes", false);
        addOption(OptionType.BOOLEAN, "ignore_methods", "Ignore methods", false);
        addOption(OptionType.BOOLEAN, "ignore_fields", "Ignore fields", false);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!"mappings".equals(event.getName()) && !Objects.equals(event.getSubcommandName(), getName()))
            return;

        if (event.getFocusedOption().getName().equals("version")) {
            List<String> choices = MinecraftVersions.getPistonVersions();
            String focusedValue = event.getFocusedOption().getValue();
            List<String> filteredChoices = choices.stream()
                    .filter(choice -> choice.toLowerCase().startsWith(focusedValue.toLowerCase()))
                    .toList();

            event.replyChoiceStrings(filteredChoices.stream()
                    .limit(25)
                    .toList()).queue();
        }
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

    }
}
