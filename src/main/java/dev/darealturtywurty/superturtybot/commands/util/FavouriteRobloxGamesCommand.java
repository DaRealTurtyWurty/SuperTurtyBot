package dev.darealturtywurty.superturtybot.commands.util;

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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FavouriteRobloxGamesCommand extends CoreCommand {
    public FavouriteRobloxGamesCommand() {
        super(new Types(true, false, false, false));
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
        return "Returns the user's favourite games.";
    }

    @Override
    public String getName() {
        return "favourite-roblox-games";
    }

    @Override
    public String getRichName() {
        return "Favourite Roblox Games";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);

        if (robloxUserId == null) {
            reply(event, "❌ You must provide a valid User ID!", false, true);
            return;
        }

        event.deferReply().queue();

        String robloxSearchForUsernameUrl = "https://games.roblox.com/v2/users/%s/favorite/games".formatted(robloxUserId);
        final Request request = new Request.Builder()
                .url(robloxSearchForUsernameUrl)
                .get()
                .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
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

            RobloxUserFavouriteGame robloxResponse = RobloxUserFavouriteGame.fromJsonString(bodyString);

            List<FavouriteGameData> robloxData = robloxResponse.getData();
            if (robloxData.isEmpty()) {
                event.getHook()
                        .sendMessage("❌ Failed to get data!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (FavouriteGameData data : robloxData) {
                contents.field(data.getName(), data.getId() +
                        "\n**Created: **" + data.getCreated() +
                        "\n**Updated : **" + data.getUpdated() +
                        "\n**Place visits: **" + data.getPlaceVisits(), false);
            }

            var embed = new PaginatedEmbed.Builder(5, contents)
                    .title("UserId: " + robloxUserId)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .timestamp(Instant.now())
                    .build(event.getJDA());

            embed.send(event.getHook(),
                    () -> event.getHook().editOriginal("❌ Failed to list games!").queue());
        } catch (IOException exception) {
            event.getHook()
                    .sendMessage("❌ Failed to get response!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @Data
    public static class RobloxUserFavouriteGame {
        private List<FavouriteGameData> data;

        private static RobloxUserFavouriteGame fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, RobloxUserFavouriteGame.class);
        }
    }

    @Data
    public static class FavouriteGameData {
        private long id;
        private String name;
        private String description;
        private Creator creator;
        private RootPlace rootPlace;
        private String created;
        private String updated;
        private int placeVisits;
    }

    @Data
    public static class Creator {
        private long id;
        private String group;
    }

    @Data
    public static class RootPlace {
        private long id;
        private String type;
    }
}
