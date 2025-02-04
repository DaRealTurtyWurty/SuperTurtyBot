package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public abstract class EconomyCommand extends CoreCommand {
    public EconomyCommand() {
        super(new Types(true, false, false, false));
    }

    protected EconomyCommand(Types types) {
        super(types);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected final void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if(guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        GuildData config = GuildData.getOrCreateGuildData(guild);

        if(!config.isEconomyEnabled()) {
            event.getHook().sendMessage("❌ Economy is not enabled in this server!").queue();
            return;
        }

        runSlash(event, guild, config);
    }

    @Override
    protected final void runNormalMessage(MessageReceivedEvent event) {
        if(!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false);
            return;
        }

        Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        if(!config.isEconomyEnabled()) {
            reply(event, "❌ Economy is not enabled in this server!", false);
            return;
        }

        runNormalMessage(event, guild, config);
    }

    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {}
    protected void runNormalMessage(MessageReceivedEvent event, Guild guild, GuildData config) {}
}