package dev.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import dev.darealturtywurty.superturtybot.commands.image.AbstractImageCommand;
import dev.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand.NSFWCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CommandListCommand extends CoreCommand {
    public CommandListCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.STRING, "category", "The category to get the list of commands from.", false)
                .setAutoComplete(true));
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
        if (!event.getName().equals(getName()))
            return;

        final String term = event.getFocusedOption().getValue();
        final List<String> categories = CommandCategory.getCategories().stream()
            .filter(category -> category.getName().toLowerCase().contains(term.trim().toLowerCase()))
            .filter(category -> {
                if (!event.isFromGuild() || !category.isNSFW())
                    return true;
                
                final TextChannel channel = event.getChannel().asTextChannel();
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
        
        final var embed = commandsEmbed(category,
            event.getChannel() instanceof final TextChannel tChannel && tChannel.isNSFW() || !event.isFromGuild());
        if (embed == null) {
            event.deferReply(true).setContent("You must provide a valid category!").mentionRepliedUser(false).queue();
            return;
        }
        
        setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }
    
    private static EmbedBuilder categoriesEmbed(@Nullable Guild guild, @Nullable Member member, boolean allowNSFW) {
        final var embed = new EmbedBuilder();
        embed.setTitle("Categories:");
        embed.setColor(Color.BLUE);
        embed.setTimestamp(Instant.now());
        CommandCategory.getCategories().stream()
            .filter(category -> category.isNSFW() && allowNSFW || !category.isNSFW())
            .sorted(Comparator.comparing(CommandCategory::getName))
            .forEach(category -> embed.addField(category.getEmoji() + " " + category.getName(),
                (category.isNSFW() ? "⚠️Warning: NSFW⚠️\n" : "")
                    + String.format("`/commands %s`", category.getName().toLowerCase()),
                true));
        
        return embed;
    }
    
    private static EmbedBuilder commandsEmbed(String categoryStr, boolean allowNSFW) {
        final var category = CommandCategory.byName(categoryStr);
        final var embed = new EmbedBuilder();
        embed.setTitle("Commands for category: " + category.getName());
        final var cmdsString = new StringBuilder();
        if (category.isNSFW()) {
            if (!allowNSFW)
                return null;
            
            final List<NSFWCommand> cmds = CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getCategory() == CommandCategory.NSFW).map(NSFWCommand.class::cast)
                .collect(Collectors.toList());
            for (final var nsfwCategory : NSFWCategory.values()) {
                cmdsString.append("**" + StringUtils.capitalize(nsfwCategory.name().toLowerCase()) + "**\n");
                final List<NSFWCommand> toRemove = new ArrayList<>();
                cmds.stream().filter(cmd -> cmd.category == nsfwCategory)
                    .sorted(Comparator.comparing(CoreCommand::getName)).forEachOrdered(cmd -> {
                        cmdsString.append("`" + cmd.getName() + "`, ");
                        toRemove.add(cmd);
                    });
                
                cmdsString.delete(cmdsString.length() - 2, cmdsString.length());
                cmdsString.append("\n");
                
                toRemove.forEach(cmds::remove);
                toRemove.clear();
            }
        } else if (category == CommandCategory.IMAGE) {
            final List<AbstractImageCommand> cmds = CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getCategory() == CommandCategory.IMAGE).map(AbstractImageCommand.class::cast)
                .collect(Collectors.toList());
            for (final var imageCategory : ImageCategory.values()) {
                cmdsString.append("**" + StringUtils.capitalize(imageCategory.name().toLowerCase()) + "**\n");
                final List<AbstractImageCommand> toRemove = new ArrayList<>();
                cmds.stream().filter(cmd -> cmd.getImageCategory() == imageCategory)
                    .sorted(Comparator.comparing(CoreCommand::getName)).forEachOrdered(cmd -> {
                        cmdsString.append("`" + cmd.getName() + "`, ");
                        toRemove.add(cmd);
                    });
                
                if (!toRemove.isEmpty()) {
                    cmdsString.delete(cmdsString.length() - 2, cmdsString.length());
                } else {
                    cmdsString.append("Not Yet Implemented");
                }
                
                cmdsString.append("\n");
                
                toRemove.forEach(cmds::remove);
                toRemove.clear();
            }
        } else {
            CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getCategory() == CommandCategory.byName(categoryStr))
                .sorted(Comparator.comparing(CoreCommand::getName))
                .forEachOrdered(cmd -> cmdsString.append("`" + cmd.getName() + "`\n"));
        }
        
        embed.setDescription(cmdsString.toString());
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        return embed;
    }
    
    private static void setAuthor(EmbedBuilder embed, boolean fromGuild, User author, Member member) {
        if (fromGuild) {
            embed.setFooter(member.getEffectiveName() + "#" + author.getDiscriminator(),
                member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getName() + "#" + author.getDiscriminator(), author.getEffectiveAvatarUrl());
        }
    }
}
