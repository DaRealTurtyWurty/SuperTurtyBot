package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class GoFishLeaveSubcommand extends GoFishSubcommand {
    protected GoFishLeaveSubcommand() {
        super("leave", "Leave the Go Fish game before it starts");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        long channelId = event.getChannel().getIdLong();
        GoFishCommand.Game game = GoFishCommand.getGame(channelId);
        if (game == null) {
            event.getHook().editOriginal("❌ There is no Go Fish game in this channel.").queue();
            return;
        }

        synchronized (game) {
            if (!game.hasPlayer(event.getUser().getIdLong())) {
                event.getHook().editOriginal("❌ You are not part of this Go Fish game.").queue();
                return;
            }

            if (game.isStarted()) {
                event.getHook().editOriginal("❌ You cannot leave after the game has started.").queue();
                return;
            }

            if (game.getHostId() == event.getUser().getIdLong()) {
                GoFishCommand.cancelAutoStart(game);
                GoFishCommand.refundAndCancel(guild, config, game,
                        "⚠️ The Go Fish game was canceled by the host. All bets were refunded.");
                event.getHook().editOriginal("✅ Go Fish game canceled. All bets have been refunded.").queue();
                return;
            }

            EconomyManager.addMoney(account, game.getBet(), false);
            EconomyManager.updateAccount(account);
            game.removePlayer(event.getUser().getIdLong());

            event.getHook().editOriginal("✅ You left the Go Fish game. Your bet has been refunded.").queue();
            event.getChannel().sendMessageFormat("%s left the Go Fish game. Players: %d/%d",
                    event.getUser().getAsMention(), game.playerCount(), game.getMaxPlayers()).queue();
            GoFishCommand.updateLobbyMessage(guild, config, game, false);
        }
    }
}
