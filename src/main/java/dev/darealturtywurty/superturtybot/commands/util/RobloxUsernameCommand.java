package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RobloxUsernameCommand extends CoreCommand {
    public RobloxUsernameCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "username", "The roblox username", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the username information of a roblox user.";
    }

    @Override
    public String getName() {
        return "roblox-username";
    }

    @Override
    public String getRichName() {
        return "Roblox Username";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String robloxUserName = event.getOption("username", null, OptionMapping::getAsString);

        if (robloxUserName == null) {
            reply(event, "❌ You must provide a valid username!", false, true);
            return;
        }

        event.deferReply().queue();

        String robloxSearchForUsernameUrl = "https://users.roblox.com/v1/users/search?keyword=%s".formatted(robloxUserName);
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

            RobloxUserNameInfo robloxResponse = RobloxUserNameInfo.fromJsonString(bodyString);
            List<PlayerData> robloxData = robloxResponse.getData();
            if (robloxData.isEmpty()) {
                event.getHook()
                        .sendMessage("❌ Failed to get data!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (PlayerData data : robloxData) {
                contents.field(data.name, data.id +
                        "\n**Display Name: **" + data.displayName +
                        "\n**Has verified badge: **" + data.hasVerifiedBadge +
                        "\n**Previous Names: **" + Arrays.toString(data.previousUsernames), false);
            }

            var embed = new PaginatedEmbed.Builder(5, contents)
                    .title("Roblox Username: " + robloxUserName)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .timestamp(Instant.now())
                    .build(event.getJDA());

            embed.send(event.getHook(),
                    () -> event.getHook().editOriginal("❌ Failed to list roblox user!").queue());
        } catch (IOException exception) {
            event.getHook()
                    .sendMessage("❌ Failed to get response!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @Data
    public static class RobloxUserNameInfo {
        private List<PlayerData> data;

        private static RobloxUserNameInfo fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, RobloxUserNameInfo.class);
        }
    }

    @Data
    public static class PlayerData {
        private String[] previousUsernames;
        private boolean hasVerifiedBadge;
        private long id;
        private String name;
        private String displayName;
    }
}
