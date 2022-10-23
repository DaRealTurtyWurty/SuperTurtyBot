package io.github.darealturtywurty.superturtybot.commands.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
    protected void runSlash(SlashCommandInteractionEvent event) {
        try {
            final URLConnection connection = new URL(ENDPOINT).openConnection();
            final JsonObject json = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            if (json.has("error")) {
                final String error = json.get("error").getAsString();
                event.reply(error).setEphemeral(true).mentionRepliedUser(false).queue();
                return;
            }
            
            reply(event, json.get("fact").getAsString());
        } catch (final IOException exception) {
            reply(event, "❌ Something went wrong!", false, true);
        }
    }
}
