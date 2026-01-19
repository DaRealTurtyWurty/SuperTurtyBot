package dev.darealturtywurty.superturtybot.commands.economy.blackjack;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BlackjackHitCommand extends BlackjackSubcommand {
    public BlackjackHitCommand() {
        super("hit", "Draw another card in your current blackjack game");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        BlackjackCommand.Game game = BlackjackCommand.getOngoingGame(event);
        if (game == null) {
            event.getHook().editOriginal("❌ You do not have an ongoing blackjack game!").queue();
            return;
        }

        if (game.isFinished()) {
            event.getHook().editOriginal("❌ Your blackjack game is already finished!").queue();
            return;
        }

        if (game.getStatus() != BlackjackCommand.Game.Status.PLAYER_TURN) {
            event.getHook().editOriginal("❌ It is not your turn to hit!").queue();
            return;
        }

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.getHook().editOriginal("❌ You must use this command in your Blackjack thread!").queue();
            return;
        }

        if (thread.getIdLong() != game.getChannel()) {
            event.getHook().editOriginal("❌ This is not your current Blackjack thread!").queue();
            return;
        }

        final List<BlackjackCommand.Game> games = BlackjackCommand.GAMES.computeIfAbsent(guild.getIdLong(), ignored -> new ArrayList<>());

        synchronized (game) {
            BlackjackCommand.Card card = game.hit();
            if (game.isFinished()) {
                Optional<BlackjackCommand.Game.Settlement> settlementOpt = game.getSettlement();
                if (settlementOpt.isPresent()) {
                    BlackjackCommand.Game.Settlement settlement = settlementOpt.get();
                    applySettlement(account, settlement);

                    games.remove(game);
                    String resultMessage = game.getResultMessage(number -> StringUtils.numberFormat(number, config.getEconomyCurrency()));
                    String content = "%s drew a %s.\n%s".formatted(event.getUser().getAsMention(), card.getFriendlyName(), resultMessage);
                    try (FileUpload upload = BlackjackImageRenderer.createUpload(game, true)) {
                        event.getHook().editOriginal(content).setFiles(upload).queue();
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to create blackjack image!", exception);
                        event.getHook().editOriginal(content + "\n❌ Failed to create the blackjack image.").queue();
                    }
                    thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
                }
            } else {
                String content = "%s drew a %s.\nUse /blackjack hit to draw a card or /blackjack stand to end your turn."
                        .formatted(event.getUser().getAsMention(), card.getFriendlyName());
                try (FileUpload upload = BlackjackImageRenderer.createUpload(game, false)) {
                    event.getHook().editOriginal(content).setFiles(upload).queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to create blackjack image!", exception);
                    event.getHook().editOriginal(content + "\n❌ Failed to create the blackjack image.").queue();
                }
            }
        }
    }
}
