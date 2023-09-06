package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import lombok.Data;
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

public class GetRobloxUserFavoriteGameCommand extends CoreCommand {
    public GetRobloxUserFavoriteGameCommand() {
        super(new Types(true,false,false,false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "userid", "The userid of the roblox user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the user favorite game";
    }

    @Override
    public String getName() {
        return "roblox-favorite-game";
    }

    @Override
    public String getRichName() {
        return "Roblox Game";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);

        if(robloxUserId == null){
            reply(event, "❌ You must provide a valid User ID!", false, true);
            return;
        }

        String RobloxSearchForUsernameUrl = "https://games.roblox.com/v2/users/%s/favorite/games"
                .formatted(robloxUserId);


        final Request request = new Request.Builder().url(RobloxSearchForUsernameUrl).get().build();

        try(Response response = new OkHttpClient().newCall(request).execute()){
            if(!response.isSuccessful()){
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
            }

            ResponseBody body = response.body();
            if(body == null){
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

            RobloxUserFavoriteGame robloxResponse = RobloxUserFavoriteGame.fromJsonString(bodyString);

            List<FavoriteGameData> robloxData = robloxResponse.data;

            if(robloxData.isEmpty()) {
                reply(event, "❌ Failed to get data!", false, true);
                return;
            }
            event.deferReply().queue();

            var contents = new PaginatedEmbed.ContentsBuilder();

            for (FavoriteGameData data : robloxData) {
                contents.field(data.name, data.id +
                        "\n**Created: **" + data.created +
                        "\n**Updated : **" + data.updated +
                        "\n**Place visits: **" + data.placeVisits,false);

            }

            var embed = new PaginatedEmbed.Builder(5, contents)
                    .title("UserId: " + robloxUserId)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ Failed to list roblox user!").queue());

        }catch(IOException exception){
            reply(event, "Failed to response!");
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    public class RobloxUserFavoriteGame{
        List<FavoriteGameData> data;
        private static RobloxUserFavoriteGame fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, RobloxUserFavoriteGame.class);
        }
    }

    @Data
    public class FavoriteGameData{
        private int id;
        private String name;
        private String description;
        private Creator creator;
        private RootPlace rootPlace;
        private String created;
        private String updated;
        private int placeVisits;
    }
    @Data
    class Creator{
        private int id;
        private String group;
    }
    @Data
    class RootPlace{
        private int id;
        private String type;
    }

}
