package dev.darealturtywurty.superturtybot.commands.util.roblox;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RobloxCommand extends CoreCommand {
    public RobloxCommand() {
        super(new Types(true, false, false, false));
    }

    private static void handleUsername(SlashCommandInteractionEvent event) {
        String username = event.getOption("username", null, OptionMapping::getAsString);
        if (username == null) {
            event.getHook().sendMessage("❌ You must provide a valid username!").queue();
            return;
        }

        String searchUrl = "https://users.roblox.com/v1/users/search?keyword=%s".formatted(username);
        final Request request = new Request.Builder().url(searchUrl).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from Roblox!").queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Roblox!").queue();
                return;
            }

            var usernameInfo = RobloxUsernameInfo.fromJsonString(bodyString);
            List<RobloxPlayerData> robloxData = usernameInfo.getData();
            if (robloxData.isEmpty()) {
                event.getHook().sendMessage("❌ No roblox user found with the username: " + username).queue();
                return;
            }

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (RobloxPlayerData data : robloxData) {
                contents.field(data.getName(), data.getId() +
                        "\n**Display Name: **" + data.getDisplayName() +
                        "\n**Has verified badge: **" + data.isHasVerifiedBadge() +
                        "\n**Previous Names: **" + Arrays.toString(data.getPreviousUsernames()), false);
            }

            var embed = new PaginatedEmbed.Builder(10, contents)
                    .title("Roblox Username: " + username)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .timestamp(Instant.now())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ Failed to list roblox user!").queue());
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    private static void handleAvatar(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);
        if (robloxUserId == null) {
            event.getHook().sendMessage("❌ You must provide a user id!").queue();
            return;
        }

        String robloxAvatarUrl = "https://avatar.roblox.com/v1/users/%s/avatar".formatted(robloxUserId);
        final Request request = new Request.Builder().url(robloxAvatarUrl).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from Roblox!").queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Roblox!").queue();
                return;
            }

            var playerProfile = RobloxPlayerProfile.fromJsonString(bodyString);
            PaginatedEmbed embed = createAvatarEmbed(playerProfile, robloxUserId, event.getUser().getIdLong())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ Failed to get response!").queue());
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    private static void handleFriends(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);
        if (robloxUserId == null) {
            event.getHook().sendMessage("❌ You must provide a valid user id!").queue();
            return;
        }

        String url = "https://friends.roblox.com/v1/users/%s/friends".formatted(robloxUserId);
        final Request request = new Request.Builder().url(url).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from Roblox!").queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Roblox!").queue();
                return;
            }

            var robloxResponse = RobloxFriendDataResponse.fromJsonString(bodyString);
            List<RobloxFriendData> friendData = robloxResponse.getData();
            if (friendData.isEmpty()) {
                event.getHook().sendMessage("❌ No friends found for the user id: " + robloxUserId).queue();
                return;
            }

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (RobloxFriendData data : friendData) {
                contents.field(data.getName(), data.getId() +
                        "\n**Display Name**: " + data.getDisplayName() +
                        "\n**Has Verified Badge**: " + data.isHasVerifiedBadge() +
                        "\n**Is Online**: " + data.isOnline() +
                        "\n**Is Deleted**: " + data.isDeleted());
            }

            var embed = new PaginatedEmbed.Builder(5, contents)
                    .title("Roblox UserID: " + robloxUserId)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .timestamp(Instant.now())
                    .build(event.getJDA());

            embed.send(event.getHook(),
                    () -> event.getHook().editOriginal("❌ Failed to list roblox friends!").queue());
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    private static void handleFavouriteGames(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);
        if (robloxUserId == null) {
            event.getHook().sendMessage("❌ You must provide a valid user id!").queue();
            return;
        }

        String url = "https://games.roblox.com/v2/users/%s/favorite/games".formatted(robloxUserId);
        final Request request = new Request.Builder().url(url).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from Roblox!").queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Roblox!").queue();
                return;
            }

            var robloxResponse = RobloxFavouriteGamesResponse.fromJsonString(bodyString);
            List<RobloxFavouriteGameData> favouriteGames = robloxResponse.getData();
            if (favouriteGames.isEmpty()) {
                event.getHook().sendMessage("❌ No favourite games found for the user id: " + robloxUserId).queue();
                return;
            }

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (RobloxFavouriteGameData data : favouriteGames) {
                contents.field(data.getName(), data.getId() +
                        "\n**Description**: " + StringUtils.truncateString(data.getDescription(), 128) +
                        "\n**Creator**: " + data.getCreator() +
                        "\n**Place Visits**: " + data.getPlaceVisits(), false);
            }

            var embed = new PaginatedEmbed.Builder(5, contents)
                    .title("Roblox UserID: " + robloxUserId)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .timestamp(Instant.now())
                    .build(event.getJDA());

            embed.send(event.getHook(),
                    () -> event.getHook().editOriginal("❌ Failed to list roblox favourite games!").queue());
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @NotNull
    private static PaginatedEmbed.Builder createAvatarEmbed(RobloxPlayerProfile robloxResponse, String robloxUserId, long userId) {
        var contents = new PaginatedEmbed.ContentsBuilder();
        for (RobloxPlayerProfile.Asset asset : robloxResponse.getAssets()) {
            contents.field(asset.getName(),
                    "ID: " + asset.getId() + "\nAsset type: " + asset.getAssetType().getName(),
                    false);
        }

        return new PaginatedEmbed.Builder(5, contents)
                .title("User ID: " + robloxUserId)
                .description("Player Animation Type: " + robloxResponse.getPlayerAvatarType() +
                        "\n Default Shirt Applied: " + robloxResponse.isDefaultShirtApplied() +
                        "\n Default Pants Applied: " + robloxResponse.isDefaultPantsApplied())
                .color(0xf4dcb4)
                .timestamp(Instant.now())
                .authorOnly(userId);
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("username", "Returns the username information of a roblox user.")
                        .addOption(OptionType.STRING, "username", "The roblox username", true),
                new SubcommandData("avatar", "Returns the avatar of a roblox user.")
                        .addOption(OptionType.STRING, "userid", "The user id of the roblox user.", true),
                new SubcommandData("friends", "Returns the friends of a roblox user.")
                        .addOption(OptionType.STRING, "userid", "The user id of the roblox user.", true),
                new SubcommandData("favourite-games", "Returns the favourite games of a roblox user.")
                        .addOption(OptionType.STRING, "userid", "The user id of the roblox user.", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns information about a roblox user.";
    }

    @Override
    public String getName() {
        return "roblox";
    }

    @Override
    public String getRichName() {
        return "Roblox";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You need to specify a subcommand!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        switch (subcommand) {
            case "username" -> handleUsername(event);
            case "avatar" -> handleAvatar(event);
            case "friends" -> handleFriends(event);
            case "favourite-games" -> handleFavouriteGames(event);
            default -> event.getHook().sendMessage("❌ You need to specify a valid subcommand!").queue();
        }
    }
}
