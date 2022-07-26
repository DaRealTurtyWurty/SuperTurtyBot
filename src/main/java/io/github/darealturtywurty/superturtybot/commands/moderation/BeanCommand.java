package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.util.List;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class BeanCommand extends CoreCommand {
    public BeanCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to bean!", true),
            new OptionData(OptionType.INTEGER, "delete_days", "Number of days to delete this user's messages", false)
                .setRequiredRange(0, 7),
            new OptionData(OptionType.STRING, "reason", "The bean reason", false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Beans a user";
    }
    
    @Override
    public String getHowToUse() {
        return "/bean [user]\n/bean [user] [deleteDays]\n/bean [user] [reason]\n/bean [user] [deleteDays] [reason]\n/bean [user] [reason] [deleteDays]";
    }
    
    @Override
    public String getName() {
        return "bean";
    }
    
    @Override
    public String getRichName() {
        return "bean";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild())
            return;
        
        final User user = event.getOption("user").getAsUser();
        event.deferReply().setContent("Successfully beaned " + user.getAsMention() + "!").mentionRepliedUser(false)
            .queue();
    }
}
