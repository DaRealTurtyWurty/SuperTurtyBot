package dev.darealturtywurty.superturtybot.modules.rpg.command;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RPGProfileCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "View your RPG profile!";
    }

    @Override
    public String getName() {
        return "rpgprofile";
    }

    @Override
    public String getRichName() {
        return "View Profile";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(
                OptionType.USER,
                "user",
                "The user you want to view the profile of.",
                false));
    }

    @Override
    protected void handleSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);

        RPGPlayer player = RPGManager.getPlayer(guild.getIdLong(), user.getIdLong());
        if (player == null) {
            reply(event, "❌ This user has not started their RPG adventure!", false, true);
            return;
        }

        final var embed = new EmbedBuilder();
        embed.setColor(event.getMember().getColorRaw());
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getMember().getEffectiveName(), event.getMember().getEffectiveAvatarUrl());
        embed.setTitle(event.getMember().getEffectiveName() + "'s Profile");
        embed.setThumbnail(event.getMember().getEffectiveAvatarUrl());
        embed.setDescription(
                "**__Level__**: " + player.getLevel() + "\n**__XP__**: " + player.getXp() + "\n**__Health__**: "
                        + player.getHealth() + "/" + player.getMaxHealth() + "\n**__Money__**: $" + player.getGold());

        event.replyEmbeds(embed.build()).queue();
    }
}
