package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PeriodicTableCommand extends CoreCommand {
    private static final String ENDPOINT = "https://api.popcat.xyz/periodic-table?element=%s";

    public PeriodicTableCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "element",
            "The name/atomic number/symbol of an element on the periodic table", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets information about the given element on the periodic table";
    }

    @Override
    public String getHowToUse() {
        return "/periodic-table [element name | symbol | atomic number]";
    }

    @Override
    public String getName() {
        return "periodic-table";
    }

    @Override
    public String getRichName() {
        return "Periodic Table";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String element = event.getOption("element", OptionMapping::getAsString);
        final String urlStr = ENDPOINT.formatted(element);
        try {
            final URLConnection connection = new URI(urlStr).toURL().openConnection();
            final JsonElement jsonElem = Constants.GSON.fromJson(IOUtils.toString(connection.getInputStream(), StandardCharsets.ISO_8859_1), JsonElement.class);
            if(jsonElem.isJsonPrimitive()) {
                reply(event, "❌ Element not found!", false, true);
                return;
            }

            final JsonObject json = jsonElem.getAsJsonObject();

            if (json.has("error")) {
                final String error = json.get("error").getAsString();
                event.reply(error).setEphemeral(true).mentionRepliedUser(false).queue();
                return;
            }

            final String name = json.get("name").getAsString();
            final String symbol = json.get("symbol").getAsString();
            final int atomicNumber = json.get("atomic_number").getAsInt();
            final float atomicMass = json.get("atomic_mass").getAsFloat();
            final int period = json.get("period").getAsInt();
            final String phase = json.get("phase").getAsString();
            final String discoveredBy = json.get("discovered_by").getAsString();
            final String imageURL = json.get("image").getAsString();
            final String summary = json.get("summary").getAsString();
            reply(event,
                new EmbedBuilder().setTitle("Element: " + name + " (" + symbol + ")").setDescription(summary)
                    .addField("Atomic Number", Integer.toString(atomicNumber), true)
                    .addField("Atomic Mass", Float.toString(atomicMass), true)
                    .addField("Period", Integer.toString(period), true).addField("Phase/State", phase, false)
                    .addField("Discovered By", discoveredBy, false).setThumbnail(imageURL).setTimestamp(Instant.now()));
        } catch (final IOException | URISyntaxException exception) {
            reply(event, "❌ Something went wrong!", false, true);
            Constants.LOGGER.error("Unable to get element information!", exception);
        }
    }
}
