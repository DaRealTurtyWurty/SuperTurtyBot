package dev.darealturtywurty.superturtybot.commands.core.quest;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.quest.Quest;
import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestManager;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;
import java.util.List;

public abstract class QuestSubcommand extends SubcommandCommand {
    protected QuestSubcommand(String name, String description) {
        super(name, description);
    }

    @Override
    public final void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "This command can only be used in a server.", false, true);
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            reply(event, "This command can only be used by a member.", false, true);
            return;
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        if (!guildData.isQuestEnabled()) {
            reply(event, "Quests are not enabled on this server.", false, true);
            return;
        }

        QuestPlayer player = QuestManager.INSTANCE.getOrCreateWeeklyQuests(guild, member);
        List<QuestEvent> events = QuestManager.INSTANCE.getQuestEvents(guild, member, player.getWeekStart());
        execute(event, guild, member, player, events);
    }

    protected abstract void execute(
        SlashCommandInteractionEvent event,
        Guild guild,
        Member member,
        QuestPlayer player,
        List<QuestEvent> events
    );

    protected static EmbedBuilder createQuestEmbed(Quest<?, ?> quest, QuestStatus status, QuestPlayer player) {
        return new EmbedBuilder()
            .setTitle(quest.getDisplayName())
            .setDescription(quest.getDescription())
            .setColor(getStatusColor(status, player.hasBeenRewarded(quest.getId())))
            .addField("Status", getStatusLabel(status, player.hasBeenRewarded(quest.getId())), true)
            .addField("Progress", status.displayProgress(), true)
            .addField("Reward", quest.getReward().getDescription(), true);
    }

    protected static String getStatusLabel(QuestStatus status, boolean rewarded) {
        if (rewarded || status.complete())
            return "Completed";

        if (status.progress() <= 0)
            return "Not started";

        return "In progress";
    }

    protected static Color getStatusColor(QuestStatus status, boolean rewarded) {
        if (rewarded || status.complete())
            return Color.GREEN;

        if (status.progress() <= 0)
            return Color.GRAY;

        return Color.ORANGE;
    }
}
