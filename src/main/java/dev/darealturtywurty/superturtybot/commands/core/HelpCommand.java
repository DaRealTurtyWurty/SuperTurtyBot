package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class HelpCommand extends CoreCommand {
    public HelpCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(
                OptionType.STRING,
                "command",
                "Gets information about a specific command.",
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
        return "Gets information about the bot or a specific bot command";
    }

    @Override
    public String getHowToUse() {
        return "/help\n/help [command]";
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getRichName() {
        return "Help";
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(getName()))
            return;

        final String term = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);

        final List<String> commands = CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getName().contains(term))
                .filter(cmd -> {
                    if (!event.isFromGuild() || cmd.getCategory() != CommandCategory.NSFW)
                        return true;

                    return NSFWCommand.isValidChannel(event.getChannel());
                })
                .limit(25)
                .map(CoreCommand::getName)
                .toList();
        event.replyChoiceStrings(commands).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String command = event.getOption("command", null, OptionMapping::getAsString);
        if (command == null) {
            final EmbedBuilder embed = commandlessEmbed();
            setAuthor(embed, event.isFromGuild(), event.getUser(), event.getMember());
            reply(event, embed);
            return;
        }

        final var embed = new EmbedBuilder()
                .setTimestamp(Instant.now())
                .setColor(Color.GREEN);
        setAuthor(embed, event.isFromGuild(), event.getUser(), event.getMember());

        CommandHook.INSTANCE.getCommands().stream()
                .filter(cmd -> cmd.getName().equals(command))
                .findFirst()
                .ifPresentOrElse(
                        cmd -> populateEmbed(embed, cmd),
                        () -> {
                            embed.setDescription("No command found by name '%s'!".formatted(command));
                            embed.setColor(Color.RED);
                        });

        reply(event, embed);
    }

    private EmbedBuilder commandlessEmbed() {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GREEN);
        embed.setDescription("""
                Welcome to TurtyBot.

                I contain a wide diversity of features ranging from music to moderation to minigames.

                You can view my list of commands using `/commands`!""");
        return embed;
    }

    private void populateEmbed(EmbedBuilder embed, CoreCommand cmd) {
        embed.setDescription(String.format("Information about command: **`%s%s`**",
                cmd.types.slash() ? "/" : Environment.INSTANCE.defaultPrefix().orElse(""), cmd.getName()));
        embed.addField("Name: ", cmd.getRichName() + " (" + cmd.getName() + ")", false);
        embed.addField("Description: ", cmd.getDescription(), false);
        embed.addField("Category: ", cmd.getCategory().getName(), false);
        embed.addField("How To Use: ", cmd.getHowToUse(), false);
        embed.addField("Command Access: ", cmd.getAccess(), false);
        embed.addField("Is Server Only: ", StringUtils.trueFalseToYesNo(cmd.isServerOnly()), false);
        embed.addField("Is NSFW: ", StringUtils.trueFalseToYesNo(cmd.getCategory() == CommandCategory.NSFW), false);
    }

    private static void setAuthor(@NotNull EmbedBuilder embed, boolean fromGuild, @Nullable User author, @Nullable Member member) {
        if (fromGuild) {
            embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getName(), author.getEffectiveAvatarUrl());
        }
    }
}
