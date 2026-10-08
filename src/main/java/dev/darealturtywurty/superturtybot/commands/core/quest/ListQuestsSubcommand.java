package dev.darealturtywurty.superturtybot.commands.core.quest;

import dev.darealturtywurty.superturtybot.modules.quest.Quest;
import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestManager;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public class ListQuestsSubcommand extends QuestSubcommand {
    public ListQuestsSubcommand() {
        super("list", "List all available quests.");
    }

    @Override
    protected void execute(
        SlashCommandInteractionEvent event,
        Guild guild,
        Member member,
        QuestPlayer player,
        List<QuestEvent> events
    ) {
        List<MessageEmbed> embeds = new ArrayList<>();
        for (String questId : player.getAssignedQuestIds()) {
            Quest<?, ?> quest = QuestManager.QUESTS.get(questId);
            if (quest == null)
                continue;

            QuestStatus status = quest.evaluate(events);
            embeds.add(createQuestEmbed(quest, status, player).build());
        }

        if (embeds.isEmpty()) {
            reply(event, "You do not have any quests assigned this week.", false, true);
            return;
        }

        event.replyEmbeds(embeds).queue();
    }
}
