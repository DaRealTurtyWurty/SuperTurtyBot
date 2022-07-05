package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Highlighter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class HighlightCommand extends CoreCommand {
    public HighlightCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("create", "Creates a new highlighter")
                .addOption(OptionType.STRING, "text", "The string of text that should notify you", true)
                .addOption(OptionType.BOOLEAN, "case_sensitive",
                    "Whether or not this highlighter is case sensitive (default: false)", false),
            new SubcommandData("list", "Lists your current highlighters"),
            new SubcommandData("delete", "Deletes an existing highlighter").addOption(OptionType.STRING, "id",
                "The ID of the highlighter that you want to delete", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Notifies you every time a message containing your specified text is sent.";
    }

    @Override
    public String getName() {
        return "highlight";
    }

    @Override
    public String getRichName() {
        return "Highlight";
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || !event.isWebhookMessage() || !event.getAuthor().isBot()
            || !event.getAuthor().isSystem())
            return;

        final Bson filter = Filters.eq("guild", event.getGuild().getIdLong());

        final String content = event.getMessage().getContentRaw();
        for (final Highlighter highlighter : Database.getDatabase().highlighters.find(filter)) {
            event.getGuild().retrieveMemberById(highlighter.getUser()).queue(
                member -> performHighlight(event, content, highlighter, member),
                error -> highlightFailed(event, content, highlighter, error));
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ This command can only be used inside a server!", false, true);
            return;
        }

        switch (event.getSubcommandName()) {
            case "create": {
                final String text = event.getOption("text").getAsString();
                final boolean caseSensitive = event.getOption("case_sensitive", false, OptionMapping::getAsBoolean);

                createHighlighter(event, text, caseSensitive);
                return;
            }

            case "list": {
                final Set<Highlighter> highlighters = new HashSet<>();
                final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                    Filters.eq("user", event.getUser().getIdLong()));
                Database.getDatabase().highlighters.find(filter).forEach(highlighters::add);

                if (highlighters.isEmpty()) {
                    reply(event, "❌ You have no highlighters!", false, true);
                    return;
                }

                listHighlighters(event, highlighters);
                return;
            }

            case "delete": {
                final String id = event.getOption("id").getAsString();

                final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                    Filters.eq("user", event.getUser().getIdLong()), Filters.eq("uuid", id));
                final Highlighter highlighter = Database.getDatabase().highlighters.find(filter).first();

                if (highlighter == null) {
                    reply(event, "❌ You do not have a highlighter with this ID!", false, true);
                    return;
                }

                deleteHighlighter(event, filter, highlighter);
                return;
            }

            default: {
                reply(event, "❌ You must provide a valid subcommand (`create`, `list`, `delete`)!", false, true);
                break;
            }
        }
    }

    private static void createHighlighter(SlashCommandInteractionEvent event, final String text,
        final boolean caseSensitive) {
        final Highlighter highlighter = new Highlighter(event.getGuild().getIdLong(), event.getUser().getIdLong(), text,
            caseSensitive);
        Database.getDatabase().highlighters.insertOne(highlighter);

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GREEN);
        embed.setDescription("✅ Highlighter for text `" + truncateString(text, 15) + "` has been added!");
        embed.setFooter("ID: " + highlighter.asUUID(), event.getUser().getEffectiveAvatarUrl());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private static void deleteHighlighter(SlashCommandInteractionEvent event, Bson filter, Highlighter highlighter) {
        Database.getDatabase().highlighters.deleteOne(filter);

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.RED);
        embed.setDescription(
            "❌ Highlighter for text `" + truncateString(highlighter.getText(), 15) + "` has been removed!");
        embed.setFooter("ID: " + highlighter.asUUID(), event.getUser().getEffectiveAvatarUrl());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private static void highlightFailed(MessageReceivedEvent event, final String content, final Highlighter highlighter,
        Throwable error) {
        Constants.LOGGER.error(
            "There has been a major error with the highlight command!\n{}\n{}\n{}\n{}\n{}\n{}\n\n{}\n{}", content,
            event.getChannel().getIdLong(), event.getGuild().getIdLong(), event.getMessageId(), error.getMessage(),
            ExceptionUtils.getMessage(error), error.getMessage(), ExceptionUtils.getMessage(error));
    }

    private static void listHighlighters(SlashCommandInteractionEvent event, Set<Highlighter> highlighters) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        embed.setDescription("**" + event.getUser().getName() + "'s Highlighters:**");
        final List<Highlighter> guildHighlighters = highlighters.stream()
            .filter(highlighter -> highlighter.getGuild() == event.getGuild().getIdLong())
            .sorted(Comparator.comparing(Highlighter::getTimeAdded)).toList();

        boolean none = true;
        if (!guildHighlighters.isEmpty()) {
            final var builder = new StringBuilder();
            guildHighlighters.stream()
                .forEachOrdered(highlighter -> builder.append("`" + truncateString(highlighter.getText(), 15) + "`"
                    + " (ID: **" + highlighter.asUUID().toString() + "**)\n"));
            none = false;
        }

        if (none) {
            embed.addField("N/A", "N/A", false);
        }

        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private static void performHighlight(MessageReceivedEvent event, final String content,
        final Highlighter highlighter, Member member) {
        if (!member.hasAccess(event.getGuildChannel()))
            return;

        if (highlighter.isCaseSensitive() ? content.contains(highlighter.getText())
            : content.toLowerCase().contains(highlighter.getText().toLowerCase())) {
            member.getUser().openPrivateChannel()
                .queue(
                    channel -> channel
                        .sendMessage("A message has been sent in <#" + event.getChannel().getIdLong()
                            + "> containing content from your highlighter (`"
                            + truncateString(highlighter.getText(), 15) + "`).\n\n" + event.getMessage().getJumpUrl())
                        .queue());
        }
    }

    // TODO: Utility class
    private static String truncateString(String str, int length) {
        if (str.length() > length)
            return str.substring(0, length - 3) + "...";
        return str;
    }
}
