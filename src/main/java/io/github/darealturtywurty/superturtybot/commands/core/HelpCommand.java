package io.github.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CommandHook;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
                final TextChannel channel = (TextChannel) event.getChannel();
                return channel.isNSFW();
            }).limit(25).map(CoreCommand::getName).toList();
        event.replyChoiceStrings(commands).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final OptionMapping cmdOption = event.getOption("command");
        if (cmdOption == null) {
            final var embed = commandless(Environment.INSTANCE.defaultPrefix());
            setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
            return;
        }

        final String command = cmdOption.getAsString();
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        setAuthor(embed, event.isFromGuild(), event.getInteraction().getUser(), event.getMember());

        embed.setColor(Color.GREEN);
        CommandHook.INSTANCE.getCommands().stream().filter(cmd -> cmd.getName().equals(command)).findFirst()
            .ifPresentOrElse(cmd -> constructEmbed(embed, cmd), () -> {
                embed.setDescription(String.format("No command found by name '%s'!", command));
                embed.setColor(Color.RED);
            });

        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private EmbedBuilder commandless(String prefix) {
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
            cmd.types.slash() ? "/" : Environment.INSTANCE.defaultPrefix(), cmd.getName()));
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
            embed.setFooter(member.getEffectiveName() + "#" + author.getDiscriminator(),
                member.getEffectiveAvatarUrl());
        } else {
            embed.setFooter(author.getName() + "#" + author.getDiscriminator(), author.getEffectiveAvatarUrl());
        }
    }
}
