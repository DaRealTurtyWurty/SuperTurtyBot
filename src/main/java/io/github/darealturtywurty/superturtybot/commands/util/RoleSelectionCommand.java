package io.github.darealturtywurty.superturtybot.commands.util;

import java.time.Instant;
import java.util.List;

import io.github.darealturtywurty.superturtybot.commands.core.config.Validators;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

public class RoleSelectionCommand extends CoreCommand {
    public RoleSelectionCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("create", "Creates a role selection menu")
                .addOption(OptionType.CHANNEL, "channel", "The channel to create this role selection menu in", true)
                .addOptions(new OptionData(OptionType.STRING, "title", "The title for this role selection menu", true)
                    .setRequiredLength(1, 1028))
                .addOption(OptionType.ROLE, "role", "The default role to add to this menu", true)
                .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true)
                .addOptions(new OptionData(OptionType.STRING, "description", "The description for this role", true)
                    .setRequiredLength(1, 1028))
                .addOptions(new OptionData(OptionType.INTEGER, "embed_color",
                    "The color that will be used for the embed. This defaults to blue if not specified!", false)
                        .addChoice("red", 0xFF0000).addChoice("green", 0x00FF00).addChoice("blue", 0x0000FF)
                        .addChoice("black", 0x000000).addChoice("white", 0xFFFFFF).addChoice("yellow", 0xFFFF00)
                        .addChoice("cyan", 0x00FFFF).addChoice("brown", 0xFF00FF).addChoice("orange", 0xFFA500)
                        .addChoice("purple", 0x800080).addChoice("pink", 0xFFC0CB)),
            new SubcommandData("add", "Adds to an existing role selection menu")
                .addOption(OptionType.STRING, "menu-id", "The ID of the role selection menu to add to", true, true)
                .addOption(OptionType.ROLE, "role", "The role to add to this menu", true)
                .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true)
                .addOptions(new OptionData(OptionType.STRING, "description", "The description for this role", true)
                    .setRequiredLength(1, 1028)),
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
        return "/role-selection create [channel] [title] [role] [emoji] [description] {embedColor}\n/role-selection add [id] [role] [emoji] [description]\n/role-selection delete [id]\n/role-selection remove [id] [role]\n/role-selection list";
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
    public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
        if (!event.isFromGuild())
            return;

        final String id = event.getComponentId();
        if (!id.startsWith("role-selection-"))
            return;

        long messageId;
        try {
            messageId = Long.parseLong(id.split("role-selection-")[1]);
        } catch (final NumberFormatException exception) {
            return;
        }

        if (messageId != event.getMessageIdLong())
            return;

        final Member member = event.getMember();
        final Guild guild = event.getGuild();
        for (final String value : event.getValues()) {
            long roleId;
            try {
                roleId = Long.parseLong(value);
            } catch (final NumberFormatException exception) {
                event.deferReply(true).setContent("❌ The role that you have selected is invalid!")
                    .mentionRepliedUser(false).queue();
                return;
            }

            final Role role = guild.getRoleById(roleId);
            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue();

            } else {
                guild.addRoleToMember(member, role).queue();
            }
        }
        
        event.deferReply(true).setContent("I have added/removed the selected roles!").mentionRepliedUser(false).queue();
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
        
        event.deferReply(true).queue();
        final String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "create" -> {
                final TextChannel channel = event.getOption("channel", null,
                    mapping -> mapping.getChannelType().isMessage() ? mapping.getAsChannel().asTextChannel() : null);
                
                final String title = event.getOption("title").getAsString();
                final Role role = event.getOption("role").getAsRole();
                final String emoji = event.getOption("emoji").getAsString();
                final String description = event.getOption("description").getAsString();
                final int color = event.getOption("embed_color", 0x0000FF, OptionMapping::getAsInt);
                if (!validateCreateOptions(event, channel, title, role, emoji, description))
                    return;
                
                final var embed = new EmbedBuilder();
                embed.setTitle(title);
                embed.setColor(color);
                embed.setTimestamp(Instant.now());
                embed.setFooter(event.getMember().getEffectiveName() + event.getUser().getDiscriminator(),
                    event.getMember().getEffectiveAvatarUrl());
                embed.addField(emoji + " " + role.getAsMention(), description, false);

                channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                    msg.editMessageComponents(ActionRow.of(SelectMenu.create("role-selection-" + msg.getId())
                        .addOption(role.getName(), role.getId(), Emoji.fromFormatted(emoji)).build())).queue();
                    event.getHook().editOriginal("✅ I have created this role selection menu at:\n" + msg.getJumpUrl())
                        .mentionRepliedUser(false).queue();
                });
            }
        }
    }

    private static boolean validateCreateOptions(SlashCommandInteractionEvent event, TextChannel channel, String title,
        Role role, String emoji, String description) {
        boolean success = false;
        if (!event.getMember().canInteract(role)) {
            event.getHook().editOriginal("❌ You do not have permission to interact with the provided role!")
                .mentionRepliedUser(false).queue();
        } else if (!event.getGuild().getSelfMember().canInteract(role)) {
            event.getHook().editOriginal("❌ I am unable to interact with the provided role!").mentionRepliedUser(false)
                .queue();
        } else if (!channel.canTalk(event.getMember())) {
            event.getHook().editOriginal("❌ You do not have permission to send messages in the provided channel!")
                .mentionRepliedUser(false).queue();
        } else if (!channel.canTalk()) {
            event.getHook().editOriginal("❌ I do not have permission to send messages in the provided channel!")
                .mentionRepliedUser(false).queue();
        } else if (title.isBlank()) {
            event.getHook().editOriginal("❌ The title of this role selection menu cannot be blank!")
                .mentionRepliedUser(false).queue();
        } else if (emoji.isBlank() || !Validators.EMOJI_VALIDATOR.test(event, emoji)) {
            event.getHook().editOriginal("❌ You must provide a valid emoji!").mentionRepliedUser(false).queue();
        } else if (description.isBlank()) {
            event.getHook().editOriginal("❌ The description of this role selection menu cannot be blank!")
                .mentionRepliedUser(false).queue();
        } else {
            success = true;
        }
        
        return success;
    }
}
