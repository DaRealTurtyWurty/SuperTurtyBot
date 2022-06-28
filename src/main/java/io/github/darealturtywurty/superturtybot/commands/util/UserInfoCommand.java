package io.github.darealturtywurty.superturtybot.commands.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
    public String getName() {
        return "userinfo";
    }

    @Override
    public String getRichName() {
        return "User Info";
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

    // TODO: Utility class
    private static String convertOnlineStatus(OnlineStatus status) {
        return switch (status) {
            case DO_NOT_DISTURB -> "Do Not Disturb";
            case IDLE -> "Idle";
            case INVISIBLE -> "Invisible";
            case OFFLINE -> "Offline";
            case ONLINE -> "Online";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
    }

    private static EmbedBuilder createEmbed(Member member) {
        final var embed = new EmbedBuilder();
        embed.setTitle("User Info for user: " + member.getUser().getName() + "#" + member.getUser().getDiscriminator());
        embed.setTimestamp(Instant.now());
        embed.setColor(member.getColorRaw());
        embed.addField("Nickname", member.getNickname() == null ? "N/A" : member.getNickname(), false);
        embed.addField("Mention", member.getAsMention(), false);
        embed.addField("Online Status", convertOnlineStatus(member.getOnlineStatus()), false);
        
        final var perms = new StringBuilder();
        member.getPermissions().forEach(perm -> perms.append("`" + perm.getName() + "`, "));
        perms.delete(perms.length() - 2, perms.length());
        embed.addField("Permissions", perms.toString(), false);

        final var roles = new StringBuilder();
        member.getRoles().forEach(role -> roles.append(role.getAsMention() + ", "));
        roles.delete(roles.length() - 2, roles.length());
        embed.addField("Roles", roles.toString(), false);

        if (member.isBoosting()) {
            embed.addField("Time Boosted", formatTime(member.getTimeBoosted()), false);
        }

        if (member.isTimedOut()) {
            embed.addField("Timeout End", formatTime(member.getTimeOutEnd()), false);
        }

        embed.addField("Created At", formatTime(member.getTimeCreated()), false);
        embed.addField("Joined At", formatTime(member.getTimeJoined()), false);
        embed.addField("Is Owner", StringUtils.trueFalseToYesNo(member.isOwner()), true);
        embed.addField("Is Bot", StringUtils.trueFalseToYesNo(member.getUser().isBot()), true);
        embed.addField("Is System", StringUtils.trueFalseToYesNo(member.getUser().isSystem()), true);
        embed.setThumbnail(member.getEffectiveAvatarUrl());
        
        embed.setFooter("ID: " + member.getIdLong());
        return embed;
    }

    // TODO: Utility class
    private static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
