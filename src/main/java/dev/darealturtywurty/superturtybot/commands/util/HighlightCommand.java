package dev.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Highlighter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

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
                "The ID of the highlighter that you want to delete", true, true));
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
    public String getHowToUse() {
        return "/highlight create [text]\n/highlight create [text] [caseSensitive]\nhighlight list\n/highlight delete [id]";
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
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(getName()))
            return;
        
        final String subcommand = event.getSubcommandName();
        if (!"delete".equals(subcommand))
            return;
        
        final String term = event.getFocusedOption().getValue();
        
        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getUser().getIdLong()));
        final List<Highlighter> highlighters = new ArrayList<>();
        Database.getDatabase().highlighters.find(filter).forEach(highlighters::add);
        final List<String> options = highlighters.stream().filter(highlighter -> highlighter.getUuid().contains(term))
            .limit(25).map(Highlighter::getUuid).toList();
        event.replyChoiceStrings(options).queue();
    }
    
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot()
            || event.getAuthor().isSystem())
            return;
        
        final Bson filter = Filters.eq("guild", event.getGuild().getIdLong());
        
        final String content = event.getMessage().getContentRaw();
        for (final Highlighter highlighter : Database.getDatabase().highlighters.find(filter)) {
            if (highlighter == null) continue;

            event.getGuild().retrieveMemberById(highlighter.getUser()).queue(
                member -> performHighlight(event, content, highlighter, member),
                error -> highlightFailed(event, content, error));
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ This command can only be used inside a server!", false, true);
            return;
        }

        if(event.getSubcommandName() == null || event.getSubcommandName().isBlank()) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }
        
        switch (event.getSubcommandName()) {
            case "create": {
                final String text = event.getOption("text").getAsString();
                if (text.length() < 4) {
                    reply(event, "❌ A highlighter must be at least 4 characters!", false, true);
                    return;
                }

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
    
    private static void createHighlighter(SlashCommandInteractionEvent event, final String text, final boolean caseSensitive) {
        final Highlighter highlighter = new Highlighter(event.getGuild().getIdLong(), event.getUser().getIdLong(), text,
            caseSensitive);
        Database.getDatabase().highlighters.insertOne(highlighter);
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GREEN);
        embed.setDescription("✅ Highlighter for text `" + StringUtils.truncateString(text, 15) + "` has been added!");
        embed.setFooter("ID: " + highlighter.asUUID(), event.getUser().getEffectiveAvatarUrl());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }
    
    private static void deleteHighlighter(SlashCommandInteractionEvent event, Bson filter, Highlighter highlighter) {
        Database.getDatabase().highlighters.deleteOne(filter);
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.RED);
        embed.setDescription(
            "❌ Highlighter for text `" + StringUtils.truncateString(highlighter.getText(), 15) + "` has been removed!");
        embed.setFooter("ID: " + highlighter.asUUID(), event.getUser().getEffectiveAvatarUrl());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }
    
    private static void highlightFailed(MessageReceivedEvent event, final String content, Throwable error) {
        Constants.LOGGER.debug(
            "There has been a major error with the highlight command!\n{}\n{}\n{}\n{}\n{}\n{}\n\n{}\n{}", content,
            event.getChannel().getIdLong(), event.getGuild().getIdLong(), event.getMessageId(), error.getMessage(),
            ExceptionUtils.getMessage(error), error.getMessage(), ExceptionUtils.getMessage(error));
    }
    
    private static void listHighlighters(SlashCommandInteractionEvent event, Set<Highlighter> highlighters) {
        event.deferReply().queue();

        if(highlighters.isEmpty()) {
            event.getHook().sendMessage("❌ No highlighters found!").queue();
            return;
        }

        List<Highlighter> sorted = highlighters.stream().sorted(Comparator.comparing(Highlighter::getTimeAdded)).toList();

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Highlighter highlighter : sorted) {
            contents.field("ID: " + highlighter.getUuid(), highlighter.getText());
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                .title(event.getUser().getName() + "'s Highlighters")
                .color(event.getMember() == null ? Color.BLUE : new Color(event.getMember().getColorRaw()))
                .timestamp(Instant.now())
                .description("Here are your highlighters!")
                .thumbnail(event.getMember() == null ? event.getUser().getEffectiveAvatarUrl() : event.getMember().getEffectiveAvatarUrl())
                .build(event.getJDA());

        embed.send(event.getHook(), () -> event.getHook().sendMessage("No highlighters found!").queue());
    }
    
    private static void performHighlight(MessageReceivedEvent event, final String content,
        final Highlighter highlighter, Member member) {
        try {
            if (!member.hasAccess(event.getGuildChannel()))
                return;
            
            if (highlighter.isCaseSensitive() ? content.contains(highlighter.getText())
                : content.toLowerCase().contains(highlighter.getText().toLowerCase())) {
                member.getUser().openPrivateChannel()
                    .queue(channel -> channel.sendMessage("A message has been sent in <#"
                        + event.getChannel().getIdLong() + "> containing content from your highlighter (`"
                        + StringUtils.truncateString(highlighter.getText(), 15) + "`).\n\n"
                        + event.getMessage().getJumpUrl()).queue());
            }
        } catch (final IllegalStateException | IllegalArgumentException ignored) {
            
        }
    }
}
