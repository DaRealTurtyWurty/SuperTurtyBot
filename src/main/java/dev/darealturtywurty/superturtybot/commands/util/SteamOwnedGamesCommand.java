package dev.darealturtywurty.superturtybot.commands.util;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.ownedgames.Game;
import com.lukaspradel.steamapi.data.json.ownedgames.GetOwnedGames;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetOwnedGamesRequest;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SteamOwnedGamesCommand extends CoreCommand {

    public SteamOwnedGamesCommand() {
        super(new Types(true, false, false, false));
    }

    SteamWebApiClient client = new SteamWebApiClient.SteamWebApiClientBuilder(Environment.INSTANCE.steamKey().get()).build();


    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "steamid", "The steamid of the user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets the games owned by the user through steam.";
    }

    @Override
    public String getName() {
        return "owned-games";
    }

    @Override
    public String getRichName() {
        return "Owned Games";
    }
    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }
    private List<Game> getUserOwnGames(String steamID){
        try {

            GetOwnedGamesRequest gameRequest = new GetOwnedGamesRequest.GetOwnedGamesRequestBuilder(steamID).includeAppInfo(true).includePlayedFreeGames(true).buildRequest();

            GetOwnedGames reqGame = client.<GetOwnedGames>processRequest(gameRequest);

            List<Game> games = reqGame.getResponse().getGames();
            games = games.stream().sorted(Comparator.comparingLong(Game::getPlaytimeForever).reversed()).toList();

            return games;
        } catch (final SteamApiException exception) {
            exception.printStackTrace();
        }

        return List.of();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event){
        String rawOption = event.getOption("steamid", null, OptionMapping::getAsString);

        if (rawOption == null) {
            reply(event, "❌ You must provide a steamID!", false, true);
            return;
        }

        event.deferReply().queue();
        List<Game> gameList = getUserOwnGames(rawOption);

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Game game : gameList) {
            long convertMinutesToHours = game.getPlaytimeForever() / 60;
            String sHour = String.valueOf(convertMinutesToHours);
            contents.field("\n" + game.getName(), sHour + "h", false);
        }

        Color steamColorPalette = new Color(0x00adee);
        var embed = new PaginatedEmbed.Builder(5,contents)
                .title("Steam ID: " + rawOption)
                .color(steamColorPalette)
                .build(event.getJDA());

        embed.send(event.getHook(),() -> event.getHook().editOriginal("❌ Failed to send page!").queue());



    }
}
