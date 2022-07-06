package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class BunnyCommand extends ImageCommand {
    private static final Random RANDOM = new Random();
    
    public BunnyCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Random bunny image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "bunny";
    }
    
    @Override
    public String getRichName() {
        return "Bunny Image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final URLConnection connection = new URL(
                RANDOM.nextBoolean() ? "https://api.bunnies.io/v2/loop/random/?media=mp4"
                    : "https://api.bunnies.io/v2/loop/random/?media=gif").openConnection();
            final JsonObject result = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = result.get("media").getAsJsonObject().get("poster").getAsString();
            event.getMessage().reply(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been an issue gathering this bunny image.").mentionRepliedUser(false)
                .queue();
        }
    }
}
