package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;
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

public class SteamIDCommand extends CoreCommand {
    public SteamIDCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "vanityurl", "The steam username of the user.", true));
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
        return "steamid";
    }

    @Override
    public String getRichName() {
        return "Steam ID";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(Environment.INSTANCE.steamKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Steam API key is not set!");
            return;
        }

        String steamName = event.getOption("vanityurl", null, OptionMapping::getAsString);
        if (steamName == null) {
            reply(event, "❌ You must provide a Steam vanity URL!", false, true);
            return;
        }

        event.deferReply().queue();

        String steamVanityUrl = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=%s&vanityurl=%s"
                .formatted(Environment.INSTANCE.steamKey().get(), steamName);

        final Request request = new Request.Builder().url(steamVanityUrl).get().build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if(!response.isSuccessful()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            SteamVanityUrlResponse vanityUrlResponse = SteamVanityUrlResponse.fromJsonString(bodyString);
            if(vanityUrlResponse.getSuccess() != 1) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            if (vanityUrlResponse.getSteamid() == null) {
                event.getHook()
                        .sendMessage("❌ You must provide a valid steam vanity url!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            var embed = new EmbedBuilder()
                    .setTitle("Steam Name: " + steamName)
                    .setDescription("Steam ID: " + vanityUrlResponse.getSteamid())
                    .setColor(0x00adee)
                    .build();

            event.getHook()
                    .sendMessageEmbeds(embed)
                    .mentionRepliedUser(false)
                    .queue();
        } catch (IOException exception) {
            reply(event, "Failed to response!");
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @Data
    public static class SteamVanityUrlResponse {
        private String steamid;
        private int success;

        private static SteamVanityUrlResponse fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object.get("response"), SteamVanityUrlResponse.class);
        }
    }
}
