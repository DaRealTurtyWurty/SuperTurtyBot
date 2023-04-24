package io.github.darealturtywurty.superturtybot.modules.idlerpg.commands;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class RPGCommand extends CoreCommand {
    protected RPGCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.RPG;
    }

    protected RPGPlayer getStats(MessageReceivedEvent event) {
        final Bson filter = getFilter(event);

        final RPGPlayer found = Database.getDatabase().rpgStats.find(filter).first();
        if (found == null) {
            reply(event, "â�Œ You do not have a profile on this server! Use `.start` to get started!");
            return null;
        }

        return found;
    }
    
    protected boolean requiresProfile() {
        return true;
    }

    protected abstract void run(MessageReceivedEvent event);

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (!event.isFromGuild() || requiresProfile() && getStats(event) == null)
            return;

        run(event);
    }

    public static Bson getFilter(MessageReceivedEvent event) {
        return Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getAuthor().getIdLong()));
    }
}
