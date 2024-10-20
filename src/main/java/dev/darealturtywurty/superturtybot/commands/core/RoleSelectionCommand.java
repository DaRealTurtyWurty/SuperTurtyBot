package dev.darealturtywurty.superturtybot.commands.core;

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
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RoleSelectionCommand extends CoreCommand {
    public RoleSelectionCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("create", "Creates a role selection menu").addOption(OptionType.CHANNEL, "channel",
                                "The channel to create this role selection menu in", true).addOptions(
                                new OptionData(OptionType.STRING, "title", "The title for this role selection menu",
                                        true).setRequiredLength(1, 1028))
                        .addOption(OptionType.ROLE, "role", "The default role to add to this menu", true)
                        .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true).addOptions(
                                new OptionData(OptionType.STRING, "description", "The description for this role",
                                        true).setRequiredLength(1, 1028)).addOptions(
                                new OptionData(OptionType.INTEGER, "embed_color",
                                        "The color that will be used for the embed. This defaults to blue if not specified!",
                                        false).addChoice("red", 0xFF0000).addChoice("green", 0x00FF00)
                                        .addChoice("blue", 0x0000FF).addChoice("black", 0x000000).addChoice("white", 0xFFFFFF)
                                        .addChoice("yellow", 0xFFFF00).addChoice("cyan", 0x00FFFF).addChoice("brown", 0xFF00FF)
                                        .addChoice("orange", 0xFFA500).addChoice("purple", 0x800080)
                                        .addChoice("pink", 0xFFC0CB)),
                new SubcommandData("add", "Adds to an existing role selection menu").addOption(OptionType.STRING,
                                "message-url", "The URL of the message that contains the role selection menu", true, true)
                        .addOption(OptionType.ROLE, "role", "The role to add to this menu", true)
                        .addOption(OptionType.STRING, "emoji", "The emoji to use for this role", true, true).addOptions(
                                new OptionData(OptionType.STRING, "description", "The description for this role",
                                        true).setRequiredLength(1, 1028)),
                new SubcommandData("delete", "Deletes an existing role selection menu").addOption(OptionType.STRING,
                        "message-url", "The URL of the message that contains the role selection menu", true, true),
                new SubcommandData("remove", "Removes a role from an existing role selection menu").addOption(
                                OptionType.STRING, "message-url",
                                "The URL of the message that contains the role selection menu", true, true)
                        .addOption(OptionType.ROLE, "role", "The role to remove from this selection menu", true));
    }

    @Override
    public String getAccess() {
        return "Moderator Only (Manage Server)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        final String id = event.getComponentId();
        if (!id.startsWith("role-selection-")) return;

        long messageId;
        try {
            messageId = Long.parseLong(id.split("role-selection-")[1]);
        } catch (final NumberFormatException exception) {
            return;
        }

        if (messageId != event.getMessageIdLong()) return;

        final Member member = event.getMember();
        final Guild guild = event.getGuild();
        List<String> values = event.getValues();

        for (final String value : values) {
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

            if (member == null) {
                event.deferReply(true).setContent("❌ You are not a member of this server!").mentionRepliedUser(false)
                        .queue();
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
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final Member member = event.getMember();
        if (member == null) {
            reply(event, "❌ You are not a member of this server!", false, true);
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }

        final String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ The subcommand that you have selected is invalid!", false, true);
            return;
        }

        switch (subcommand) {
            case "create" -> {
                final TextChannel channel = event.getOption("channel", null,
                        mapping -> mapping.getChannelType().isMessage() ? mapping.getAsChannel()
                                .asTextChannel() : null);

                final String title = event.getOption("title", "Unknown", OptionMapping::getAsString);
                final Role role = event.getOption("role", null, OptionMapping::getAsRole);
                final String emoji = event.getOption("emoji", "❓", OptionMapping::getAsString);
                final String description = event.getOption("description", "Unknown", OptionMapping::getAsString);
                final int color = event.getOption("embed_color", 0x0000FF, OptionMapping::getAsInt);

                if ((channel == null) || (channel.getGuild().getIdLong() != event.getGuild().getIdLong())) {
                    reply(event, "❌ The channel that you have specified is invalid!", false, true);
                    return;
                }

                if (role == null || role.getGuild().getIdLong() != event.getGuild().getIdLong()) {
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
                embed.setFooter(member.getUser().getEffectiveName(), member.getEffectiveAvatarUrl());
                embed.addField(emoji + " `@" + role.getName() + "`", description, false);

                channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                    SelectOption option = SelectOption.of(role.getName(), role.getId()).withEmoji(Emoji.fromFormatted(emoji))
                            .withDescription(description);

                    msg.editMessageComponents(ActionRow.of(StringSelectMenu.create("role-selection-" + msg.getId())
                            .addOptions(option).setPlaceholder("Select a role").build())).queue();
                    event.getHook().editOriginal("✅ I have created this role selection menu at:\n" + msg.getJumpUrl())
                            .mentionRepliedUser(false).queue();
                });
            }

            case "add" -> {
                String messageURL = event.getOption("message-url", "Unknown", OptionMapping::getAsString);
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

                if (message.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
                    reply(event, "❌ The message URL that you have provided is invalid!", false, true);
                    return;
                }

                final Role role = event.getOption("role", null, OptionMapping::getAsRole);
                final String emoji = event.getOption("emoji", "❓", OptionMapping::getAsString);
                final String description = event.getOption("description", "Unknown", OptionMapping::getAsString);

                StringSelectMenu menu = getStringSelectMenu(message);

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

                StringSelectMenu.Builder menuBuilder = menu.createCopy();
                menuBuilder.getOptions()
                        .add(SelectOption.of(role.getName(), role.getId()).withEmoji(Emoji.fromFormatted(emoji))
                                .withDescription(description));
                menuBuilder.setMaxValues(menu.getMaxValues() + 1);

                EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().getFirst());
                embedBuilder.addField(emoji + " `@" + role.getName() + "`", description, false);

                message.editMessageComponents(ActionRow.of(menuBuilder.build())).setEmbeds(embedBuilder.build()).queue();
                reply(event, "✅ I have added the role to the role selection menu!");
            }

            case "remove" -> {
                String messageURL = event.getOption("message-url", "Unknown", OptionMapping::getAsString);
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

                final Role role = event.getOption("role", null, OptionMapping::getAsRole);

                if (role == null) {
                    reply(event, "❌ The role that you have provided is invalid!", false, true);
                    return;
                }

                StringSelectMenu menu = getStringSelectMenu(message);

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

                StringSelectMenu.Builder menuBuilder = menu.createCopy();
                menuBuilder.getOptions().removeIf(option -> option.getLabel().equals(role.getName()));
                menuBuilder.setMaxValues(menu.getMaxValues() - 1);

                EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().getFirst());
                embedBuilder.getFields().removeIf(field -> Objects.equals(field.getName(), role.getName()));

                message.editMessageComponents(ActionRow.of(menuBuilder.build())).setEmbeds(embedBuilder.build()).queue();
                reply(event, "✅ I have removed the role from the role selection menu!");
            }

            case "delete" -> {
                String messageURL = event.getOption("message-url", "Unknown", OptionMapping::getAsString);
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

                SelectMenu menu = getSelectMenu(message);

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

    @Nullable
    private static SelectMenu getSelectMenu(Message message) {
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
        return menu;
    }

    @Nullable
    private static StringSelectMenu getStringSelectMenu(Message message) {
        StringSelectMenu menu = null;
        for (final LayoutComponent component : message.getComponents()) {
            if (component instanceof final ActionRow row) {
                for (final ItemComponent column : row.getComponents()) {
                    if (column instanceof final StringSelectMenu selection) {
                        menu = selection;
                        break;
                    }
                }
            }
        }
        return menu;
    }
}