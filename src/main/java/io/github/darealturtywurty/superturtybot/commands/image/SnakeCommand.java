package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SnakeCommand extends ImageCommand {
    public SnakeCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Random snake image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "snake";
    }
    
    @Override
    public String getRichName() {
        return "Snake Image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final Document page = Jsoup.connect("https://generatorfun.com/random-snake-image").get();
            event.getMessage().reply(page.select("img").first().attr("abs:src")).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been an issue gathering this snake image.").mentionRepliedUser(false)
                .queue();
        }
    }
}
