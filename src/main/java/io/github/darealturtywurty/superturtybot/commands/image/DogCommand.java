package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DogCommand extends ImageCommand {
    public DogCommand() {
        super(new Types(true, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Random dog image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "dog";
    }
    
    @Override
    public String getRichName() {
        return "Dog Image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final URLConnection connection = new URL("https://dog.ceo/api/breeds/image/random").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("message").getAsString();
            event.getMessage().reply(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been an issue gathering this dog image.").mentionRepliedUser(false)
                .queue();
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        try {
            final URLConnection connection = new URL("https://dog.ceo/api/breeds/image/random").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("message").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("There has been an issue gathering this dog image.")
                .mentionRepliedUser(false).queue();
        }
    }
}
