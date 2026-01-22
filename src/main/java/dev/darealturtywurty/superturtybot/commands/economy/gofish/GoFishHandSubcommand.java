package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

public class GoFishHandSubcommand extends GoFishSubcommand {
    protected GoFishHandSubcommand() {
        super("hand", "Show your current hand (ephemeral)");
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

        GoFishCommand.PlayerState player = game.getPlayer(event.getUser().getIdLong());
        if (player == null) {
            event.getHook().editOriginal("❌ You are not part of this Go Fish game.").queue();
            return;
        }

        String message = "**Your hand:** " + GoFishCommand.renderHand(player)
                + "\n**Your books:** " + GoFishCommand.renderBooks(player);
        try (FileUpload upload = GoFishImageRenderer.createUpload(player.hand())) {
            event.getHook().editOriginal(message).setFiles(upload).queue();
        } catch (Exception exception) {
            Constants.LOGGER.error("Failed to create Go Fish hand image!", exception);
            event.getHook().editOriginal(message + "\n❌ Failed to render the hand image.").queue();
        }
    }
}
