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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class HelpCommand extends CoreCommand {
    public HelpCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "command", "Gets information about a specific command.", false)
            .setAutoComplete(true));
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
        
        final String term = event.getFocusedOption().getValue().toLowerCase();
        
        final List<String> commands = CommandHook.INSTANCE.getCommands().stream()
            .filter(cmd -> cmd.getName().contains(term)).filter(cmd -> {
                if (!event.isFromGuild() || !(cmd instanceof NSFWCommand))
                    return true;

                final TextChannel channel = event.getChannel().asTextChannel();
                return channel.isNSFW();
            }).limit(25).map(CoreCommand::getName).toList();
        event.replyChoiceStrings(commands).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final OptionMapping cmdOption = event.getOption("command");
        if (cmdOption == null) {
            final var embed = commandless();
            setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
            return;
        }

        final String command = cmdOption.getAsString();
        final EmbedBuilder[] embed = {new EmbedBuilder()};
        embed[0].setTimestamp(Instant.now());
        setAuthor(embed[0], event.isFromGuild(), event.getInteraction().getUser(), event.getMember());

        embed[0].setColor(Color.GREEN);
        CommandHook.INSTANCE.getCommands().stream().filter(cmd -> cmd.getName().equals(command)).findFirst()
            .ifPresentOrElse(cmd -> embed[0] = constructEmbed(embed[0], cmd), () -> {
                embed[0].setDescription(String.format("No command found by name '%s'!", command));
                embed[0].setColor(Color.RED);
            });

        event.deferReply().addEmbeds(embed[0].build()).mentionRepliedUser(false).queue();
    }

    private EmbedBuilder commandless() {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GREEN);
        embed.setDescription("""
            Welcome to TurtyBot.

            I contain a wide diversity of features ranging from music to moderation to minigames.

            You can view my list of commands using `/commands`!""");
        return embed;
    }

    private EmbedBuilder constructEmbed(EmbedBuilder embed, CoreCommand cmd) {
        embed.setDescription(String.format("Information about command: **`%s%s`**",
            cmd.types.slash() ? "/" : Environment.INSTANCE.defaultPrefix().orElse(""), cmd.getName()));
        embed.addField("Name: ", cmd.getRichName() + " (" + cmd.getName() + ")", false);
        embed.addField("Description: ", cmd.getDescription(), false);
        embed.addField("Category: ", cmd.getCategory().getName(), false);
        embed.addField("How To Use: ", cmd.getHowToUse(), false);
        embed.addField("Command Access: ", cmd.getAccess(), false);
        embed.addField("Is Server Only: ", StringUtils.trueFalseToYesNo(cmd.isServerOnly()), false);
        embed.addField("Is NSFW: ", StringUtils.trueFalseToYesNo(cmd instanceof NSFWCommand), false);
        
        return embed;
    }

    private static void setAuthor(EmbedBuilder embed, boolean fromGuild, User author, Member member) {
        if (fromGuild) {
            embed.setFooter(member.getUser().getName(), member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getName(), author.getEffectiveAvatarUrl());
        }
    }
}
