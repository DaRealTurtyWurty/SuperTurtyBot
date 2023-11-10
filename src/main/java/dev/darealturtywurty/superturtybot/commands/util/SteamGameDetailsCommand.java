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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SteamGameDetailsCommand extends CoreCommand {
    public SteamGameDetailsCommand() {
        super(new Types(true,false,false,false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "app-id", "The steam app id.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the app/game details of the specified app id";
    }

    @Override
    public String getName() {
        return "game-details";
    }

    @Override
    public String getRichName() {
        return "game details";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {

        if(Environment.INSTANCE.steamKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Steam API key is not set!");
            return;
        }

        String appID = event.getOption("app-id", null, OptionMapping::getAsString);

        if (appID == null) {
            reply(event, "❌ You must provide a Steam app-id!", false, true);
            return;
        }
        event.deferReply().queue();

        String getGameDetailsUrl = "https://store.steampowered.com/api/appdetails?appids=%s".formatted(appID);

        String getCurrentPlayedGameApi = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?key=%s&appid=%s"
                .formatted(Environment.INSTANCE.steamKey().get(), appID);


        final Request request = new Request.Builder().url(getGameDetailsUrl).get().build();

        final Request request1 = new Request.Builder().url(getCurrentPlayedGameApi).get().build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            Response getSteamPlayerCount = new OkHttpClient().newCall(request1).execute();

            if (!response.isSuccessful()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            if (!getSteamPlayerCount.isSuccessful()) {
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

            ResponseBody playerCountBody = getSteamPlayerCount.body();

            if (playerCountBody == null) {
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

            String playerCountBodyString = playerCountBody.string();

            if (playerCountBodyString.isBlank()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            SteamStoreApiResponse steamResponse = SteamStoreApiResponse.fromJsonString(bodyString, appID);

            GetPlayerCountApi getCurrentPlayers = GetPlayerCountApi.fromJsonString(playerCountBodyString);

            SteamData getSteamAppDetails = steamResponse.data;
            int PlayerCount = getCurrentPlayers.player_count;

            if(!steamResponse.success) {
                event.getHook()
                        .sendMessage("❌ Failed to get steam app-id!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            if(getCurrentPlayers.result == 0) {
                event.getHook()
                        .sendMessage("❌ Failed to get player count!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            StringBuilder categories = new StringBuilder();

            for(Categories category : getSteamAppDetails.categories){
                categories.append(category.description + ", ");
            }

            StringBuilder genres = new StringBuilder();

            for(Genres genre : getSteamAppDetails.genres){
                genres.append(genre.description + ", ");
            }

            String category = String.valueOf(categories);
            String genre = String.valueOf(genres);
            String playerCount = String.valueOf(PlayerCount);
            String isFree = String.valueOf(getSteamAppDetails.is_free);
            String recommendationTotal = String.valueOf(getSteamAppDetails.recommendations.total);

            var embed = new EmbedBuilder()
                    .setTitle("%s".formatted(getSteamAppDetails.name), "https://store.steampowered.com/app/%s/".formatted(appID))

                    .addField("🟦 Description", getSteamAppDetails.short_description, false)
                    .addField("🟦 Categories", category, true)
                    .addField("🟦 Platforms\nWindows: %b\nMac: %b\nLinux: %b".formatted(getSteamAppDetails.platforms.windows, getSteamAppDetails.platforms.mac, getSteamAppDetails.platforms.linux), "",true)
                    .addField("🟦 Release Date", getSteamAppDetails.release_date.date,true)
                    .addField("🟦 Recommendations", recommendationTotal, true)
                    .addField("🟦 Genre", genre, true)
                    .addField("🟦 Players playing", playerCount,true)
                    .addField("🟦 Is free", isFree, false)
                    .addField("🟦 Publishers\n"+ getSteamAppDetails.developers.toString() + "\n🟦 Developers" , getSteamAppDetails.publishers.toString(),false)

                    .setImage(getSteamAppDetails.header_image)

                    .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .setColor(0x66c0f4)

                    .build();

            event.getHook()
                    .sendMessageEmbeds(embed)
                    .mentionRepliedUser(false)
                    .queue();

        }catch (IOException exception) {
            reply(event, "Failed to response!");
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }
    @Data
    public static class SteamStoreApiResponse{
        private boolean success;
        private SteamData data;

        private static SteamStoreApiResponse fromJsonString(String json, String appid) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object.get(appid), SteamStoreApiResponse.class);
        }
    }
    @Data
    public static class SteamData{
        private String type;
        private String name;
        private String header_image;
        private int steam_appid;
        private boolean is_free;
        private String about_the_game;
        private String short_description;
        private List<String> publishers;
        private List<String> developers;
        private Platforms platforms;
        private List<Categories> categories;
        private List<Genres> genres;
        private Recommendations recommendations;
        private ReleaseDate release_date;

    }

    @Data
    public static class Platforms {
        private boolean windows;
        private boolean mac;
        private boolean linux;
    }
    @Data

    public static class Categories {
        private int id;
        private String description;
    }
    @Data

    public static class Genres {
        private int id;
        private String description;
    }
    @Data
    public static class Recommendations {
        private int total;
    }
    @Data
    public static class ReleaseDate {
        private boolean coming_soon;
        private String date;
    }
    @Data
    public static class GetPlayerCountApi{
        private int player_count;
        private int result;
        private static GetPlayerCountApi fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object.get("response"), GetPlayerCountApi.class);
        }
    }

}
