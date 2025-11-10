package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CommandListCommand extends CoreCommand {
    public CommandListCommand() {
        super(new Types(true, false, false, false));
    }

    // TODO: Display commands based on guild and member permissions
    private static EmbedBuilder categoriesEmbed(@Nullable Guild guild, @Nullable Member member, boolean allowNSFW) {
        final var embed = new EmbedBuilder();
        embed.setTitle("Categories:");
        embed.setColor(Color.BLUE);
        embed.setTimestamp(Instant.now());
        CommandCategory.getCategories().stream()
                .filter(category -> category.isNSFW() && allowNSFW || !category.isNSFW())
                .sorted(Comparator.comparing(CommandCategory::getName)).forEach(
                        category -> embed.addField(category.getEmoji() + " " + category.getName(),
                                (category.isNSFW() ? "⚠️Warning: NSFW⚠️\n" : "") + String.format("`/commands %s`",
                                        category.getName().toLowerCase()), true));

        return embed;
    }

    private static CompletableFuture<EmbedBuilder> commandsEmbed(String categoryStr, boolean allowNSFW, @Nullable Guild guild) {
        final var category = CommandCategory.byName(categoryStr.toUpperCase(Locale.ROOT));
        if (category == null)
            return CompletableFuture.completedFuture(null);

        final var embed = new EmbedBuilder();
        embed.setTitle("Commands for category: " + category.getName());
        CompletableFuture<StringBuilder> cmdsString = new CompletableFuture<>();
        if (category.isNSFW()) {
            if (!allowNSFW)
                return CompletableFuture.completedFuture(null);

            CompletableFuture<List<CoreCommand>> cmds = new CompletableFuture<>();
            if (guild != null) {
                guild.retrieveCommandPrivileges().queue(privilegeConfig ->
                        cmds.complete(CommandHook.INSTANCE.getCommands()
                                .stream()
                                .filter(cmd -> cmd.getCategory() == CommandCategory.NSFW)
                                .filter(cmd -> {
                                    List<IntegrationPrivilege> privileges = privilegeConfig.getCommandPrivileges(cmd.getCommandId());
                                    return privileges == null || privileges.isEmpty() || privileges.stream()
                                            .noneMatch(privilege -> privilege.targetsEveryone() && privilege.isDisabled());
                                })
                                .sorted(Comparator.comparing(CoreCommand::getName))
                                .toList()));
            } else {
                cmds.complete(CommandHook.INSTANCE.getCommands()
                        .stream()
                        .filter(cmd -> cmd.getCategory() == CommandCategory.NSFW)
                        .sorted(Comparator.comparing(CoreCommand::getName))
                        .toList());
            }

            cmds.thenAccept(list -> {
                var builder = new StringBuilder();
                list.forEach(cmd -> builder.append("`").append(cmd.getName()).append("`\n"));
                builder.delete(builder.length() - 1, builder.length());
                cmdsString.complete(builder);
            });
        } else {
            if (guild != null) {
                guild.retrieveCommandPrivileges().queue(privilegeConfig -> {
                    var builder = new StringBuilder();
                    for (CoreCommand cmd : CommandHook.INSTANCE.getCommands()
                            .stream()
                            .filter(cmd -> cmd.getCategory() == CommandCategory.byName(categoryStr))
                            .filter(cmd -> {
                                String commandId;
                                if (cmd.isServerOnly()) {
                                    commandId = cmd.getCommandId(guild.getIdLong());
                                } else {
                                    commandId = cmd.getCommandId();
                                }

                                if (commandId == null)
                                    return false;

                                List<IntegrationPrivilege> privileges = privilegeConfig.getCommandPrivileges(commandId);
                                return privileges == null || privileges.isEmpty() || privileges.stream()
                                        .noneMatch(privilege -> privilege.targetsEveryone() && privilege.isDisabled());
                            })
                            .sorted(Comparator.comparing(CoreCommand::getName))
                            .toList()) {
                        builder.append("`").append(cmd.getName()).append("`\n");
                    }

                    cmdsString.complete(builder);
                });
            } else {
                var builder = new StringBuilder();
                for (CoreCommand cmd : CommandHook.INSTANCE.getCommands()
                        .stream()
                        .filter(cmd -> cmd.getCategory() == CommandCategory.byName(categoryStr))
                        .sorted(Comparator.comparing(CoreCommand::getName))
                        .toList()) {
                    builder.append("`").append(cmd.getName()).append("`\n");
                }

                cmdsString.complete(builder);
            }
        }

        CompletableFuture<EmbedBuilder> embedFuture = new CompletableFuture<>();
        cmdsString.thenAccept(cmds -> {
            if (cmds.isEmpty()) {
                embed.setDescription("There are no commands in this category!");
                embedFuture.complete(embed);
                return;
            }

            embed.setDescription(cmds.toString());
            embed.setTimestamp(Instant.now());
            embed.setColor(Color.BLUE);
            embedFuture.complete(embed);
        });

        return embedFuture;
    }

    private static void setAuthor(EmbedBuilder embed, boolean fromGuild, User author, Member member) {
        if (fromGuild) {
            embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getEffectiveName(), author.getEffectiveAvatarUrl());
        }
    }

    private static void createButtons(@Nullable CommandCategory category, User user,
                                      @Nullable MessageEditCallbackAction messageEditAction, Message message) {
        var trashButton = Button.danger("commandlist-trash", Emoji.fromUnicode("🗑️"));
        if (category != null) {
            if (messageEditAction == null) {
                message.editMessageComponents(
                                ActionRow.of(Button.primary("commandlist-back", Emoji.fromUnicode("⬅️")), trashButton))
                        .queue(ignored -> createEventWaiter(user, message).build());
            } else {
                messageEditAction.setComponents(
                                ActionRow.of(Button.primary("commandlist-back", Emoji.fromUnicode("⬅️")), trashButton))
                        .queue(ignored -> createEventWaiter(user, message).build());
            }
            return;
        }

        // only allowed to have 5 buttons per row
        List<List<Button>> buttonRows = new ArrayList<>();
        for (CommandCategory commandCategory : CommandCategory.getCategories()) {
            if (commandCategory.isNSFW() && !NSFWCommand.isValidChannel(message.getChannel()))
                continue;

            if (buttonRows.isEmpty()) {
                buttonRows.add(new ArrayList<>());
            }

            List<Button> currentRow = buttonRows.getLast();
            if (currentRow.size() >= 5) {
                currentRow = new ArrayList<>();
                buttonRows.add(currentRow);
            }

            currentRow.add(Button.primary("commandlist-" + commandCategory.getName().toLowerCase(Locale.ROOT),
                            commandCategory.getName())
                    .withEmoji(Emoji.fromUnicode(commandCategory.getEmoji())));
        }

        List<ActionRow> actionRows = new ArrayList<>();
        for (List<Button> buttonRow : buttonRows) {
            actionRows.add(ActionRow.of(buttonRow));
        }

        // add trash button
        actionRows.add(ActionRow.of(trashButton));

        if (messageEditAction == null) {
            message.editMessageComponents(actionRows)
                    .queue(ignored -> createEventWaiter(user, message).build());
        } else {
            messageEditAction.setComponents(actionRows)
                    .queue(ignored -> createEventWaiter(user, message).build());
        }
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createEventWaiter(User user, Message message) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(event -> event.isFromGuild() == message.isFromGuild()
                        && event.getChannelIdLong() == message.getChannelIdLong()
                        && event.getMessageIdLong() == message.getIdLong()
                        && event.getButton().getCustomId() != null
                        && event.getButton().getCustomId().startsWith("commandlist-"))
                .timeout(1, TimeUnit.MINUTES)
                .timeoutAction(() -> message.delete().queue())
                .failure(() -> message.delete().queue())
                .success(event -> {
                    if (event.getUser().getIdLong() != user.getIdLong()) {
                        event.deferEdit().queue();
                        return;
                    }

                    String buttonId = event.getButton().getCustomId();
                    switch (buttonId) {
                        case "commandlist-trash" -> event.deferEdit().queue(hook -> hook.deleteOriginal().queue());
                        case "commandlist-back" -> {
                            EmbedBuilder embed = categoriesEmbed(
                                    event.getGuild(),
                                    event.getMember(),
                                    NSFWCommand.isValidChannel(event.getChannel()));
                            createButtons(
                                    null,
                                    event.getUser(),
                                    event.editMessageEmbeds(embed.build()),
                                    event.getMessage());
                        }
                        case null -> event.deferEdit().queue();
                        default -> {
                            String category = buttonId.split("-")[1];

                            CompletableFuture<EmbedBuilder> embedFuture = commandsEmbed(
                                    category,
                                    NSFWCommand.isValidChannel(event.getChannel()),
                                    event.getGuild());
                            embedFuture.thenAcceptAsync(embedBuilder -> {
                                if (embedBuilder == null) {
                                    event.deferEdit().queue();
                                    return;
                                }

                                createButtons(
                                        CommandCategory.byName(category.toUpperCase(Locale.ROOT)),
                                        event.getUser(),
                                        event.editMessageEmbeds(embedBuilder.build()),
                                        event.getMessage());
                            });
                        }
                    }
                });
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(
                OptionType.STRING,
                "category",
                "The category to get the list of commands from.",
                false,
                true
        ));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Retrieves the list of commands.";
    }

    @Override
    public String getHowToUse() {
        return "/commands\n/commands [category]";
    }

    @Override
    public String getName() {
        return "commands";
    }

    @Override
    public String getRichName() {
        return "Command List";
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName())) return;

        final String term = event.getFocusedOption().getValue();
        final List<String> categories = CommandCategory.getCategories().stream()
                .filter(category -> category.getName().toLowerCase().contains(term.trim().toLowerCase(Locale.ROOT)))
                .filter(category -> {
                    if (!event.isFromGuild() || !category.isNSFW())
                        return true;

                    return NSFWCommand.isValidChannel(event.getChannel());
                })
                .limit(25)
                .map(CommandCategory::getName)
                .map(String::toLowerCase)
                .toList();
        event.replyChoiceStrings(categories).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final String category = event.getOption("category", OptionMapping::getAsString);
        if (category == null) {
            final EmbedBuilder embed = categoriesEmbed(
                    event.isFromGuild() ? event.getGuild() : null,
                    event.isFromGuild() ? event.getMember() : null,
                    NSFWCommand.isValidChannel(event.getChannel()));
            setAuthor(embed, event.isFromGuild(), event.getUser(), event.getMember());
            event.getHook().editOriginalEmbeds(embed.build())
                    .queue(message -> createButtons(null, event.getUser(), null, message));
            return;
        }

        CommandCategory commandCategory = CommandCategory.byName(category.toUpperCase(Locale.ROOT));
        if (commandCategory == null) {
            reply(event, "You must provide a valid category!", false, true);
            return;
        }

        commandsEmbed(category, NSFWCommand.isValidChannel(event.getChannel()), event.getGuild()).thenAccept(embed -> {
            if (embed == null) {
                event.getHook().sendMessage("❌ You must specify a valid category!").queue();
                return;
            }

            setAuthor(embed, event.isFromGuild(), event.getUser(), event.getMember());
            event.getHook()
                    .editOriginalEmbeds(embed.build())
                    .queue(message -> createButtons(commandCategory, event.getUser(), null, message));
        });
    }
}
