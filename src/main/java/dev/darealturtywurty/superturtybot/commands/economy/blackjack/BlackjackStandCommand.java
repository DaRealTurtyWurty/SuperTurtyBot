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
import java.util.concurrent.TimeUnit;

public class BlackjackStandCommand extends BlackjackSubcommand {
    public BlackjackStandCommand() {
        super("stand", "End your turn in your current blackjack game");
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
            event.getHook().editOriginal("❌ It is not your turn to stand!").queue();
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
            game.stand();

            BlackjackCommand.Game.Settlement settlement = game.getSettlement().orElseThrow();
            applySettlement(account, settlement);

            games.remove(game);
            String resultMessage = game.getResultMessage(number -> StringUtils.numberFormat(number, config.getEconomyCurrency()));
            String content = "%s has chosen to stand.\n%s".formatted(event.getUser().getAsMention(), resultMessage);
            try (FileUpload upload = BlackjackImageRenderer.createUpload(game, true)) {
                event.getHook().editOriginal(content).setFiles(upload).queue();
            } catch (Exception exception) {
                Constants.LOGGER.error("Failed to create blackjack image!", exception);
                event.getHook().editOriginal(content + "\n❌ Failed to create the blackjack image.").queue();
            }
            thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
        }
    }
}
