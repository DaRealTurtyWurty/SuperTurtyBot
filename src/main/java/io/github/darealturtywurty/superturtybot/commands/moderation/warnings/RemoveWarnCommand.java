package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import io.github.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RemoveWarnCommand extends CoreCommand {
    public RemoveWarnCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to remove a warn from", true),
            new OptionData(OptionType.STRING, "uuid", "The ID of the warn that you want to remove", true));
    }
    
    @Override
    public String getAccess() {
        return "Moderators (Ban Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Removes a warn from a user";
    }
    
    @Override
    public String getHowToUse() {
        return "/removewarn [user] [warnUUID]";
    }
    
    @Override
    public String getName() {
        return "removewarn";
    }
    
    @Override
    public String getRichName() {
        return "Remove Warning";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("This command can only be used inside of a server!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.deferReply(true).setContent("You require the `Ban Members` permission to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final User user = event.getOption("user").getAsUser();
        final String uuid = event.getOption("uuid").getAsString();

        final Warning warn = WarnManager.removeWarn(user, event.getGuild(), uuid);

        event.getUser().openPrivateChannel()
            .queue(channel -> channel
                .sendMessage(
                    "Your warning (`" + warn.getUuid() + "`) on `" + event.getGuild().getName() + "` has been removed!")
                .queue(success -> {
                }, error -> {
                }));
        
        event.getJDA().retrieveUserById(warn.getWarner()).queue(warner -> {
            final var embed = new EmbedBuilder();
            embed.setColor(Color.GREEN);
            embed.setTitle(user.getName() + "'s warn has been removed!");
            embed.setDescription("Warn Reason: " + warn.getReason() + "\nOriginal Warner: " + warner.getAsMention()
                + "\nWarned At: " + formatTime(Instant.ofEpochMilli(warn.getWarnedAt()).atOffset(ZoneOffset.UTC))
                + "\nWarn UUID: " + warn.getUuid() + "\nRemoved By: " + event.getMember().getAsMention());
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
            
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has removed warn `"
                    + warn.getUuid() + "` from " + user.getAsMention() + "!", true);
            }
        });
    }
    
    // TODO: Utility class
    private static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
