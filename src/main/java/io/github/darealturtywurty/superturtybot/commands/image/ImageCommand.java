package io.github.darealturtywurty.superturtybot.commands.image;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ImageCommand extends AbstractImageCommand {
    public ImageCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.STRING, "type", "The image type to receive", true).setAutoComplete(true));
    }
    
    @Override
    public String getDescription() {
        return "Gets an image from the given type";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.MISC;
    }

    @Override
    public String getName() {
        return "image";
    }
    
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        super.onCommandAutoCompleteInteraction(event);
        if (!event.getName().equalsIgnoreCase(getName()))
            return;
        
        final List<String> allowed = ImageCommandRegistry.IMAGE_CMD_TYPES.getRegistry().entrySet().stream()
            .map(Entry::getKey).filter(str -> str.contains(event.getFocusedOption().getValue())).limit(25).toList();
        event.replyChoiceStrings(allowed).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String typeOption = event.getOption("type").getAsString();
        final Optional<ImageCommandType> allowed = ImageCommandRegistry.IMAGE_CMD_TYPES.getRegistry().entrySet()
            .stream().filter(entry -> entry.getKey().equalsIgnoreCase(typeOption)).map(Entry::getValue).findFirst();
        if (!allowed.isPresent()) {
            reply(event, "❌ `" + typeOption + "` is not a valid image type!", false, true);
            return;
        }
        
        final ImageCommandType type = allowed.get();
        type.getRunner().accept(event, type);
    }
}
