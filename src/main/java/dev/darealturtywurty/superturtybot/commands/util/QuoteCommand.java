package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("add", "Adds a quote.")
                        .addOption(OptionType.STRING, "text", "The text of the quote.", true)
                        .addOption(OptionType.USER, "user", "The user who said the quote.", true),
                new SubcommandData("remove", "Removes a quote.")
                        .addOption(OptionType.INTEGER, "number", "The number of the quote to remove.", true),
                new SubcommandData("list", "Lists" + " all quotes."),
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
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        Guild guild = event.getGuild();
        if ("add".equals(event.getSubcommandName())) {
            final var text = event.getOption("text").getAsString();
            final var user = event.getOption("user").getAsUser();
            if (!guild.isMember(user)) {
                reply(event, "❌ That user is not in this server!", false, true);
                return;
            }

            Member member = guild.getMember(user);
            if (member.getIdLong() == event.getUser().getIdLong()) {
                reply(event, "❌ You can't quote yourself!", false, true);
                return;
            }

            if (user.isSystem() || user.isBot()) {
                reply(event, "❌ You can't quote a bot or system user!", false, true);
                return;
            }

            if (text.length() > 1000) {
                reply(event, "❌ That quote is too long!", false, true);
                return;
            }

            if (text.isBlank() || text.length() < 3) {
                reply(event, "❌ That quote is too short!", false, true);
                return;
            }

            List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (quotes.stream().anyMatch(quote -> quote.getText().equals(text))) {
                reply(event, "❌ That quote already exists!", false, true);
                return;
            }

            // Search the last 100 messages to see if that text was said by that user
            event.deferReply().setContent("Checking to see if that quote was said...").queue();
            GuildMessageChannel channel = event.getChannel().asGuildMessageChannel();
            channel.getHistory().retrievePast(100).queue(messages -> {
                AtomicReference<Message> found = new AtomicReference<>();
                for (var message : messages) {
                    if (message.getAuthor().getIdLong() != user.getIdLong() || !message.getContentRaw()
                            .equalsIgnoreCase(text)) {
                        continue;
                    }

                    found.set(message);
                }

                if (found.get() == null) {
                    event.getHook().retrieveOriginal().queue(original -> {
                        var confirmButton = Button.primary(
                                "quote_confirm-" + guild.getId() + "-" + user.getId() + "-" + event.getUser()
                                        .getId() + "-" + original.getId(), "✅");
                        var cancelButton = Button.danger(
                                "quote_cancel-" + guild.getId() + "-" + user.getId() + "-" + event.getUser()
                                        .getId() + "-" + original.getId(), "❌");
                        event.getHook().editOriginal(
                                        "❓That quote was not found in the last 100 messages! Are you sure you typed it correctly?")
                                .setActionRow(confirmButton, cancelButton).queue();

                        QUOTE_MESSAGE_IDS.put(original.getIdLong(), text);
                    });
                } else {
                    var quote = new Quote(guild.getIdLong(), user.getIdLong(), text,
                            found.get().getTimeCreated().toInstant().toEpochMilli(), event.getUser().getIdLong());
                    Database.getDatabase().quotes.insertOne(quote);
                    event.getHook().editOriginal("✅ Quote added! #" + (quotes.size() + 1)).queue();
                }
            });

            return;
        }

        if ("remove".equals(event.getSubcommandName())) {
            int number = event.getOption("number").getAsInt();
            List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (number < 1 || number > quotes.size()) {
                reply(event, "❌ That quote does not exist!", false, true);
                return;
            }

            Quote quote = quotes.get(number - 1);
            if (quote.getAddedBy() != event.getUser().getIdLong() && !event.getMember()
                    .hasPermission(Permission.ADMINISTRATOR)) {
                reply(event, "❌ You can't remove a quote that you didn't add!", false, true);
                return;
            }

            DeleteResult result = Database.getDatabase().quotes.deleteOne(
                    Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("text", quote.getText()),
                            Filters.eq("addedBy", quote.getAddedBy()), Filters.eq("user", quote.getUser()),
                            Filters.eq("timestamp", quote.getTimestamp())));
            if (result.getDeletedCount() == 0) {
                reply(event, "❌ That quote does not exist!", false, true);
                return;
            }

            reply(event, "✅ Quote #" + number + " removed!");
            return;
        }

        if ("list".equals(event.getSubcommandName())) {
            List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (quotes.isEmpty()) {
                reply(event, "❌ There are no quotes in this server!", false, true);
                return;
            }

            event.deferReply().queue();

            var contents = new PaginatedEmbed.ContentsBuilder();
            for (int index = 0; index < quotes.size(); index++) {
                Quote quote = quotes.get(index);
                contents.field("Quote #" + (index + 1), quote.getText());
            }

            PaginatedEmbed embed = new PaginatedEmbed.Builder(2, contents)
                    .title("Quotes in " + guild.getName())
                    .color(Color.GREEN)
                    .timestamp(Instant.now())
                    .authorOnly(event.getUser().getIdLong())
                    .footer("Requested by " + event.getUser().getName(), event.getMember().getEffectiveAvatarUrl())
                    .thumbnail(guild.getIconUrl())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ No quotes found!").queue());

            return;
        }

        if ("random".equals(event.getSubcommandName())) {
            List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (quotes.isEmpty()) {
                reply(event, "❌ There are no quotes in this server!", false, true);
                return;
            }

            int index = ThreadLocalRandom.current().nextInt(quotes.size());
            Quote quote = quotes.get(index);
            var embed = new EmbedBuilder();
            embed.setTitle("Quote #" + (index + 1));
            embed.setColor(Color.GREEN);
            embed.setDescription(quote.getText() + "\n*- " + event.getJDA().getUserById(quote.getUser())
                    .getName() + ", " + Instant.ofEpochMilli(quote.getTimestamp()).atZone(ZoneId.systemDefault())
                    .toLocalDate().getYear() + "*");
            embed.setFooter("Added by " + event.getJDA().getUserById(quote.getAddedBy()).getName(),
                    event.getJDA().getUserById(quote.getAddedBy()).getEffectiveAvatarUrl());
            embed.setTimestamp(Instant.ofEpochMilli(quote.getTimestamp()));

            reply(event, embed);
            return;
        }

        if ("get".equals(event.getSubcommandName())) {
            int number = event.getOption("number").getAsInt();
            List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (number < 1 || number > quotes.size()) {
                reply(event, "❌ That quote does not exist!", false, true);
                return;
            }

            Quote quote = quotes.get(number - 1);
            var embed = new EmbedBuilder();
            embed.setTitle("Quote #" + number);
            embed.setColor(Color.GREEN);
            embed.setDescription(quote.getText() + "\n*\\- " + event.getJDA().getUserById(quote.getUser()).getName() +
                    ", " + Instant.ofEpochMilli(quote.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate().getYear() +
                    "*");
            embed.setFooter("Added by " + event.getJDA().getUserById(quote.getAddedBy()).getName(),
                    event.getJDA().getUserById(quote.getAddedBy()).getEffectiveAvatarUrl());
            embed.setTimestamp(Instant.ofEpochMilli(quote.getTimestamp()));

            reply(event, embed);
        }
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
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

        if (message.getContentRaw().isBlank() || message.getContentRaw().length() < 4) {
            event.getHook().editOriginal("❌ That message is empty/too short!").queue();
            return;
        }

        var quote = new Quote(guild.getIdLong(), message.getAuthor().getIdLong(), message.getContentRaw(),
                message.getTimeCreated().toInstant().toEpochMilli(), user.getIdLong());
        Database.getDatabase().quotes.insertOne(quote);
        event.getHook().editOriginal("✅ Quote added! #" + (quotes.size() + 1)).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;

        if (!event.getButton().getId().startsWith("quote_")) return;

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

        var quote = new Quote(guildId, userId, text, System.currentTimeMillis(), addedBy);
        Database.getDatabase().quotes.insertOne(quote);
        event.getMessage().editMessage("✅ Quote added! #" + (quotes.size() + 1)).setComponents().queue();
    }
}
