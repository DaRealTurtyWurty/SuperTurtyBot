package dev.darealturtywurty.superturtybot.commands.core.quest;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.quest.QuestManager;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class QuestCommand extends CoreCommand {
    public QuestCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new ListQuestsSubcommand(),
                new ViewQuestSubcommand(),
                new RerollQuestsSubcommand()
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        super.onCommandAutoCompleteInteraction(event);
        if (!event.getName().equals(getName()) || !"quest".equals(event.getFocusedOption().getName()))
            return;

        String subcommand = event.getSubcommandName();
        if (!"view".equals(subcommand))
            return;

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null || !GuildData.getOrCreateGuildData(guild).isQuestEnabled()) {
            event.replyChoices().queue();
            return;
        }

        try {
            QuestPlayer player = QuestManager.INSTANCE.getOrCreateWeeklyQuests(guild, member);
            String input = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
            List<Command.Choice> choices = player.getAssignedQuestIds().stream()
                    .map(QuestManager.QUESTS::get)
                    .filter(Objects::nonNull)
                    .filter(quest -> quest.getDisplayName().toLowerCase(Locale.ROOT).contains(input)
                            || quest.getId().toLowerCase(Locale.ROOT).contains(input))
                    .limit(25)
                    .map(quest -> new Command.Choice(quest.getDisplayName(), quest.getId()))
                    .toList();
            event.replyChoices(choices).queue();
        } catch (RuntimeException exception) {
            event.replyChoices().queue();
        }
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "View and manage quests.";
    }

    @Override
    public String getName() {
        return "quest";
    }

    @Override
    public String getRichName() {
        return "Quest";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
