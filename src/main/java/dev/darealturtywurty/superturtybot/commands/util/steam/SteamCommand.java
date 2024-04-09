package dev.darealturtywurty.superturtybot.commands.util.steam;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.ownedgames.Game;
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetOwnedGamesRequest;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SteamCommand extends CoreCommand {
    private static final SteamWebApiClient STEAM_WEB_CLIENT;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    static {
        if (Environment.INSTANCE.steamKey().isPresent()) {
            STEAM_WEB_CLIENT = new SteamWebApiClient.SteamWebApiClientBuilder(Environment.INSTANCE.steamKey().get()).build();
        } else {
            STEAM_WEB_CLIENT = null;
        }
    }

    public SteamCommand() {
        super(new Types(true, false, false, false));
    }

    private static void handleUserID(SlashCommandInteractionEvent event, String steamKey) {
        String steamName = event.getOption("vanityurl", null, OptionMapping::getAsString);
        if (steamName == null) {
            event.getHook().sendMessage("❌ You must provide a Steam vanity URL!").queue();
            return;
        }

        String steamVanityUrl = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=%s&vanityurl=%s"
                .formatted(steamKey, steamName);

        final Request request = new Request.Builder().url(steamVanityUrl).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from Steam!").queue();
                return;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Steam!").queue();
                return;
            }

            var vanityUrlResponse = SteamVanityUrlResponse.fromJsonString(bodyString);
            if (vanityUrlResponse.getSuccess() != 1 || vanityUrlResponse.getSteamid() == null) {
                event.getHook().sendMessage("❌ You must provide a valid steam vanity url!").queue();
                return;
            }

            var embed = new EmbedBuilder()
                    .setTitle("Steam Name: " + steamName)
                    .setDescription("Steam ID: " + vanityUrlResponse.getSteamid())
                    .setColor(0x00adee)
                    .build();

            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    private static void handleUserGames(SlashCommandInteractionEvent event) {
        String steamId = event.getOption("user", null, OptionMapping::getAsString);
        if (steamId == null) {
            event.getHook().sendMessage("❌ You must provide a valid Steam User! Use `/steam-id` with your vanity url to get an ID.").queue();
            return;
        }

        List<Game> games = getOwnedGames(steamId);
        if (games.isEmpty()) {
            event.getHook().editOriginal("❌ Failed to get user's steam games!").queue();
            return;
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Game game : games) {
            String playTime = "%s hrs".formatted(DECIMAL_FORMAT.format(game.getPlaytimeForever() / 60f));
            contents.field(game.getName(), playTime, false);
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                .title("Steam ID: " + steamId)
                .color(0x66c0f4)
                .description("Total Games: %s".formatted(games.size()))
                .authorOnly(event.getUser().getIdLong())
                .build(event.getJDA());

        embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ Failed to list games!").queue());
    }

    private static void handleGameDetails(SlashCommandInteractionEvent event, String steamKey) {
        String appID = event.getOption("appid", null, OptionMapping::getAsString);
        if (appID == null) {
            event.getHook().sendMessage("❌ You must provide a Steam App ID!").queue();
            return;
        }

        String gameDetailsUrl = "https://store.steampowered.com/api/appdetails?appids=%s&l=english&cc=us".formatted(appID);
        String playerCountUrl = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?key=%s&appid=%s"
                .formatted(steamKey, appID);

        final Request gameDetailsRequest = new Request.Builder().url(gameDetailsUrl).get().build();
        final Request playerCountRequest = new Request.Builder().url(playerCountUrl).get().build();

        try (Response gameDetailsResponse = Constants.HTTP_CLIENT.newCall(gameDetailsRequest).execute();
             Response playerCountResponse = Constants.HTTP_CLIENT.newCall(playerCountRequest).execute()) {
            if (!gameDetailsResponse.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            if (!playerCountResponse.isSuccessful()) {
                event.getHook()
                        .sendMessage("❌ Response was not successful!")
                        .queue();
                return;
            }

            ResponseBody gameDetailsBody = gameDetailsResponse.body();
            if (gameDetailsBody == null) {
                event.getHook().sendMessage("❌ Invalid response from Steam!").queue();
                return;
            }

            ResponseBody playerCountBody = playerCountResponse.body();
            if (playerCountBody == null) {
                event.getHook().sendMessage("❌ Invalid response from Steam!").queue();
                return;
            }

            String gameDetailsStr = gameDetailsBody.string();
            if (gameDetailsStr.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Steam!").queue();
                return;
            }

            String playerCountStr = playerCountBody.string();
            if (playerCountStr.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from Steam!").queue();
                return;
            }

            var appDetailsDataResponse = SteamAppDetailsResponse.fromJsonString(gameDetailsStr, appID);
            var playerCountDataResponse = SteamAppPlayerCountResponse.fromJsonString(playerCountStr);

            SteamAppDetailsData appDetailsData = appDetailsDataResponse.getData();
            int playerCount = playerCountDataResponse.getPlayer_count();

            if (!appDetailsDataResponse.isSuccess()) {
                event.getHook().sendMessage("❌ Failed to get steam app id!").queue();
                return;
            }

            if (playerCountDataResponse.getResult() == 0) {
                event.getHook().sendMessage("❌ Failed to get player count!").queue();
                return;
            }

            StringBuilder categories = new StringBuilder();
            if (!appDetailsData.getCategories().isEmpty()) {
                for (SteamAppDetailsData.Category category : appDetailsData.getCategories()) {
                    categories.append("`").append(category.getDescription()).append("`").append(", ");
                }
            }

            StringBuilder genres = new StringBuilder();
            if (!appDetailsData.getGenres().isEmpty()) {
                for (SteamAppDetailsData.Genre genre : appDetailsData.getGenres()) {
                    genres.append("`").append(genre.getDescription()).append("`").append(", ");
                }
            }

            StringBuilder developers = new StringBuilder();
            if (!appDetailsData.getDevelopers().isEmpty()) {
                for (String developer : appDetailsData.getDevelopers()) {
                    developers.append("`").append(developer).append("`").append(", ");
                }
            }

            StringBuilder publishers = new StringBuilder();
            if (!appDetailsData.getPublishers().isEmpty()) {
                for (String publisher : appDetailsData.getPublishers()) {
                    publishers.append("`").append(publisher).append("`").append(", ");
                }
            }

            var embed = new EmbedBuilder()
                    .setTitle(appDetailsData.getName(), appDetailsData.getWebsite())
                    .setThumbnail(appDetailsData.getCapsule_imagev5())
                    .setDescription(appDetailsData.getShort_description())
                    .setImage(appDetailsData.getHeader_image())
                    .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                            event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (!categories.isEmpty()) {
                embed.addField("Categories", categories.substring(0, categories.length() - 2), true);
            }

            if (!genres.isEmpty()) {
                embed.addField("Genres", genres.substring(0, genres.length() - 2), true);
            }

            if (!developers.isEmpty()) {
                embed.addField("Developers", developers.substring(0, developers.length() - 2), false);
            }

            if (!publishers.isEmpty()) {
                embed.addField("Publishers", publishers.substring(0, publishers.length() - 2), true);
            }

            embed.addField("Platforms", """
                    Windows: %s
                    Mac: %s
                    Linux: %s""".formatted(
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isWindows()),
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isMac()),
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isLinux())), false);

            if (appDetailsData.getPrice_overview() != null && !appDetailsData.is_free()) {
                embed.addField("Price", appDetailsData.getPrice_overview().getFinal_formatted(), true);
            } else {
                embed.addField("Price", "Free", true);
            }

            embed.addField("Player Count", String.valueOf(playerCount), true);
            embed.addField("Release Date", appDetailsData.getRelease_date().getDate(), true);

            List<EmbedBuilder> screenshotEmbeds = new ArrayList<>();

            List<SteamAppDetailsData.Screenshot> screenshots = appDetailsData.getScreenshots();
            if (!screenshots.isEmpty()) {
                SteamAppDetailsData.Screenshot screenshot0 = screenshots.getFirst();
                var screenshot0Embed = new EmbedBuilder()
                        .setTitle("Screenshots")
                        .setUrl(appDetailsData.getWebsite() == null ?
                                "https://store.steampowered.com/app/%s".formatted(appDetailsData.getSteam_appid()) :
                                appDetailsData.getWebsite() + "/")
                        .setImage(screenshot0.getPath_full())
                        .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                                event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now());
                screenshotEmbeds.add(screenshot0Embed);

                appDetailsData.getScreenshots().subList(1, Math.min(appDetailsData.getScreenshots().size(), 4)).forEach(screenshot -> {
                    var screenshotEmbed = new EmbedBuilder()
                            .setUrl(appDetailsData.getWebsite() == null ?
                                    "https://store.steampowered.com/app/%s".formatted(appDetailsData.getSteam_appid()) :
                                    appDetailsData.getWebsite() + "/")
                            .setImage(screenshot.getPath_full())
                            .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                                    event.getUser().getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now());
                    screenshotEmbeds.add(screenshotEmbed);
                });
            }

            List<MessageEmbed> embeds = new ArrayList<>();
            embeds.add(embed.build());
            embeds.addAll(screenshotEmbeds.stream().map(EmbedBuilder::build).toList());
            event.getHook().sendMessageEmbeds(embeds).mentionRepliedUser(false).queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ Failed to get response!").queue();
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    private static List<Game> getOwnedGames(String steamID) {
        try {
            var gameRequest = new GetOwnedGamesRequest.GetOwnedGamesRequestBuilder(steamID)
                    .includeAppInfo(true)
                    .includePlayedFreeGames(true)
                    .buildRequest();

            GetOwnedGames reqGame = STEAM_WEB_CLIENT.processRequest(gameRequest);

            List<Game> games = reqGame.getResponse().getGames();
            games.sort(Comparator.comparingLong(Game::getPlaytimeForever).reversed());
            return games;
        } catch (final SteamApiException exception) {
            Constants.LOGGER.error("Failed to get user's steam games!", exception);
            return List.of();
        }
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("userid", "Gets the user's steam id from their vanity url.")
                        .addOption(OptionType.STRING, "vanityurl", "The steam username of the user.", true),
                new SubcommandData("usergames", "Gets the user's list of owned steam games.")
                        .addOption(OptionType.STRING, "user", "The steamid or vanityurl of the user.", true),
                new SubcommandData("game-details", "Gets information about a steam game.")
                        .addOption(OptionType.STRING, "appid", "The appid of the game.", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Get information about a steam user or game!";
    }

    @Override
    public String getName() {
        return "steam";
    }

    @Override
    public String getRichName() {
        return "Steam";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (Environment.INSTANCE.steamKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Steam API key is not set!");
            return;
        }

        String steamKey = Environment.INSTANCE.steamKey().get();

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You need to specify a subcommand!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        switch (subcommand) {
            case "userid":
                handleUserID(event, steamKey);
                break;
            case "usergames":
                handleUserGames(event);
                break;
            case "game-details":
                handleGameDetails(event, steamKey);
                break;
            default:
                event.getHook().sendMessage("❌ You need to specify a valid subcommand!").queue();
        }
    }
}
