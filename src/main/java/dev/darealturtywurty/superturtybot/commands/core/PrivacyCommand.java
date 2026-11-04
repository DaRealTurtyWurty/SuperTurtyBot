package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyDeleteSubcommand;
import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyExportSubcommand;
import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyInfoSubcommand;
import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyOperations;
import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyOptInSubcommand;
import dev.darealturtywurty.superturtybot.commands.core.privacy.PrivacyOptOutSubcommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public final class PrivacyCommand extends CoreCommand {
    public PrivacyCommand() {
        super(new Types(true, false, false, false));
        var operations = new PrivacyOperations();
        addSubcommands(new PrivacyInfoSubcommand(), new PrivacyExportSubcommand(operations),
            new PrivacyDeleteSubcommand(operations), new PrivacyOptOutSubcommand(operations),
            new PrivacyOptInSubcommand(operations));
    }

    @Override
    public String getName() {
        return "privacy";
    }

    @Override
    public String getDescription() {
        return "Read the privacy policy, export or delete your data, or manage optional data processing";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getHowToUse() {
        return "/privacy info | /privacy export | /privacy delete | /privacy opt-out | /privacy opt-in";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected boolean recordsQuestActivity() {
        return false;
    }
}
