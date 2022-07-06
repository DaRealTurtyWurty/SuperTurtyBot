package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ClearWarningsCommand extends CoreCommand {
    public ClearWarningsCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to clear warns from", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Clears all warns from a user";
    }
    
    @Override
    public String getName() {
        return "clearwarns";
    }
    
    @Override
    public String getRichName() {
        return "Clear Warnings";
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
        final Set<Warning> warns = WarnManager.clearWarnings(event.getGuild(), user);
        
        final var embed = new EmbedBuilder();
        embed.setColor(Color.GREEN);
        embed.setTitle(user.getName() + "'s warns has been cleared!");
        embed.setDescription("Warns Removed: " + warns.size() + "\nRemoved By: " + event.getMember().getAsMention());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
        // TODO: Option to notify user of warnings clear
    }
}
