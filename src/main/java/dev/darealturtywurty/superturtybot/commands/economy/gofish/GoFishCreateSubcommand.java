package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.math.BigInteger;

public class GoFishCreateSubcommand extends GoFishSubcommand {
    protected GoFishCreateSubcommand() {
        super("create", "Create a new Go Fish game in this channel");
        addOption(OptionType.STRING, "bet", "The amount each player must bet to join.", true);
        addOption(OptionType.INTEGER, "max_players", "Maximum number of players (2-6).", false);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        long channelId = event.getChannel().getIdLong();
        if (GoFishCommand.getGame(channelId) != null) {
            event.getHook().editOriginal("❌ There is already a Go Fish game in this channel.").queue();
            return;
        }

        GoFishCommand.Game existing = GoFishCommand.findGameByUser(guild.getIdLong(), event.getUser().getIdLong());
        if (existing != null) {
            event.getHook().editOriginal("❌ You are already in a Go Fish game in another channel.").queue();
            return;
        }

        BigInteger bet = event.getOption("bet", StringUtils.getAsBigInteger(event));
        if (bet == null) {
            event.getHook().editOriginal("❌ Invalid bet amount!").queue();
            return;
        }

        if (bet.signum() <= 0) {
            event.getHook().editOriginal("❌ You must bet at least %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        if (bet.compareTo(account.getWallet()) > 0) {
            event.getHook().editOriginal("❌ You cannot bet more than you have in your wallet!").queue();
            return;
        }

        int maxPlayers = event.getOption("max_players", GoFishCommand.MAX_PLAYERS, OptionMapping::getAsInt);
        if (maxPlayers < GoFishCommand.MIN_PLAYERS || maxPlayers > GoFishCommand.MAX_PLAYERS) {
            event.getHook().editOriginal("❌ max_players must be between 2 and 6.").queue();
            return;
        }

        EconomyManager.removeMoney(account, bet, false);
        EconomyManager.updateAccount(account);

        var game = new GoFishCommand.Game(
                guild.getIdLong(),
                channelId,
                event.getUser().getIdLong(),
                bet,
                maxPlayers);
        game.addPlayer(event.getUser().getIdLong(), bet);
        GoFishCommand.GAMES.put(channelId, game);

        event.getHook().editOriginal("✅ Go Fish game created! Players can join with the button below. Auto-starts in 2 minutes.")
                .queue();
        String lobbyMessage = "🃏 **Go Fish** created by %s\nBet: %s | Players: 1/%d\nJoin with the button below."
                .formatted(event.getUser().getAsMention(),
                        StringUtils.numberFormat(bet, config),
                        maxPlayers);
        event.getChannel().sendMessage(lobbyMessage)
                .setComponents(ActionRow.of(
                        Button.success("gofish:join:" + channelId, "Join Game"),
                        Button.primary("gofish:start:" + channelId, "Force Start"),
                        Button.danger("gofish:cancel:" + channelId, "Cancel")
                ))
                .queue(message -> {
                    game.setLobbyMessageId(message.getIdLong());
                    GoFishCommand.scheduleAutoStart(guild, game);
                });
    }
}
