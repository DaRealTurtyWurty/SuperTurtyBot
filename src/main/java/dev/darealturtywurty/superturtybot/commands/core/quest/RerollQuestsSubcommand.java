package dev.darealturtywurty.superturtybot.commands.core.quest;

import dev.darealturtywurty.superturtybot.modules.quest.Quest;
import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestManager;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;
import java.util.Objects;

public class RerollQuestsSubcommand extends QuestSubcommand {
    public RerollQuestsSubcommand() {
        super("reroll", "Replace all three weekly quests with new quests.");
    }

    @Override
    protected void execute(
        SlashCommandInteractionEvent event,
        Guild guild,
        Member member,
        QuestPlayer player,
        List<QuestEvent> events
    ) {
        if (player.isRerollUsed()) {
            reply(event, "You have already used your quest reroll this week.", false, true);
            return;
        }

        boolean hasCompletedQuest = player.getAssignedQuestIds().stream()
            .map(QuestManager.QUESTS::get)
            .filter(Objects::nonNull)
            .anyMatch(quest -> player.hasBeenRewarded(quest.getId()) || quest.evaluate(events).complete());
        if (hasCompletedQuest) {
            reply(event, "You cannot reroll after completing one of this week's quests.", false, true);
            return;
        }

        try {
            List<Quest<?, ?>> replacements = QuestManager.INSTANCE.rerollQuests(guild, member, player);
            List<MessageEmbed> embeds = replacements.stream()
                .map(quest -> createQuestEmbed(quest, quest.evaluate(events), player).build())
                .toList();
            event.reply("Rerolled all three of your weekly quests.")
                .addEmbeds(embeds)
                .setEphemeral(true)
                .queue();
        } catch (IllegalStateException exception) {
            reply(event, exception.getMessage(), false, true);
        }
    }
}
