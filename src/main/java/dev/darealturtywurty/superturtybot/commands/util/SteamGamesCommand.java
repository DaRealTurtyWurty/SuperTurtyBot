package dev.darealturtywurty.superturtybot.commands.util;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.ownedgames.Game;
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetOwnedGamesRequest;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SteamGamesCommand extends CoreCommand {
    private static final SteamWebApiClient CLIENT;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    static {
        if (Environment.INSTANCE.steamKey().isPresent()) {
            CLIENT = new SteamWebApiClient.SteamWebApiClientBuilder(Environment.INSTANCE.steamKey().get()).build();
        } else {
            CLIENT = null;
        }
    }

    public SteamGamesCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "steamid", "The steamid of the user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets the user's list of owned steam games.";
    }

    @Override
    public String getName() {
        return "steam-games";
    }

    @Override
    public String getRichName() {
        return "Steam Games";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(Environment.INSTANCE.steamKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Steam API key is not set!");
            return;
        }

        String steamId = event.getOption("steamid", null, OptionMapping::getAsString);
        if (steamId == null) {
            reply(event, "❌ You must provide a valid Steam ID! Use `/steam-id` to get it.", false, true);
            return;
        }

        event.deferReply().queue();

        List<Game> games = getOwnedGames(steamId);
        if(games.isEmpty()) {
            event.getHook().editOriginal("❌ Failed to get user's steam games!").queue();
            return;
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Game game : games) {
            String playTime = "%s hrs".formatted(DECIMAL_FORMAT.format(game.getPlaytimeForever() / 60f));
            contents.field(game.getName(), playTime, false);
        }

        var embed = new PaginatedEmbed.Builder(5, contents)
                .title("Steam ID: " + steamId)
                .color(0x66c0f4)
                .description("Total Games: %s".formatted(games.size()))
                .authorOnly(event.getUser().getIdLong())
                .build(event.getJDA());

        embed.send(event.getHook(),
                () -> event.getHook().editOriginal("❌ Failed to list games!").queue());
    }

    private static List<Game> getOwnedGames(String steamID) {
        try {
            var gameRequest = new GetOwnedGamesRequest.GetOwnedGamesRequestBuilder(steamID)
                    .includeAppInfo(true)
                    .includePlayedFreeGames(true)
                    .buildRequest();

            GetOwnedGames reqGame = CLIENT.processRequest(gameRequest);

            List<Game> games = reqGame.getResponse().getGames();
            games.sort(Comparator.comparingLong(Game::getPlaytimeForever).reversed());
            return games;
        } catch (final SteamApiException exception) {
            Constants.LOGGER.error("Failed to get user's steam games!", exception);
            return List.of();
        }
    }
}
