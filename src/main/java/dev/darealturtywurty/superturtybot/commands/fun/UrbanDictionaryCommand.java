package dev.darealturtywurty.superturtybot.commands.fun;

import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.codepoetics.ambivalence.Either;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UrbanDictionaryCommand extends CoreCommand {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    static {
        ShutdownHooks.register(() -> ShutdownHooks.shutdownOkHttpClient(CLIENT));
    }

    public UrbanDictionaryCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "search_term", "The thing that you want to search", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Searches the Urban Dictionary for a definition of the given term";
    }

    @Override
    public String getHowToUse() {
        return "/urban [searchTerm]";
    }

    @Override
    public String getName() {
        return "urban";
    }

    @Override
    public String getRichName() {
        return "Urban Dictionary";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String searchTerm = URLEncoder.encode(event.getOption("search_term").getAsString().toLowerCase().trim(),
            StandardCharsets.UTF_8);
        final Pair<Boolean, Either<String, EmbedBuilder>> returned = makeRequest(searchTerm);
        if (Boolean.FALSE.equals(returned.getLeft())) {
            event.deferReply(true).setContent(returned.getRight().left().toOptional().get()).mentionRepliedUser(false)
                .queue();
            return;
        }

        if (returned.getRight().isLeft()) {
            event.deferReply().setContent(returned.getRight().left().toOptional().get()).mentionRepliedUser(false)
                .queue();
        } else {
            event.deferReply().addEmbeds(returned.getRight().right().toOptional().get().build())
                .mentionRepliedUser(false).queue();
        }
    }

    private static Pair<Boolean, Either<String, EmbedBuilder>> makeRequest(String searchTerm) {
        try {
            final Request request = new Request.Builder()
                .url("https://mashape-community-urban-dictionary.p.rapidapi.com/define?term=" + searchTerm).get()
                .addHeader("X-RapidAPI-Host", "mashape-community-urban-dictionary.p.rapidapi.com")
                .addHeader("X-RapidAPI-Key", Environment.INSTANCE.urbanDictionaryKey()).build();
            final Response response = CLIENT.newCall(request).execute();
            final JsonObject json = Constants.GSON.fromJson(response.body().string(), JsonObject.class);
            final JsonArray list = json.getAsJsonArray("list");
            if (list.isEmpty())
                return Pair.of(false, Either.ofLeft("No Results Found!"));

            final JsonObject first = list.get(0).getAsJsonObject();

            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setColor(Color.GREEN);
            embed.setTitle("Definition for: '" + first.get("word").getAsString() + "' by Urban Dictionary!",
                first.get("permalink").getAsString());
            embed.setDescription("Description:\n" + first.get("definition").getAsString() + "\n\nExample(s):\n"
                + first.get("example").getAsString());
            embed.setFooter("👍" + first.get("thumbs_up").getAsInt() + " 👎" + first.get("thumbs_down").getAsInt()
                + " | Written By: " + first.get("author").getAsString());

            response.close();

            return Pair.of(true, Either.ofRight(embed));
        } catch (final IOException exception) {
            return Pair.of(false, Either.ofLeft("Failed to connect!"));
        } catch (final JsonParseException | NullPointerException exception) {
            return Pair.of(false, Either.ofLeft(
                "There has been an issue accessing our database! " + "The bot owner has been notified of this issue!"));
        }
    }
}
