package dev.darealturtywurty.superturtybot.commands.fun;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

public class AdviceCommand extends CoreCommand {
    public AdviceCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "Grabs some advice";
    }
    
    @Override
    public String getName() {
        return "advice";
    }
    
    @Override
    public String getRichName() {
        return "Advice";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        try {
            final URLConnection connection = new URI("https://api.adviceslip.com/advice").toURL().openConnection();
            final JsonObject json = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            if (!json.has("slip")) {
                event.deferReply(true)
                    .setContent("There appears to be an issue processing this command! Please try again later.")
                    .mentionRepliedUser(false).queue();
                return;
            }
            
            final String advice = json.getAsJsonObject("slip").get("advice").getAsString();
            event.deferReply().setContent(advice).mentionRepliedUser(false).queue();
        } catch (final IOException | URISyntaxException exception) {
            event.deferReply(true)
                .setContent("There appears to be an issue processing this command! Please try again later.")
                .mentionRepliedUser(false).queue();
            Constants.LOGGER.error("An error occurred while running the advice command!", exception);
        }
    }
}
