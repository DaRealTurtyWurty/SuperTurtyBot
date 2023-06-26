package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class CommandListCommand extends CoreCommand {
    public CommandListCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "category", "The category to get the list of commands from.",
                false).setAutoComplete(true));
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
                .filter(category -> category.getName().toLowerCase().contains(term.trim().toLowerCase()))
                .filter(category -> {
                    if (!event.isFromGuild() || !category.isNSFW()) return true;

                    final IAgeRestrictedChannel channel = event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD ||
                            event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD
                            ? event.getChannel().asThreadChannel().getParentChannel().asTextChannel()
                            : event.getChannel().asTextChannel();

                    return channel.isNSFW();
                }).limit(25).map(CommandCategory::getName).map(String::toLowerCase).toList();
        event.replyChoiceStrings(categories).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final OptionMapping categoryOption = event.getOption("category");
        if (categoryOption == null) {
            final var embed = categoriesEmbed(event.isFromGuild() ? event.getGuild() : null,
                    event.isFromGuild() ? event.getMember() : null,
                    event.getChannel() instanceof final TextChannel tChannel && tChannel.isNSFW() || !event.isFromGuild());
            setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
            return;
        }

        final String category = categoryOption.getAsString();
        if (CommandCategory.byName(category) == null) {
            event.deferReply(true).setContent("You must provide a valid category!").mentionRepliedUser(false).queue();
            return;
        }

        event.deferReply().queue();

        commandsEmbed(category,
                event.getChannel() instanceof final TextChannel tChannel && tChannel.isNSFW() || !event.isFromGuild(), event.getGuild()).thenAccept(embed -> {
            if (embed == null) {
                event.getHook().sendMessage("❌ You must specify a valid category!").queue();
                return;
            }

            setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }

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
        final var category = CommandCategory.byName(categoryStr);
        if (category == null) return null;

        final var embed = new EmbedBuilder();
        embed.setTitle("Commands for category: " + category.getName());
        CompletableFuture<StringBuilder> cmdsString = new CompletableFuture<>();
        if (category.isNSFW()) {
            if (!allowNSFW) return null;

            CompletableFuture<List<CoreCommand>> cmds = new CompletableFuture<>();
            if (guild != null) {
                guild.retrieveCommandPrivileges().queue(privilegeConfig -> {
                    cmds.complete(CommandHook.INSTANCE.getCommands()
                            .stream()
                            .filter(cmd -> cmd.getCategory() == CommandCategory.NSFW)
                            .filter(cmd -> {
                                List<IntegrationPrivilege> privileges = privilegeConfig.getCommandPrivileges(cmd.getCommandId());
                                return privileges == null || privileges.isEmpty() || privileges.stream()
                                        .noneMatch(privilege -> privilege.targetsEveryone() && privilege.isDisabled());
                            })
                            .sorted(Comparator.comparing(CoreCommand::getName))
                            .toList());
                });
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
                                List<IntegrationPrivilege> privileges = privilegeConfig.getCommandPrivileges(cmd.getCommandId());
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
            if (cmds.length() == 0) {
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
            embed.setFooter(member.getUser().getName(), member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getName(), author.getEffectiveAvatarUrl());
        }
    }
}
