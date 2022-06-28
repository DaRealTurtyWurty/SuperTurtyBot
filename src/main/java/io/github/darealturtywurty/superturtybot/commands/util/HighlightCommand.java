package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

// TODO: Save to database
public class HighlightCommand extends CoreCommand {
    private static final Map<Long, Set<Highlighter>> HIGHLIGHTERS = new HashMap<>();
    
    public HighlightCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("create", "Creates a new highlighter")
                .addOption(OptionType.STRING, "text", "The string of text that should notify you", true)
                .addOption(OptionType.BOOLEAN, "case_sensitive",
                    "Whether or not this highlighter is case sensitive (default: false)", false)
                .addOption(OptionType.BOOLEAN, "is_global",
                    "Whether or not this highlighter is global for all your servers or whether it is server specific",
                    false),
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
        if (!event.isFromGuild() && !event.isWebhookMessage() && !event.getAuthor().isBot()
            && !event.getAuthor().isSystem())
            return;

        final String content = event.getMessage().getContentRaw();
        for (final Entry<Long, Set<Highlighter>> userHighlights : HIGHLIGHTERS.entrySet()) {
            // TODO: Fix this breaking when it happens in another guild since that member id
            // may not exist in that guild
            event.getGuild().retrieveMemberById(userHighlights.getKey()).queue(member -> {
                if (!member.hasAccess(event.getGuildChannel()))
                    return;

                final List<Highlighter> highlighters = userHighlights.getValue().stream()
                    .filter(highlighter -> highlighter instanceof final GuildHighlighter gHighlighter
                        && gHighlighter.guildId() == event.getGuild().getIdLong()
                        || highlighter instanceof GlobalHighlighter)
                    .filter(highlighter -> highlighter.caseSensitive() ? content.contains(highlighter.text())
                        : content.toLowerCase().contains(highlighter.text().toLowerCase()))
                    .toList();

                if (!highlighters.isEmpty()) {
                    member.getUser().openPrivateChannel()
                        .queue(channel -> highlighters.forEach(highlighter -> channel
                            .sendMessage("A message has been sent in <#" + event.getChannel().getIdLong()
                                + "> containing content from your highlighter (`"
                                + truncateString(highlighter.text(), 15) + "`).\n\n" + event.getMessage().getJumpUrl())
                            .queue()));
                }
            }, error -> {
                Constants.LOGGER.error("There has been a major error with the highlight command!\n" + content + "\n"
                    + event.getChannel().getIdLong() + "\n" + event.getGuild().getIdLong() + "\n" + event.getMessageId()
                    + "\n" + error.getMessage() + "\n" + ExceptionUtils.getMessage(error) + "\n\n" + error.getMessage()
                    + "\n" + ExceptionUtils.getMessage(error));
                HIGHLIGHTERS.remove(userHighlights.getKey());
            });
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        switch (event.getSubcommandName()) {
            case "create": {
                final String text = event.getOption("text").getAsString();
                final boolean caseSensitive = event.getOption("case_sensitive", false, OptionMapping::getAsBoolean);
                final boolean isGlobal = event.getOption("is_global", !event.isFromGuild(),
                    OptionMapping::getAsBoolean);
                if (!isGlobal && !event.isFromGuild()) {
                    event.deferReply(true)
                        .setContent("If you want a server only highlighter, you must run this command in that server!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                Highlighter highlighter;
                if (isGlobal) {
                    highlighter = createGlobalHighlighter(text, caseSensitive);
                } else {
                    highlighter = createGuildHighlighter(event.getGuild(), text, caseSensitive);
                }
                
                Set<Highlighter> highlighters;
                if (!HIGHLIGHTERS.containsKey(event.getUser().getIdLong())) {
                    highlighters = new HashSet<>();
                } else {
                    highlighters = HIGHLIGHTERS.get(event.getUser().getIdLong());
                }

                highlighters.add(highlighter);

                HIGHLIGHTERS.put(event.getUser().getIdLong(), highlighters);
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.GREEN);
                embed.setDescription("✅ Highlighter for text `" + truncateString(text, 15) + "` has been added!");
                embed.setFooter("ID: " + highlighter.uuid(), event.getUser().getEffectiveAvatarUrl());
                event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                return;
            }
            
            case "list": {
                Set<Highlighter> highlighters = null;
                if (HIGHLIGHTERS.containsKey(event.getUser().getIdLong())) {
                    highlighters = HIGHLIGHTERS.get(event.getUser().getIdLong());
                }
                
                if (highlighters == null) {
                    event.deferReply(true).setContent("You have no highlighters!").mentionRepliedUser(false).queue();
                    return;
                }
                
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.BLUE);
                embed.setDescription("**" + event.getUser().getName() + "'s Highlighters:**");
                final List<Highlighter> globalHighlighters = highlighters.stream()
                    .filter(GlobalHighlighter.class::isInstance).sorted(Comparator.comparing(Highlighter::timeAdded))
                    .toList();
                
                final List<Highlighter> guildHighlighters = highlighters.stream()
                    .filter(GuildHighlighter.class::isInstance)
                    .filter(highlighter -> ((GuildHighlighter) highlighter).guildId == event.getGuild().getIdLong())
                    .sorted(Comparator.comparing(Highlighter::timeAdded)).toList();

                boolean none = true;
                if (!globalHighlighters.isEmpty()) {
                    final var builder = new StringBuilder();
                    globalHighlighters.stream()
                        .forEachOrdered(highlighter -> builder.append("`" + truncateString(highlighter.text(), 15) + "`"
                            + " (ID: **" + highlighter.uuid().toString() + "**)\n"));
                    embed.addField("**Global**", builder.toString(), false);
                    none = false;
                }

                if (!guildHighlighters.isEmpty()) {
                    final var builder = new StringBuilder();
                    guildHighlighters.stream()
                        .forEachOrdered(highlighter -> builder.append("`" + truncateString(highlighter.text(), 15) + "`"
                            + " (ID: **" + highlighter.uuid().toString() + "**)\n"));
                    embed.addField("**Server**", builder.toString(), false);
                    none = false;
                }

                if (none) {
                    embed.addField("N/A", "N/A", false);
                }

                event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                return;
            }

            case "delete": {
                final String id = event.getOption("id").getAsString();
                Set<Highlighter> highlighters;
                if (!HIGHLIGHTERS.containsKey(event.getUser().getIdLong())) {
                    event.deferReply().setContent("You do not have a highlighter with this ID!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                highlighters = HIGHLIGHTERS.get(event.getUser().getIdLong());
                
                if (highlighters.isEmpty()) {
                    event.deferReply().setContent("You do not have a highlighter with this ID!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final Optional<Highlighter> optionalHighlighter = highlighters.stream()
                    .filter(highlighter -> highlighter.uuid().toString().equalsIgnoreCase(id)).findFirst();
                if (optionalHighlighter.isEmpty()) {
                    event.deferReply().setContent("You do not have a highlighter with this ID!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                highlighters.remove(optionalHighlighter.get());
                
                final Highlighter highlighter = optionalHighlighter.get();
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.RED);
                embed.setDescription(
                    "❌ Highlighter for text `" + truncateString(highlighter.text(), 15) + "` has been removed!");
                embed.setFooter("ID: " + highlighter.uuid(), event.getUser().getEffectiveAvatarUrl());
                event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                return;
            }
            
            default: {
                event.deferReply(true).setContent("You must provide a valid subcommand (`create`, `list`, `delete`)!")
                    .mentionRepliedUser(false).queue();
                break;
            }
        }
    }

    private Highlighter createGlobalHighlighter(String text, boolean caseSensitive) {
        return new GlobalHighlighter(text, caseSensitive);
    }
    
    private Highlighter createGuildHighlighter(Guild guild, String text, boolean caseSensitive) {
        return new GuildHighlighter(guild.getIdLong(), text, caseSensitive);
    }
    
    // TODO: Utility class
    private static String truncateString(String str, int length) {
        if (str.length() > length)
            return str.substring(0, length - 3) + "...";
        return str;
    }
    
    public interface Highlighter {
        boolean caseSensitive();

        String text();

        long timeAdded();
        
        UUID uuid();
    }
    
    public record GuildHighlighter(long guildId, String text, boolean caseSensitive, UUID uuid, long timeAdded)
        implements Highlighter {
        public GuildHighlighter(long guildId, String text, boolean caseSensitive) {
            this(guildId, text, caseSensitive, UUID.randomUUID(), System.currentTimeMillis());
        }
    }
    
    public record GlobalHighlighter(String text, boolean caseSensitive, UUID uuid, long timeAdded)
        implements Highlighter {
        public GlobalHighlighter(String text, boolean caseSensitive) {
            this(text, caseSensitive, UUID.randomUUID(), System.currentTimeMillis());
        }
    }
}
