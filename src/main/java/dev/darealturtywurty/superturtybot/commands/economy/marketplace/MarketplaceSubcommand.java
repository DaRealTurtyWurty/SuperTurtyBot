package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import com.mongodb.MongoException;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class MarketplaceSubcommand extends SubcommandCommand {
    protected MarketplaceSubcommand(String name, String description) {
        super(name, description);
    }

    @Override
    public final void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "This command is only available in servers.", false, true);
            return;
        }
        event.deferReply().mentionRepliedUser(false).queue();
        GuildData config = GuildData.getOrCreateGuildData(guild);
        if (!config.isEconomyEnabled()) {
            event.getHook().editOriginalEmbeds(MarketplaceCommand.notice("Marketplace unavailable",
                    "Economy is not enabled in this server.", true).build()).queue();
            return;
        }
        try {
            execute(event, guild, config);
        } catch (IllegalArgumentException exception) {
            event.getHook().editOriginalEmbeds(MarketplaceCommand.notice("Marketplace action failed",
                    exception.getMessage(), true).build()).queue();
        } catch (MongoException exception) {
            Constants.LOGGER.error("Marketplace action failed", exception);
            event.getHook().editOriginalEmbeds(MarketplaceCommand.notice("Marketplace unavailable",
                    "Please try again later.", true).build()).queue();
        }
    }

    protected abstract void execute(SlashCommandInteractionEvent event, Guild guild, GuildData config);
}
