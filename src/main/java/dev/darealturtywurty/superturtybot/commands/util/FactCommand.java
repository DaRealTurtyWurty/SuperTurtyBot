package dev.darealturtywurty.superturtybot.commands.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

public class FactCommand extends CoreCommand {
    private static final String ENDPOINT = "https://api.popcat.xyz/fact";
    
    public FactCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets a random fact";
    }

    @Override
    public String getName() {
        return "fact";
    }
    
    @Override
    public String getRichName() {
        return "Fact";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        try {
            final URLConnection connection = new URL(ENDPOINT).openConnection();
            final JsonObject json = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            if (json.has("error")) {
                final String error = json.get("error").getAsString();
                event.getHook().sendMessage("❌ " + error).queue();
                return;
            }
            
            final String fact = json.get("fact").getAsString();
            event.getHook().sendMessage(fact).queue();
        } catch (final IOException exception) {
            event.getHook().sendMessage("❌ Failed to get fact!").queue();
            exception.printStackTrace();
        }
    }
}
