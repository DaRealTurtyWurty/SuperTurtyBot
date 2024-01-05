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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RobloxAvatarCommand extends CoreCommand {
    public RobloxAvatarCommand() {
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
        return "Returns the avatar information of a roblox user.";
    }

    @Override
    public String getName() {
        return "roblox-avatar";
    }

    @Override
    public String getRichName() {
        return "Roblox Avatar";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);
        if (robloxUserId == null) {
            reply(event, "❌ You must provide a UserID!", false, true);
            return;
        }

        String robloxAvatarUrl = "https://avatar.roblox.com/v1/users/%s/avatar".formatted(robloxUserId);

        event.deferReply().queue();

        final Request request = new Request.Builder()
                .url(robloxAvatarUrl)
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

            PlayerProfile robloxResponse = PlayerProfile.fromJsonString(bodyString);

            var embed = createAvatarEmbed(robloxResponse, robloxUserId);
            embed.timestamp(Instant.now());
            embed.footer("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
            embed.authorOnly(event.getUser().getIdLong());

            embed.build(event.getJDA()).send(event.getHook(),
                    () -> event.getHook().editOriginal("❌ Failed to get response!").queue());
        } catch (IOException exception) {
            event.getHook()
                    .sendMessage("❌ Failed to get response!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @NotNull
    private static PaginatedEmbed.Builder createAvatarEmbed(PlayerProfile robloxResponse, String robloxUserId) {
        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Asset asset : robloxResponse.getAssets()) {
            contents.field(
                    asset.getName(),
                    "ID: " + asset.getId() + "\nAsset type: " + asset.getAssetType().getName(),
                    false);
        }

        var embed = new PaginatedEmbed.Builder(5, contents);
        embed.title("User ID: " + robloxUserId);
        embed.description("Player Animation Type: " + robloxResponse.getPlayerAvatarType() +
                "\n Default Shirt Applied: " + robloxResponse.isDefaultShirtApplied() +
                "\n Default Pants Applied: " + robloxResponse.isDefaultPantsApplied());
        embed.color(0xf4dcb4);
        return embed;
    }

    @Data
    public static class PlayerProfile {
        private Scales scales;
        private String playerAvatarType;
        private BodyColors bodyColors;
        private List<Asset> assets;
        private boolean defaultShirtApplied;
        private boolean defaultPantsApplied;
        private List<Emote> emotes;

        private static PlayerProfile fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, PlayerProfile.class);
        }
    }

    @Data
    public static class Scales {
        private double height;
        private double width;
        private double head;
        private double depth;
        private double proportion;
        private double bodyType;
    }

    @Data
    public static class BodyColors {
        private int headColorId;
        private int torsoColorId;
        private int rightArmColorId;
        private int leftArmColorId;
        private int rightLegColorId;
        private int leftLegColorId;
    }

    @Data
    public static class Asset {
        private long id;
        private String name;
        private AssetType assetType;
        private long currentVersionId;
        private Meta meta;
    }

    @Data
    public static class AssetType {
        private long id;
        private String name;
    }

    @Data
    public static class Meta {
        private int order;
        private int version;
    }

    @Data
    public static class Emote {
        private long assetId;
        private String assetName;
        private int position;
    }
}
