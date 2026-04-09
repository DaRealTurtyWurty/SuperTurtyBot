package dev.darealturtywurty.superturtybot.commands.moderation;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserEmbeds;
import dev.darealturtywurty.superturtybot.commands.moderation.sticky.StickyClearSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.sticky.StickyEmbedSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.sticky.StickyTextSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.sticky.StickyViewSubcommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class StickyCommand extends CoreCommand {
    public StickyCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new StickyTextSubcommand(),
                new StickyEmbedSubcommand(),
                new StickyViewSubcommand(),
                new StickyClearSubcommand()
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getAccess() {
        return "Moderators (Manage Messages Permission)";
    }

    @Override
    public String getDescription() {
        return "Creates sticky messages for support channels using plain text or saved embeds";
    }

    @Override
    public String getHowToUse() {
        return """
                /sticky text <channel> <content>
                /sticky embed <channel> <name>
                /sticky view <channel>
                /sticky clear <channel>""";
    }

    @Override
    public String getName() {
        return "sticky";
    }

    @Override
    public String getRichName() {
        return "Sticky Message";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()) || !"embed".equals(event.getSubcommandName())
                || !"name".equals(event.getFocusedOption().getName()))
            return;

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", event.getUser().getIdLong())).first();
        if (userEmbeds == null) {
            event.replyChoices().queue();
            return;
        }

        String value = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        event.replyChoiceStrings(userEmbeds.getEmbeds().keySet().stream()
                .filter(name -> value.isBlank() || name.toLowerCase(Locale.ROOT).contains(value))
                .limit(25)
                .toList()).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "❌ You must provide a valid subcommand!", false, true);
    }
}
