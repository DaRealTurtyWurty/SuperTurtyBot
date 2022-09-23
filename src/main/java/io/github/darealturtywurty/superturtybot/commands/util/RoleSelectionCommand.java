package io.github.darealturtywurty.superturtybot.commands.util;

import java.util.List;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class RoleSelectionCommand extends CoreCommand {
    public RoleSelectionCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("create", "Creates a role selection menu")
                .addOption(OptionType.CHANNEL, "channel", "The channel to create this role selection menu in", true)
                .addOption(OptionType.STRING, "title", "The title for this role selection menu", true)
                .addOption(OptionType.ROLE, "role", "The default role to add to this menu", true)
                .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true)
                .addOption(OptionType.STRING, "description", "The description for this role", true),
            new SubcommandData("add", "Adds to an existing role selection menu")
                .addOption(OptionType.STRING, "menu-id", "The ID of the role selection menu to add to", true, true)
                .addOption(OptionType.ROLE, "role", "The role to add to this menu", true)
                .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true)
                .addOption(OptionType.STRING, "description", "The description for this role", true),
            new SubcommandData("delete", "Deletes an existing role selection menu").addOption(OptionType.STRING,
                "menu-id", "The ID of the role selection menu to add to", true, true),
            new SubcommandData("remove", "Removes a role from an existing role selection menu")
                .addOption(OptionType.STRING, "menu-id", "The ID of the role selection menu to add to", true, true)
                .addOption(OptionType.ROLE, "role", "The role to remove from this selection menu", true),
            new SubcommandData("list", "Lists all of the role selection menus in this server"));
    }

    @Override
    public String getAccess() {
        return "Moderator Only (Manage Server)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Creates a role selection menu";
    }
    
    @Override
    public String getHowToUse() {
        return "/role-selection create [channel] [title] [role] [emoji] [description]\n/role-selection add [id] [role] [emoji] [description]\n/role-selection delete [id]\n/role-selection remove [id] [role]\n/role-selection list";
    }
    
    @Override
    public String getName() {
        return "role-selection";
    }

    @Override
    public String getRichName() {
        return "Role Selection";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }
        
        final String subcommand = event.getSubcommandName();
    }
}
