package dev.darealturtywurty.superturtybot.commands.util;

import java.time.Instant;
import java.util.List;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
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
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

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
                .addOption(OptionType.STRING, "message-url",
                    "The URL of the message that contains the role selection menu", true, true)
                .addOption(OptionType.ROLE, "role", "The role to add to this menu", true)
                .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true)
                .addOptions(new OptionData(OptionType.STRING, "description", "The description for this role", true)
                    .setRequiredLength(1, 1028)),
            new SubcommandData("delete", "Deletes an existing role selection menu").addOption(OptionType.STRING,
                "message-url", "The URL of the message that contains the role selection menu", true, true),
            new SubcommandData("remove", "Removes a role from an existing role selection menu")
                .addOption(OptionType.STRING, "message-url",
                    "The URL of the message that contains the role selection menu", true, true)
                .addOption(OptionType.ROLE, "role", "The role to remove from this selection menu", true));
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
        return """
            /role-selection create [channel] [title] [role] [emoji] [description] {embedColor}
            /role-selection add [messageURL] [role] [emoji] [description]
            /role-selection delete [messageURL]
            /role-selection remove [messageURL] [role]""";
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
            if (role == null) {
                event.deferReply(true).setContent("❌ The role that you have selected is invalid!")
                    .mentionRepliedUser(false).queue();
                return;
            }
            
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
                
                if ((channel == null) || (channel.getGuild().getIdLong() != event.getGuild().getIdLong())) {
                    reply(event, "❌ The channel that you have specified is invalid!", false, true);
                    return;
                }
                
                if (role.getGuild().getIdLong() != event.getGuild().getIdLong()) {
                    reply(event, "❌ The role that you have specified is invalid!", false, true);
                    return;
                }
                
                if (emoji.length() > 64) {
                    reply(event, "❌ The emoji that you have specified is invalid!", false, true);
                    return;
                }
                
                if (description.length() > 1028) {
                    reply(event, "❌ The description that you have specified is invalid!", false, true);
                    return;
                }
                
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
            
            case "add" -> {
                String messageURL = event.getOption("message-url").getAsString();
                if (!messageURL.startsWith("https://discord.com/channels/")) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                messageURL = messageURL.replace("https://discord.com/channels/", "");
                final String[] split = messageURL.split("/");
                if (split.length != 3) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long guildId;
                try {
                    guildId = Long.parseLong(split[0]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long channelId;
                try {
                    channelId = Long.parseLong(split[1]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long messageId;
                try {
                    messageId = Long.parseLong(split[2]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Guild guild = event.getJDA().getGuildById(guildId);
                if (guild == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Message message = channel.retrieveMessageById(messageId).complete();
                if (message == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Role role = event.getOption("role").getAsRole();
                final String emoji = event.getOption("emoji").getAsString();
                final String description = event.getOption("description").getAsString();
                
                SelectMenu menu = null;
                for (final Component component : message.getComponents()) {
                    if (component instanceof final ActionRow row) {
                        for (final Component column : row.getComponents()) {
                            if (column instanceof final SelectMenu selection) {
                                menu = selection;
                                break;
                            }
                        }
                    }
                }
                
                if (menu == null) {
                    reply(event, "❌ The message that you have provided does not have a role selection menu!", false,
                        true);
                    return;
                }
                
                if (menu.getOptions().size() >= SelectMenu.OPTIONS_MAX_AMOUNT) {
                    reply(event,
                        "❌ The role selection menu that you have provided has reached the maximum amount of roles!",
                        false, true);
                    return;
                }
                
                if (menu.getOptions().stream().anyMatch(option -> option.getLabel().equals(role.getName()))) {
                    reply(event, "❌ The role selection menu that you have provided already has this role!", false,
                        true);
                    return;
                }
                
                menu.getOptions().add(SelectOption.of(role.getName(), role.getId())
                    .withEmoji(Emoji.fromFormatted(emoji)).withDescription(description));
                message.editMessageComponents(ActionRow.of(menu)).queue();
                reply(event, "✅ I have added the role to the role selection menu!");
            }
            
            case "remove" -> {
                String messageURL = event.getOption("message-url").getAsString();
                if (!messageURL.startsWith("https://discord.com/channels/")) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                messageURL = messageURL.replace("https://discord.com/channels/", "");
                final String[] split = messageURL.split("/");
                if (split.length != 3) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long guildId;
                try {
                    guildId = Long.parseLong(split[0]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long channelId;
                try {
                    channelId = Long.parseLong(split[1]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long messageId;
                try {
                    messageId = Long.parseLong(split[2]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Guild guild = event.getJDA().getGuildById(guildId);
                if (guild == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Message message = channel.retrieveMessageById(messageId).complete();
                if (message == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Role role = event.getOption("role").getAsRole();
                
                SelectMenu menu = null;
                for (final Component component : message.getComponents()) {
                    if (component instanceof final ActionRow row) {
                        for (final Component column : row.getComponents()) {
                            if (column instanceof final SelectMenu selection) {
                                menu = selection;
                                break;
                            }
                        }
                    }
                }
                
                if (menu == null) {
                    reply(event, "❌ The message that you have provided does not have a role selection menu!", false,
                        true);
                    return;
                }
                
                if (menu.getOptions().stream().noneMatch(option -> option.getLabel().equals(role.getName()))) {
                    reply(event, "❌ The role selection menu that you have provided does not have this role!", false,
                        true);
                    return;
                }
                
                menu.getOptions().removeIf(option -> option.getLabel().equals(role.getName()));
                message.editMessageComponents(ActionRow.of(menu)).queue();
                reply(event, "✅ I have removed the role from the role selection menu!");
            }
            
            case "delete" -> {
                String messageURL = event.getOption("message-url").getAsString();
                if (!messageURL.startsWith("https://discord.com/channels/")) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                messageURL = messageURL.replace("https://discord.com/channels/", "");
                final String[] split = messageURL.split("/");
                if (split.length != 3) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long guildId;
                try {
                    guildId = Long.parseLong(split[0]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long channelId;
                try {
                    channelId = Long.parseLong(split[1]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final long messageId;
                try {
                    messageId = Long.parseLong(split[2]);
                } catch (final NumberFormatException exception) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Guild guild = event.getJDA().getGuildById(guildId);
                if (guild == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                final Message message = channel.retrieveMessageById(messageId).complete();
                if (message == null) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }
                
                SelectMenu menu = null;
                for (final Component component : message.getComponents()) {
                    if (component instanceof final ActionRow row) {
                        for (final Component column : row.getComponents()) {
                            if (column instanceof final SelectMenu selection) {
                                menu = selection;
                                break;
                            }
                        }
                    }
                }
                
                if (menu == null) {
                    reply(event, "❌ The message that you have provided does not have a role selection menu!", false,
                        true);
                    return;
                }
                
                message.delete().queue();
                reply(event, "✅ I have deleted the role selection menu!");
            }
        }
    }
}