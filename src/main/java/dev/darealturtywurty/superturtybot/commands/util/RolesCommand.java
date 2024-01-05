package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;

public class RolesCommand extends CoreCommand {
    public RolesCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Displays all the roles in this discord server.";
    }

    @Override
    public String getName() {
        return "roles";
    }

    @Override
    public String getRichName() {
        return "Server Roles";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.deferReply(true).setContent("You must be in a server to use this command!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final EmbedBuilder embed = createEmbed(event.getGuild());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private EmbedBuilder createEmbed(Guild guild) {
        final var embed = new EmbedBuilder();
        embed.setTitle("List of roles for server: " + guild.getName());
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.MAGENTA);
        guild.getRoles().stream().map(Role::getAsMention).forEach(role -> embed.appendDescription(role + "\n"));
        return embed;
    }
}
