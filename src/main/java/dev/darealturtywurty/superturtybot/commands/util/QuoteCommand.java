package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Quote;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class QuoteCommand extends CoreCommand {
    private static final Map<Long, String> QUOTE_MESSAGE_IDS = new HashMap<>();

    public QuoteCommand() {
        super(new Types(true, false, true, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Quote someone's message.";
    }

    @Override
    public String getName() {
        return "quote";
    }

    @Override
    public String getRichName() {
        return "Quote";
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(new SubcommandData("add", "Adds a quote.")
                        .addOption(OptionType.STRING, "text", "The text of the quote.", true)
                        .addOption(OptionType.USER, "user", "The user who said the quote.", true),
                new SubcommandData("remove", "Removes a quote.")
                        .addOption(OptionType.INTEGER, "number", "The number of the quote to remove.", true),
                new SubcommandData("list", "Lists all quotes.")
                        .addOption(OptionType.USER, "user", "The user to list quotes for.", false),
                new SubcommandData("get", "Gets a quote.")
                        .addOption(OptionType.INTEGER, "number", "The number of the quote to get.", true),
                new SubcommandData("random", "Gets a random quote."));
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must provide a subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "add" -> {
                Member authorMember = event.getMember();
                if (authorMember == null) {
                    reply(event, "❌ You must be in the server to use this command!", false, true);
                    return;
                }

                if (!authorMember.hasPermission(Permission.VIEW_CHANNEL)) {
                    reply(event, "❌ You must be able to read messages to use this command!", false, true);
                    return;
                }

                User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
                String text = event.getOption("text", "", OptionMapping::getAsString);

                Member member = guild.getMember(user);
                if (member == null || member.getIdLong() == event.getUser().getIdLong()) {
                    reply(event, "❌ You can't quote yourself!", false, true);
                    return;
                }

                if (user.isSystem() || user.isBot()) {
                    reply(event, "❌ You can't quote a bot or system user!", false, true);
                    return;
                }

                if (text.length() > 1000) {
                    reply(event, "❌ That quote must be at maximum 100 characters!", false, true);
                    return;
                }

                if (text.isBlank() || text.length() < 3) {
                    reply(event, "❌ That quote must be at least 3 characters.", false, true);
                    return;
                }

                List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (quotes.stream().anyMatch(quote -> quote.getText().equals(text))) {
                    reply(event, "❌ That quote already exists!", false, true);
                    return;
                }

                // Search the last 100 messages to see if that text was said by that user
                reply(event, "Checking to see if that quote was said...");
                GuildMessageChannel channel = event.getChannel().asGuildMessageChannel();
                channel.getHistory().retrievePast(100).queue(messages -> {
                    AtomicReference<Message> found = new AtomicReference<>();
                    for (var message : messages) {
                        if (message.getAuthor().getIdLong() != user.getIdLong() || !message.getContentRaw()
                                .toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
                            continue;
                        }

                        found.set(message);
                    }

                    if (found.get() == null) {
                        event.getHook().editOriginal("❌ That quote was not found!").queue();
                        return;
                    }

                    var quote = new Quote(guild.getIdLong(), found.get().getChannelIdLong(), found.get().getIdLong(),
                            user.getIdLong(), event.getUser().getIdLong(), text, found.get().getTimeCreated().toInstant().toEpochMilli());
                    Database.getDatabase().quotes.insertOne(quote);
                    event.getHook().editOriginal("✅ Quote added! #" + (quotes.size() + 1)).queue();
                });
            }

            case "remove" -> {
                int number = event.getOption("number", 0, OptionMapping::getAsInt);
                List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (number < 1 || number > quotes.size()) {
                    reply(event, "❌ That quote does not exist!", false, true);
                    return;
                }

                quotes = quotes.stream().sorted(Comparator.comparingLong(Quote::getTimestamp)).toList();
                Quote quote = quotes.get(number - 1);
                if (quote.getAddedBy() != event.getUser().getIdLong() && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    reply(event, "❌ You cannot remove someone else's quote!", false, true);
                    return;
                }

                DeleteResult result = Database.getDatabase().quotes.deleteOne(
                        Filters.and(
                                Filters.eq("guild", guild.getIdLong()),
                                Filters.eq("timestamp", quote.getTimestamp()),
                                Filters.eq("addedBy", quote.getAddedBy()),
                                Filters.eq("text", quote.getText()),
                                Filters.eq("user", quote.getUser())
                        ));
                if (result.getDeletedCount() == 0) {
                    reply(event, "❌ That quote does not exist!", false, true);
                    return;
                }

                reply(event, "✅ Quote #" + number + " removed!");
            }

            case "list" -> {
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                Bson filter = user == null ?
                        Filters.eq("guild", guild.getIdLong()) :
                        Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
                List<Quote> quotes = Database.getDatabase().quotes.find(filter).into(new ArrayList<>());
                if (quotes.isEmpty()) {
                    reply(event, "❌ There are no quotes!", false, true);
                    return;
                }

                event.deferReply().queue();

                quotes = quotes.stream().sorted(Comparator.comparingLong(Quote::getTimestamp)).toList();

                var contents = new PaginatedEmbed.ContentsBuilder();
                for (int index = 0; index < quotes.size(); index++) {
                    Quote quote = quotes.get(index);

                    String text = quote.getText();
                    if (text.length() > 100) {
                        text = text.substring(0, 100) + "...";
                    }

                    User saidBy = event.getJDA().getUserById(quote.getUser());
                    User addedBy = event.getJDA().getUserById(quote.getAddedBy());

                    String saidByName = saidBy == null ? "Unknown" : saidBy.getAsMention();
                    String addedByName = addedBy == null ? "Unknown" : addedBy.getAsMention();

                    contents.field("Quote #" + (index + 1), "%s%n%nSaid by: %s%nAdded by: %s%nHappened: %s%nLink: [Jump to Message](https://discord.com/channels/%d/%d/%d)".formatted(
                                    text,
                                    saidByName,
                                    addedByName,
                                    TimeFormat.RELATIVE.format(quote.getTimestamp()),
                                    quote.getGuild(), quote.getChannel(), quote.getMessage()),
                            false);
                }

                PaginatedEmbed embed = new PaginatedEmbed.Builder(5, contents)
                        .title("Quotes " + (user == null ? "" : "by " + user.getEffectiveName()) + " in " + guild.getName())
                        .description("Total Quotes: " + quotes.size())
                        .color(Color.CYAN)
                        .timestamp(Instant.now())
                        .footer("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .authorOnly(event.getUser().getIdLong())
                        .build(event.getJDA());

                embed.send(event.getHook(),
                        () -> event.getHook().editOriginal("❌ Failed to send quotes!").queue());
            }

            case "random" -> {
                List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (quotes.isEmpty()) {
                    reply(event, "❌ There are no quotes!", false, true);
                    return;
                }

                quotes = quotes.stream().sorted(Comparator.comparingLong(Quote::getTimestamp)).toList();

                int index = ThreadLocalRandom.current().nextInt(quotes.size());
                Quote quote = quotes.get(index);

                User saidBy = event.getJDA().getUserById(quote.getUser());
                User addedBy = event.getJDA().getUserById(quote.getAddedBy());

                String saidByName = saidBy == null ? "Unknown" : saidBy.getAsMention();
                String addedByName = addedBy == null ? "Unknown" : addedBy.getAsMention();

                var builder = new EmbedBuilder()
                        .setTitle("Quote #" + index)
                        .setDescription(quote.getText())
                        .addField("Said by", saidByName, true)
                        .addField("Added by", addedByName, true)
                        .addField("Date", TimeFormat.RELATIVE.format(quote.getTimestamp()), true)
                        .addField("Link", "[Jump to Message](https://discord.com/channels/%d/%d/%d)".formatted(
                                quote.getGuild(), quote.getChannel(), quote.getMessage()), true)
                        .setColor(Color.CYAN)
                        .setTimestamp(Instant.now())
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

                event.replyEmbeds(builder.build()).queue();
            }

            case "get" -> {
                int number = event.getOption("number", 0, optionMapping -> (int) Math.min(Integer.MAX_VALUE, optionMapping.getAsLong()));
                List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (number < 1 || number > quotes.size()) {
                    reply(event, "❌ That quote does not exist!", false, true);
                    return;
                }

                quotes = quotes.stream().sorted(Comparator.comparingLong(Quote::getTimestamp)).toList();
                Quote quote = quotes.get(number - 1);

                User saidBy = event.getJDA().getUserById(quote.getUser());
                User addedBy = event.getJDA().getUserById(quote.getAddedBy());

                String saidByName = saidBy == null ? "Unknown" : saidBy.getAsMention();
                String addedByName = addedBy == null ? "Unknown" : addedBy.getAsMention();

                var builder = new EmbedBuilder()
                        .setTitle("Quote #" + (quotes.indexOf(quote) + 1))
                        .setDescription(quote.getText())
                        .addField("Said by", saidByName, true)
                        .addField("Added by", addedByName, true)
                        .addField("Date", TimeFormat.RELATIVE.format(quote.getTimestamp()), true)
                        .addField("Link", "[Jump to Message](https://discord.com/channels/%d/%d/%d)".formatted(
                                quote.getGuild(), quote.getChannel(), quote.getMessage()), true)
                        .setColor(Color.CYAN)
                        .setTimestamp(Instant.now())
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

                event.replyEmbeds(builder.build()).queue();
            }
        }
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Guild guild = event.getGuild();
        if (!event.isFromGuild() || event.getMember() == null || guild == null) {
            event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
            return;
        }

        if (!event.getMember().hasPermission(Permission.VIEW_CHANNEL)) {
            event.reply("❌ You must be able to read messages to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = event.getUser();
        Message message = event.getTarget();

        event.deferReply().queue();

        List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                .into(new ArrayList<>());
        if (quotes.stream().anyMatch(quote -> quote.getText().equals(message.getContentRaw()))) {
            event.getHook().editOriginal("❌ That quote already exists!").queue();
            return;
        }

        if (message.getContentRaw().isBlank() || message.getContentRaw().length() < 3) {
            event.getHook().editOriginal("❌ That message must be at least 4 characters.").queue();
            return;
        }

        var quote = new Quote(guild.getIdLong(), message.getChannelIdLong(), message.getIdLong(),
                message.getAuthor().getIdLong(), user.getIdLong(), message.getContentRaw(),
                message.getTimeCreated().toInstant().toEpochMilli());
        Database.getDatabase().quotes.insertOne(quote);
        event.getHook().editOriginal("✅ Quote added! #" + (quotes.size() + 1)).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() ||
                event.getGuild() == null ||
                event.getButton().getId() == null ||
                !event.getButton().getId().startsWith("quote_")) return;

        String[] args = event.getButton().getId().split("-");
        if (args.length != 5) return;

        boolean confirm = args[0].contains("confirm");
        long guildId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long addedBy = Long.parseLong(args[3]);
        long messageId = Long.parseLong(args[4]);
        String text = QUOTE_MESSAGE_IDS.get(messageId);

        if (text == null) {
            event.reply("❌ This interaction has expired!").setEphemeral(true).queue();
            return;
        }

        if (event.getGuild().getIdLong() != guildId) {
            event.reply("❌ That quote does not exist!").setEphemeral(true).queue();
            return;
        }

        if (event.getMessageIdLong() != messageId) {
            event.reply("❌ That quote does not exist!").setEphemeral(true).queue();
            return;
        }

        if (event.getUser().getIdLong() != addedBy) {
            event.reply("❌ You cannot interact with someone else's quote!").setEphemeral(true).queue();
            return;
        }

        QUOTE_MESSAGE_IDS.remove(messageId, text);

        if (!confirm) {
            event.getMessage().delete().queue();
            return;
        }

        List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", event.getGuild().getIdLong()))
                .into(new ArrayList<>());

        var quote = new Quote(guildId, event.getChannelIdLong(), messageId, userId, addedBy, text, System.currentTimeMillis());
        Database.getDatabase().quotes.insertOne(quote);
        event.getMessage().editMessage("✅ Quote added! #" + (quotes.size() + 1)).setComponents().queue();
    }
}
