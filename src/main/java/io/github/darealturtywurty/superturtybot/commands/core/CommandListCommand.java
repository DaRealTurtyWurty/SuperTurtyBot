package io.github.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import io.github.darealturtywurty.superturtybot.commands.image.ImageCommand;
import io.github.darealturtywurty.superturtybot.commands.image.ImageCommand.ImageCategory;
import io.github.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand.NSFWCategory;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CommandHook;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
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
            .of(new OptionData(OptionType.STRING, "category", "The category to get the list of commands from.", false));
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
    public String getName() {
        return "commands";
    }
    
    @Override
    public String getRichName() {
        return "Command List";
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
        embed.setColor(Color.BLUE); // TODO: Random color
        embed.setTimestamp(Instant.now());
        if (guild == null || member == null) {
            CommandCategory.getCategories().stream()
                .filter(category -> category.isNSFW() && allowNSFW || !category.isNSFW())
                .sorted(Comparator.comparing(CommandCategory::getName))
                .forEach(category -> embed.addField(category.getEmoji() + " " + category.getName(),
                    (category.isNSFW() ? "⚠️Warning: NSFW⚠️\n" : "")
                        + String.format("`/commands %s`", category.getName().toLowerCase()),
                    true));
        } else {
            // TODO: Go through commands that are enabled in that guild and to that member
            
            CommandCategory.getCategories().stream()
                .filter(category -> category.isNSFW() && allowNSFW || !category.isNSFW())
                .sorted(Comparator.comparing(CommandCategory::getName))
                .forEach(category -> embed.addField(category.getEmoji() + " " + category.getName(),
                    (category.isNSFW() ? "⚠️Warning: NSFW⚠️\n" : "")
                        + String.format("`/commands %s`", category.getName().toLowerCase()),
                    true));
        }
        
        return embed;
    }
    
    // TODO: Guild and member specific list
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
            final List<ImageCommand> cmds = CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getCategory() == CommandCategory.IMAGE).map(ImageCommand.class::cast)
                .collect(Collectors.toList());
            for (final var imageCategory : ImageCategory.values()) {
                cmdsString.append("**" + StringUtils.capitalize(imageCategory.name().toLowerCase()) + "**\n");
                final List<ImageCommand> toRemove = new ArrayList<>();
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
        embed.setColor(Color.BLUE); // TODO: Random color
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
