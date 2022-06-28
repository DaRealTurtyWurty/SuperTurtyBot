package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class WarnCommand extends CoreCommand {
    public WarnCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to warn", true),
            new OptionData(OptionType.STRING, "reason", "The reason for why you want to warn that user", false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Warns a user";
    }

    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public String getRichName() {
        return "Add Warning";
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
        final String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
        final WarnInfo warn = WarnManager.addWarn(user, event.getGuild(), event.getMember(), reason);

        final var embed = new EmbedBuilder();
        embed.setTitle(user.getName() + " has been warned!");
        embed.setFooter("Warned At: " + formatTime(Instant.ofEpochMilli(warn.warnTime()).atOffset(ZoneOffset.UTC)));
        embed.setDescription("Reason: " + warn.reason() + "\nWarned By: " + warn.warner().getAsMention() + "\nUUID: "
            + warn.uuid().toString());
        embed.setColor(Color.RED);
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
        // TODO: Option to notify user of warn
    }

    // TODO: Utility class
    private static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
