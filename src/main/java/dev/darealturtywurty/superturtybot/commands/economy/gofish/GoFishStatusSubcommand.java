package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.math.BigInteger;
import java.util.StringJoiner;

public class GoFishStatusSubcommand extends GoFishSubcommand {
    protected GoFishStatusSubcommand() {
        super("status", "Show the current Go Fish game status");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        GoFishCommand.Game game = GoFishCommand.getGame(event.getChannel().getIdLong());
        if (game == null) {
            event.getHook().editOriginal("❌ There is no Go Fish game in this channel.").queue();
            return;
        }

        game.touch();

        if (game.isStarted() && game.getChannelId() != event.getChannel().getIdLong()) {
            event.getHook().editOriginal("❌ Use this command in the Go Fish game thread.").queue();
            return;
        }

        StringJoiner playersJoiner = new StringJoiner("\n");
        game.getPlayerStates().forEach(player -> playersJoiner.add(
                "<@" + player.userId() + "> - " + player.hand().size() + " cards, "
                        + player.books().size() + " books"
        ));

        BigInteger pot = game.getBet().multiply(BigInteger.valueOf(game.playerCount()));
        String message = "**Go Fish Status**\n"
                + "Started: " + (game.isStarted() ? "Yes" : "No") + "\n"
                + "Host: <@" + game.getHostId() + ">\n"
                + "Current turn: " + (game.isStarted() ? "<@" + game.getCurrentPlayerId() + ">" : "(not started)") + "\n"
                + "Bet: " + StringUtils.numberFormat(game.getBet(), config) + " | Pot: " + StringUtils.numberFormat(pot, config) + "\n"
                + "Deck: " + game.getDeck().size() + " cards\n"
                + "**Players**\n" + playersJoiner;

        event.getHook().editOriginal(message).queue();
    }
}
