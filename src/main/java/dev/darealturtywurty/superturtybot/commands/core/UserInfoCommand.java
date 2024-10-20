package dev.darealturtywurty.superturtybot.commands.core;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.WordleProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public class UserInfoCommand extends CoreCommand {
    public UserInfoCommand() {
        super(new Types(true, false, false, true));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to get information about.", false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Retrives information about a user.";
    }
    
    @Override
    public String getHowToUse() {
        return "/userinfo\n/userinfo [user]";
    }
    
    @Override
    public String getName() {
        return "userinfo";
    }
    
    @Override
    public String getRichName() {
        return "User Info";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "You must be in a server to use this command!", false, true);
            return;
        }
        
        final OptionMapping userOption = event.getOption("user");
        if (userOption != null && userOption.getAsMember() == null) {
            reply(event, "❌ You can only use this command on a member of this server!", false, true);
            return;
        }
        
        final Member member = userOption == null ? event.getMember() : userOption.getAsMember();
        if (member == null) {
            reply(event, "❌ You can only use this command on a member of this server!", false, true);
            return;
        }

        final EmbedBuilder embed = createEmbed(member);
        reply(event, embed, false);
    }
    
    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("You must be in a server to use this command!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final Member member = event.getTargetMember();
        if (member == null) {
            event.deferReply(true).setContent("You can only use this command on a member of this server!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final EmbedBuilder embed = createEmbed(member);
        reply(event, embed, false);
    }
    
    private static EmbedBuilder createEmbed(@NotNull Member member) {
        final var embed = new EmbedBuilder();
        embed.setTitle("User Info for user: " + member.getUser().getEffectiveName());
        embed.setTimestamp(Instant.now());
        embed.setColor(member.getColorRaw());
        embed.addField("Nickname", member.getNickname() == null ? "N/A" : member.getNickname(), false);
        embed.addField("Mention", member.getAsMention(), false);
        if(member.getJDA().getCacheFlags().contains(CacheFlag.ONLINE_STATUS)) {
            embed.addField("Online Status", StringUtils.convertOnlineStatus(member.getOnlineStatus()), false);
        }
        
        final var perms = new StringBuilder();
        member.getPermissions().forEach(perm -> perms.append("`").append(perm.getName()).append("`, "));
        perms.delete(perms.length() - 2, perms.length());
        embed.addField("Permissions", perms.toString(), false);
        
        if (!member.getRoles().isEmpty()) {
            final var roles = new StringBuilder();
            member.getRoles().forEach(role -> roles.append(role.getAsMention()).append(", "));
            
            roles.delete(roles.length() - 2, roles.length());
            embed.addField("Roles", roles.toString(), false);
        }
        
        if (member.getTimeBoosted() != null) {
            embed.addField("Time Boosted", TimeFormat.TIME_SHORT.format(member.getTimeBoosted()), false);
        }
        
        if (member.getTimeOutEnd() != null && member.getTimeOutEnd().isAfter(OffsetDateTime.from(Instant.now()))) {
            embed.addField("Timeout End", TimeFormat.TIME_SHORT.format(member.getTimeOutEnd()), false);
        }
        
        embed.addField("Created", TimeFormat.RELATIVE.format(member.getTimeCreated()), false);
        embed.addField("Joined", TimeFormat.RELATIVE.format(member.getTimeJoined()), false);
        embed.addField("Is Owner", StringUtils.trueFalseToYesNo(member.isOwner()), true);
        embed.addField("Is Bot", StringUtils.trueFalseToYesNo(member.getUser().isBot()), true);
        embed.addField("Is System", StringUtils.trueFalseToYesNo(member.getUser().isSystem()), true);

        WordleProfile profile = Database.getDatabase().wordleProfiles.find(Filters.eq("user", member.getIdLong())).first();
        if (profile != null) {
            WordleStreakData streakData = profile.getStreaks().stream().filter(streak -> streak.getGuild() == member.getGuild().getIdLong()).findFirst().orElse(null);
            if (streakData != null) {
                embed.addField("Wordle Streak", streakData.getStreak() + " days", true);
                embed.addField("Wordle Best Streak", streakData.getBestStreak() + " days", true);
            }
        }

        embed.setThumbnail(member.getEffectiveAvatarUrl());
        
        embed.setFooter("ID: " + member.getIdLong());
        return embed;
    }
}
