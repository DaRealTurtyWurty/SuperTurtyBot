package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PokerFoldCommand extends PokerSubcommand {
    protected PokerFoldCommand() {
        super("fold", "Fold your hand and forfeit the bet");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        PokerCommand.Game game = PokerCommand.getOngoingGame(event);
        if (game == null) {
            event.getHook().editOriginal("❌ You do not have an ongoing poker hand!").queue();
            return;
        }

        if (game.isFinished()) {
            event.getHook().editOriginal("❌ Your poker hand is already finished!").queue();
            return;
        }

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.getHook().editOriginal("❌ You must use this command in your poker thread!").queue();
            return;
        }

        if (thread.getIdLong() != game.getChannel()) {
            event.getHook().editOriginal("❌ This is not your current poker thread!").queue();
            return;
        }

        final List<PokerCommand.Game> games = PokerCommand.GAMES.computeIfAbsent(guild.getIdLong(), ignored -> new ArrayList<>());

        synchronized (game) {
            game.fold();
            PokerCommand.Settlement settlement = game.getSettlement();
            applySettlement(account, settlement);
            games.remove(game);

            String stateMessage = PokerPlayCommand.buildStateMessage(game, config);
            String resultMessage = game.getResultMessage(number -> StringUtils.numberFormat(number, config));
            String content = stateMessage + "\n" + resultMessage;
            try (FileUpload upload = PokerImageRenderer.createUpload(game, true)) {
                event.getHook().editOriginal(content).setFiles(upload).queue();
            } catch (Exception exception) {
                Constants.LOGGER.error("Failed to create poker image!", exception);
                event.getHook().editOriginal(content + "\n❌ Failed to create the poker image.").queue();
            }

            thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
        }
    }
}
