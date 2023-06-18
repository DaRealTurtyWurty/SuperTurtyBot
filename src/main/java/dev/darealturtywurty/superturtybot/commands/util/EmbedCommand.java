package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserEmbeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Map.entry;
import static net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE;

public class EmbedCommand extends CoreCommand {
    public EmbedCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Creates/Edits/Deletes/Sends an embed.";
    }

    @Override
    public String getName() {
        return "embed";
    }

    @Override
    public String getRichName() {
        return "Embed";
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("create", "Creates an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "A name given for this embed (used when doing /embed run)", true),
                        new OptionData(OptionType.STRING, "title", "The title of the embed", true),
                        new OptionData(OptionType.STRING, "url", "The title's url", false),
                        new OptionData(OptionType.STRING, "description", "The description of the embed", false),
                        new OptionData(OptionType.STRING, "color", "The color of the embed", false, true),
                        new OptionData(OptionType.STRING, "footer", "The footer of the embed", false),
                        new OptionData(OptionType.STRING, "footer_icon_url", "The footer's icon url", false),
                        new OptionData(OptionType.STRING, "image_url", "The image url of the embed", false),
                        new OptionData(OptionType.STRING, "thumbnail_url", "The thumbnail url of the embed", false),
                        new OptionData(OptionType.STRING, "author", "The author of the embed", false),
                        new OptionData(OptionType.STRING, "author_icon_url", "The author's icon url", false),
                        new OptionData(OptionType.STRING, "author_url", "The author's url", false),
                        new OptionData(OptionType.STRING, "timestamp", "The timestamp of the embed", false)
                ),
                new SubcommandData("edit", "Edits an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to edit", true, true),
                        new OptionData(OptionType.STRING, "title", "The title of the embed", false),
                        new OptionData(OptionType.STRING, "url", "The title's url", false),
                        new OptionData(OptionType.STRING, "description", "The description of the embed", false),
                        new OptionData(OptionType.STRING, "color", "The color of the embed", false, true),
                        new OptionData(OptionType.STRING, "footer", "The footer of the embed", false),
                        new OptionData(OptionType.STRING, "footer_icon_url", "The footer's icon url", false),
                        new OptionData(OptionType.STRING, "image_url", "The image url of the embed", false),
                        new OptionData(OptionType.STRING, "thumbnail_url", "The thumbnail url of the embed", false),
                        new OptionData(OptionType.STRING, "author", "The author of the embed", false),
                        new OptionData(OptionType.STRING, "author_icon_url", "The author's icon url", false),
                        new OptionData(OptionType.STRING, "author_url", "The author's url", false),
                        new OptionData(OptionType.STRING, "timestamp", "The timestamp of the embed", false)
                ),
                new SubcommandData("add_field", "Adds a field to an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to add a field to", true, true),
                        new OptionData(OptionType.STRING, "field_name", "The name of the field", true),
                        new OptionData(OptionType.STRING, "field_value", "The value of the field", true),
                        new OptionData(OptionType.BOOLEAN, "inline", "Whether the field is inline or not", false)
                ),
                new SubcommandData("delete_field", "Deletes a field from an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to delete a field from", true),
                        new OptionData(OptionType.INTEGER, "field_index", "The index of the field to delete", true)
                ),
                new SubcommandData("edit_field", "Edits the field in an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to edit a field in", true, true),
                        new OptionData(OptionType.INTEGER, "field_index", "The index of the field to edit", true)
                                .setRequiredRange(0, 24),
                        new OptionData(OptionType.STRING, "field_name", "The name of the field", false),
                        new OptionData(OptionType.STRING, "field_value", "The value of the field", false),
                        new OptionData(OptionType.BOOLEAN, "inline", "Whether the field is inline or not", false)
                ),
                new SubcommandData("delete", "Deletes an embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to delete", true, true)
                ),
                new SubcommandData("view", "Views the embed").addOptions(
                        new OptionData(OptionType.STRING, "name", "The name of the embed to view", true, true)
                ),
                new SubcommandData("list", "Lists all embeds")
        );
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if(subcommand == null || subcommand.isBlank()) {
            reply(event, "❌ Invalid subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "create" -> runCreateEmbed(event);
            case "edit" -> runEditEmbed(event);
            case "add_field" -> runAddFieldEmbed(event);
            case "delete_field" -> runDeleteFieldEmbed(event);
            case "edit_field" -> runEditFieldEmbed(event);
            case "delete" -> runDeleteEmbed(event);
            case "view" -> runViewEmbed(event);
            case "list" -> runListEmbeds(event);
            default -> reply(event, "❌ Invalid subcommand!", false, true);
        }
    }

    private void runCreateEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", UUID.randomUUID().toString(), OptionMapping::getAsString);
        String title = event.getOption("title", "", OptionMapping::getAsString);
        String url = event.getOption("url", null, OptionMapping::getAsString);
        String description = event.getOption("description", "", OptionMapping::getAsString);
        String color = event.getOption("color", "black", OptionMapping::getAsString);
        String footer = event.getOption("footer", "", OptionMapping::getAsString);
        String footerIconUrl = event.getOption("footer_icon_url", null, OptionMapping::getAsString);
        String imageUrl = event.getOption("image_url", null, OptionMapping::getAsString);
        String thumbnailUrl = event.getOption("thumbnail_url", null, OptionMapping::getAsString);
        String author = event.getOption("author", "", OptionMapping::getAsString);
        String authorIconUrl = event.getOption("author_icon_url", null, OptionMapping::getAsString);
        String authorUrl = event.getOption("author_url", null, OptionMapping::getAsString);
        String timestamp = event.getOption("timestamp", null, OptionMapping::getAsString);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            userEmbeds = new UserEmbeds(userId);
            Database.getDatabase().userEmbeds.insertOne(userEmbeds);
        }

        if(userEmbeds.getEmbed(name).isPresent()) {
            reply(event, "❌ An embed with that name already exists!", false, true);
            return;
        }

        var embed = new EmbedBuilder();
        embed.setTitle(title, url);
        embed.setDescription(description);
        embed.setColor(parseColor(color));
        embed.setFooter(footer, footerIconUrl);
        embed.setImage(imageUrl);
        embed.setThumbnail(thumbnailUrl);
        embed.setAuthor(author, authorUrl, authorIconUrl);
        embed.setTimestamp(timestamp == null ? null : parseTimestamp(timestamp));

        userEmbeds.addEmbed(name, embed.build());
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));

        reply(event, "✅ Successfully created embed `" + name + "`", false, true);
    }

    private void runEditEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);
        String title = event.getOption("title", null, OptionMapping::getAsString);
        String url = event.getOption("url", null, OptionMapping::getAsString);
        String description = event.getOption("description", null, OptionMapping::getAsString);
        String color = event.getOption("color", null, OptionMapping::getAsString);
        String footer = event.getOption("footer", null, OptionMapping::getAsString);
        String footerIconUrl = event.getOption("footer_icon_url", null, OptionMapping::getAsString);
        String imageUrl = event.getOption("image_url", null, OptionMapping::getAsString);
        String thumbnailUrl = event.getOption("thumbnail_url", null, OptionMapping::getAsString);
        String author = event.getOption("author", null, OptionMapping::getAsString);
        String authorIconUrl = event.getOption("author_icon_url", null, OptionMapping::getAsString);
        String authorUrl = event.getOption("author_url", null, OptionMapping::getAsString);
        String timestamp = event.getOption("timestamp", null, OptionMapping::getAsString);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        var embed = userEmbeds.getEmbed(name).get();
        MessageEmbed oldEmbed = embed.build();
        if(title != null) embed.setTitle(title, url == null ? oldEmbed.getUrl() : url);
        if(url != null) embed.setUrl(url);
        if(description != null) embed.setDescription(description);
        if(color != null) embed.setColor(parseColor(color));
        if(footer != null) embed.setFooter(footer, footerIconUrl == null ? oldEmbed.getFooter().getIconUrl() : footerIconUrl);
        if(footerIconUrl != null) embed.setFooter(footer == null ? oldEmbed.getFooter().getText() : footer, footerIconUrl);
        if(imageUrl != null) embed.setImage(imageUrl);
        if(thumbnailUrl != null) embed.setThumbnail(thumbnailUrl);
        if(author != null) embed.setAuthor(author, authorUrl == null ? oldEmbed.getAuthor().getUrl() : authorUrl, authorIconUrl == null ? oldEmbed.getAuthor().getIconUrl() : authorIconUrl);
        if(authorIconUrl != null) embed.setAuthor(author == null ? oldEmbed.getAuthor().getName() : author, authorUrl == null ? oldEmbed.getAuthor().getUrl() : authorUrl, authorIconUrl);
        if(authorUrl != null) embed.setAuthor(author == null ? oldEmbed.getAuthor().getName() : author, authorUrl, authorIconUrl == null ? oldEmbed.getAuthor().getIconUrl() : authorIconUrl);
        if(timestamp != null) embed.setTimestamp(parseTimestamp(timestamp));

        userEmbeds.setEmbed(name, embed.build());
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));

        reply(event, "✅ Successfully edited embed!", false, true);
    }

    private void runDeleteEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        userEmbeds.removeEmbed(name);
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));

        reply(event, "✅ Successfully deleted embed `" + name + "`", false, true);
    }

    private void runViewEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        var embed = userEmbeds.getEmbed(name).get();
        event.replyEmbeds(embed.build()).queue();
    }

    private void runListEmbeds(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        for(var entry : userEmbeds.getEmbeds().entrySet()) {
            Optional<EmbedBuilder> embed = userEmbeds.getEmbed(entry.getKey());
            if(embed.isEmpty()) continue;

            contents.field(entry.getKey(), embed.get().build().getTitle());
        }

        event.deferReply().queue();

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                .title(event.getUser().getName() + "'s Embeds")
                .description("Use `/embed view <name>` to view an embed")
                .color(event.getMember() == null ? Color.GRAY : new Color(event.getMember().getColorRaw()))
                .timestamp(Instant.now())
                .authorOnly(event.getUser().getIdLong())
                .thumbnail(event.getMember() == null ? event.getUser().getEffectiveAvatarUrl() : event.getMember().getEffectiveAvatarUrl())
                .build(event.getJDA());

        embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ No embeds found!").queue());
    }

    private void runAddFieldEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);
        String fieldName = event.getOption("field_name", ZERO_WIDTH_SPACE, OptionMapping::getAsString);
        String fieldValue = event.getOption("field_value", ZERO_WIDTH_SPACE, OptionMapping::getAsString);
        boolean inline = event.getOption("inline", false, OptionMapping::getAsBoolean);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        var embed = userEmbeds.getEmbed(name).get();
        if(embed.getFields().size() >= 25) {
            reply(event, "❌ You cannot have more than 25 fields in an embed", false, true);
            return;
        }

        embed.addField(fieldName, fieldValue, inline);
        userEmbeds.setEmbed(name, embed.build());
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));
        reply(event, "✅ Successfully added field to embed `" + name + "`", false, true);
    }

    private void runDeleteFieldEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);
        int fieldIndex = event.getOption("field_index", 0, OptionMapping::getAsInt);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        var embed = userEmbeds.getEmbed(name).get();
        if(fieldIndex >= embed.getFields().size()) {
            reply(event, "❌ That embed does not have a field at that index", false, true);
            return;
        }

        embed.getFields().remove(fieldIndex);
        userEmbeds.setEmbed(name, embed.build());
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));
        reply(event, "✅ Successfully removed field from embed `" + name + "`", false, true);
    }

    public void runEditFieldEmbed(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String name = event.getOption("name", null, OptionMapping::getAsString);
        int fieldIndex = event.getOption("field_index", 0, OptionMapping::getAsInt);
        String fieldName = event.getOption("field_name", ZERO_WIDTH_SPACE, OptionMapping::getAsString);
        String fieldValue = event.getOption("field_value", ZERO_WIDTH_SPACE, OptionMapping::getAsString);
        boolean inline = event.getOption("inline", false, OptionMapping::getAsBoolean);

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            reply(event, "❌ You don't have any embeds!", false, true);
            return;
        }

        if(userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have an embed with that name", false, true);
            return;
        }

        var embed = userEmbeds.getEmbed(name).get();
        if(fieldIndex >= embed.getFields().size()) {
            reply(event, "❌ That embed does not have a field at that index", false, true);
            return;
        }

        embed.getFields().set(fieldIndex, new MessageEmbed.Field(fieldName, fieldValue, inline));
        userEmbeds.setEmbed(name, embed.build());
        Database.getDatabase().userEmbeds.updateOne(Filters.eq("user", userId), Updates.set("embeds", userEmbeds.getEmbeds()));
        reply(event, "✅ Successfully edited field from embed `" + name + "`", false, true);
    }

    private static final Map<String, Color> COMMON_COLORS = Map.ofEntries(
                    entry("Black", Color.BLACK),
                    entry("White", Color.WHITE),
                    entry("Red", Color.RED),
                    entry("Green", Color.GREEN),
                    entry("Blue", Color.BLUE),
                    entry("Yellow", Color.YELLOW),
                    entry("Cyan", Color.CYAN),
                    entry("Magenta", Color.MAGENTA),
                    entry("Gray", Color.GRAY),
                    entry("Light Gray", Color.LIGHT_GRAY),
                    entry("Dark Gray", Color.DARK_GRAY),
                    entry("Orange", Color.ORANGE),
                    entry("Pink", Color.PINK)
            );

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if(!event.getName().equals(getName())) return;

        String subcommand = event.getSubcommandName();
        if(subcommand == null || subcommand.isBlank()) {
            event.replyChoices().queue();
            return;
        }

        AutoCompleteQuery focused = event.getFocusedOption();
        String name = focused.getName();
        String value = focused.getValue();

        long userId = event.getUser().getIdLong();
        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
        if(userEmbeds == null) {
            event.replyChoices().queue();
            return;
        }

        if(name.equals("name") &&
                (subcommand.equals("edit") ||
                        subcommand.equals("view") ||
                        subcommand.equals("delete") ||
                        subcommand.equals("add_field") ||
                        subcommand.equals("edit_field") ||
                        subcommand.equals("delete_field"))) {
            event.replyChoiceStrings(userEmbeds.getEmbeds().keySet().stream().filter(str -> {
                if(value.isBlank()) return true;
                return str.toLowerCase().contains(value.toLowerCase());
            }).limit(25).toList()).queue();
            return;
        }

        if(name.equals("field_index") &&
                (subcommand.equals("edit_field") ||
                        subcommand.equals("delete_field"))) {
            String embedName = event.getOption("name", null, OptionMapping::getAsString);
            if(embedName == null || embedName.isBlank()) {
                event.replyChoices().queue();
                return;
            }

            var embed = userEmbeds.getEmbed(embedName);
            if(embed.isEmpty()) {
                event.replyChoices().queue();
                return;
            }

            int fieldCount = embed.get().getFields().size();
            event.replyChoiceStrings(IntStream.range(0, fieldCount).mapToObj(Integer::toString).toList()).queue();
            return;
        }

        if(name.equals("color") &&
                (subcommand.equals("create") ||
                        subcommand.equals("edit"))) {
            event.replyChoiceStrings(COMMON_COLORS.keySet().stream().filter(str -> {
                if(value.isBlank()) return true;
                return str.toLowerCase().contains(value.toLowerCase());
            }).limit(25).toList()).queue();
        }
    }

    private static Color parseColor(String colorStr) {
        if(colorStr.startsWith("#")) {
            return Color.decode(colorStr);
        } else if(colorStr.startsWith("0x")) {
            return Color.decode("#" + colorStr.substring(2));
        } else if(COMMON_COLORS.keySet().stream().anyMatch(colorStr::equalsIgnoreCase)) {
            return COMMON_COLORS.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(colorStr))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse(Color.BLACK);
        } else {
            try {
                return Color.decode("#" + colorStr);
            } catch (NumberFormatException e) {
                return Color.BLACK;
            }
        }
    }

    public static TemporalAccessor parseTimestamp(String timestamp) {
        try {
            // Attempt to parse as ISO-8601 format
            return LocalDateTime.parse(timestamp);
        } catch (DateTimeParseException ignored) {
            // Parsing as ISO-8601 failed, try parsing as custom date format with optional time
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("[d/M/yyyy]['T'HH:mm[:ss]]")
                    .withResolverStyle(ResolverStyle.STRICT);

            try {
                return LocalDateTime.parse(timestamp, dateTimeFormatter);
            } catch (DateTimeParseException ignored2) {
                // Parsing as custom format failed, return null to indicate parsing error
                return null;
            }
        }
    }
}
