package dev.darealturtywurty.superturtybot.commands.core.quest;

import dev.darealturtywurty.superturtybot.modules.quest.Quest;
import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestManager;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;

public class ViewQuestSubcommand extends QuestSubcommand {
    public ViewQuestSubcommand() {
        super("view", "View the details and progress of one of your quests.");
        addOption(OptionType.STRING, "quest", "The quest to view.", true, true);
    }

    @Override
    protected void execute(
        SlashCommandInteractionEvent event,
        Guild guild,
        Member member,
        QuestPlayer player,
        List<QuestEvent> events
    ) {
        String questId = event.getOption("quest", "", OptionMapping::getAsString);
        if (!player.getAssignedQuestIds().contains(questId)) {
            reply(event, "That quest is not assigned to you this week.", false, true);
            return;
        }

        Quest<?, ?> quest = QuestManager.QUESTS.get(questId);
        if (quest == null) {
            reply(event, "That quest is no longer available.", false, true);
            return;
        }

        QuestStatus status = quest.evaluate(events);
        reply(event, createQuestEmbed(quest, status, player));
    }
}
