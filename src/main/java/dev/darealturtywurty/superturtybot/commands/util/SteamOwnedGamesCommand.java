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

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class SteamOwnedGamesCommand extends CoreCommand {

    public SteamOwnedGamesCommand() {
        super(new Types(true, false, false, false));
    }



    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "steamid", "The steamid of the user.", true)
                .setAutoComplete(true));
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
        return "owned-game";
    }

    @Override
    public String getRichName() {
        return "owned-game";
    }

    private List<Game> getUserOwnGames(String steamID){
        try {
            SteamWebApiClient client = new SteamWebApiClient.SteamWebApiClientBuilder(Environment.INSTANCE.steamKey().get()).build();

            GetOwnedGamesRequest gameRequest = new GetOwnedGamesRequest.GetOwnedGamesRequestBuilder(steamID).includeAppInfo(true).includePlayedFreeGames(true).buildRequest();

            GetOwnedGames reqGame = client.<GetOwnedGames>processRequest(gameRequest);

            List<Game> game = reqGame.getResponse().getGames();
            game = game.stream().sorted(Comparator.comparingLong(Game::getPlaytimeForever).reversed()).toList();

            return game;
        } catch (final SteamApiException exception) {
            exception.printStackTrace();
        }

        return List.of();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event){
        String rawOption = event.getOption("steamid", null, OptionMapping::getAsString);

        event.deferReply().queue();
        List<Game> gameList = getUserOwnGames(rawOption);

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (int index = 0; index < gameList.size(); index++) {
            Game game = gameList.get(index);
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

        if (rawOption == null) {
            reply(event, "❌ You must provide a steamID!", false, true);
            return;
        }

    }
}
