package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PandaCommand extends ImageCommand {
    public PandaCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Random panda image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "panda";
    }
    
    @Override
    public String getRichName() {
        return "Panda Image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/panda").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.getMessage().reply(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been an issue gathering this panda image.").mentionRepliedUser(false)
                .queue();
        }
    }
}
