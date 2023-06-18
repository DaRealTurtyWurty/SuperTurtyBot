package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
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
        return CommandCategory.UTILITY;
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
            event.deferReply(true).setContent("You must be in a server to use this command!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final OptionMapping userOption = event.getOption("user");
        if (userOption != null && userOption.getAsMember() == null) {
            event.deferReply(true).setContent("You can only use this command on a member of this server!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final Member member = userOption == null ? event.getMember() : userOption.getAsMember();
        final EmbedBuilder embed = createEmbed(member);
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
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
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }
    
    private static EmbedBuilder createEmbed(Member member) {
        final var embed = new EmbedBuilder();
        embed.setTitle("User Info for user: " + member.getUser().getName());
        embed.setTimestamp(Instant.now());
        embed.setColor(member.getColorRaw());
        embed.addField("Nickname", member.getNickname() == null ? "N/A" : member.getNickname(), false);
        embed.addField("Mention", member.getAsMention(), false);
        embed.addField("Online Status", StringUtils.convertOnlineStatus(member.getOnlineStatus()), false);
        
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
        
        if (member.isBoosting()) {
            embed.addField("Time Boosted", StringUtils.formatTime(member.getTimeBoosted()), false);
        }
        
        if (member.isTimedOut()) {
            embed.addField("Timeout End", StringUtils.formatTime(member.getTimeOutEnd()), false);
        }
        
        embed.addField("Created At", StringUtils.formatTime(member.getTimeCreated()), false);
        embed.addField("Joined At", StringUtils.formatTime(member.getTimeJoined()), false);
        embed.addField("Is Owner", StringUtils.trueFalseToYesNo(member.isOwner()), true);
        embed.addField("Is Bot", StringUtils.trueFalseToYesNo(member.getUser().isBot()), true);
        embed.addField("Is System", StringUtils.trueFalseToYesNo(member.getUser().isSystem()), true);
        embed.setThumbnail(member.getEffectiveAvatarUrl());
        
        embed.setFooter("ID: " + member.getIdLong());
        return embed;
    }
}
