package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ImageCommand extends CoreCommand {
    public ImageCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List
                .of(new OptionData(OptionType.STRING, "type", "The image type to receive", true).setAutoComplete(true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Gets an image from the given type";
    }

    @Override
    public String getHowToUse() {
        return "/image [type]";
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        super.onCommandAutoCompleteInteraction(event);
        if (!event.getName().equalsIgnoreCase(getName()))
            return;

        final List<String> allowed = ImageCommandRegistry.getImageCommandTypes().getRegistry().keySet().stream()
                .filter(str -> str.contains(event.getFocusedOption().getValue())).limit(25).toList();
        event.replyChoiceStrings(allowed).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String typeOption = event.getOption("type", "", OptionMapping::getAsString);
        final Optional<ImageCommandType> allowed = ImageCommandRegistry.getImageCommandTypes().getRegistry().entrySet()
                .stream().filter(entry -> entry.getKey().equalsIgnoreCase(typeOption)).map(Entry::getValue).findFirst();
        if (allowed.isEmpty()) {
            reply(event, "❌ `" + typeOption + "` is not a valid image type!", false, true);
            return;
        }

        final ImageCommandType type = allowed.get();
        type.getRunner().accept(event, type);
    }
}
