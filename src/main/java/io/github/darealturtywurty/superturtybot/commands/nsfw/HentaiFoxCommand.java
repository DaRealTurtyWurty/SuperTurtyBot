package io.github.darealturtywurty.superturtybot.commands.nsfw;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import okhttp3.Request;

public class HentaiFoxCommand extends NSFWCommand {
    public HentaiFoxCommand() {
        super(NSFWCategory.FAKE);
    }
    
    @Override
    public String getDescription() {
        return "Grabs some hentai fox images and other media";
    }
    
    @Override
    public String getName() {
        return "hfox";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getTextChannel().isNSFW())
            return;
        // Essential
        super.runNormalMessage(event);
        
        final var request = new Request.Builder().url("https://nekobot.xyz/api/image?type=hkitsune").build();
        try (var response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            final String result = Constants.GSON.fromJson(response.body().string(), JsonObject.class).get("message")
                .getAsString();
            event.getChannel()
                .sendMessageEmbeds(
                    new EmbedBuilder().setColor(Color.GREEN).setTimestamp(Instant.now()).setImage(result).build())
                .queue();
        } catch (final IOException exception) {
            final var strBuilder = new StringBuilder("```\n");
            strBuilder.append(exception.getLocalizedMessage() + "\n");
            strBuilder.append(ExceptionUtils.getStackTrace(exception));
            strBuilder.append("```");
            
            event.getChannel().sendMessage(
                "There was an issue retrieving some hentai foxes. Please report the following error to the bot owner:\n"
                    + strBuilder.toString())
                .queue(msg -> event.getMessage().delete().queue());
        }
    }
}
