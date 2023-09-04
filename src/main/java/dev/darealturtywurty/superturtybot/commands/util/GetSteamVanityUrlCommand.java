package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetSteamVanityUrlCommand extends CoreCommand {

    public GetSteamVanityUrlCommand() {
        super(new Types(true, false, false, false));
    }


    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "vanityurl", "The steam username of the user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Converts the steam username to the steam id";
    }

    @Override
    public String getName() {
        return "vanityurl";
    }



    @Override
    public String getRichName() {
        return "Vanity Url";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    public static SteamVanityUrlResponse deserializeVanityUrl(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object.get("response"), SteamVanityUrlResponse.class);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {

        String rawOption = event.getOption("vanityurl", null, OptionMapping::getAsString);

        if (rawOption == null) {
            reply(event, "❌ You must provide a Steam vanity URL!", false, true);
            return;
        }

        String steamVanityUrl = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key="+ Environment.INSTANCE.steamKey().get() + "&vanityurl=";

        final Request request = new Request.Builder().url(steamVanityUrl + rawOption).get().build();
        try(Response response = new OkHttpClient().newCall(request).execute()){
            ResponseBody body = response.body();

            SteamVanityUrlResponse json = deserializeVanityUrl(body.string());

            if(json.steamid == null){
                reply(event, "❌ You must provide a valid steam vanity url!", false, true);
                return;
            }

            var embed = new EmbedBuilder()
                    .setTitle("Steam Vanity Url: \n" + rawOption)
                    .setDescription("Steam ID " + json.steamid)
                    .setColor(0x00adee)
                    .build();

            event.deferReply().addEmbeds(embed).queue();

        } catch (IOException e) {
            reply(event, "Failed to response!");
        }

    }

    public static class SteamVanityUrlResponse{
        private String steamid;
        private int success;
    }

}
