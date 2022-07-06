package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DuckCommand extends ImageCommand {
    public DuckCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Gets a random duck image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "duck";
    }
    
    @Override
    public String getRichName() {
        return "Duck Image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final URLConnection connection = new URL("https://random-d.uk/api/v2/random").openConnection();
            final JsonObject result = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = result.get("url").getAsString();
            event.getMessage().reply(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been a problem gathering this duck image!").mentionRepliedUser(false)
                .queue();
        }
    }
}
