package dev.darealturtywurty.superturtybot.commands.core;

import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserEmbeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TagCommand extends CoreCommand {
    public TagCommand() {
        super(new Types(true, false, true, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(new SubcommandData("get", "Retrives an existing tag").addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to retrieve.", true, true)
            ),
            new SubcommandData("create", "Creates a new tag").addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to create.", true),
                new OptionData(OptionType.STRING, "content", "The content of this tag (or embed name)", true),
                new OptionData(OptionType.BOOLEAN, "embed", "Whether or not this is an embed.", false)
            ),
            new SubcommandData("edit", "Edits an existing tag").addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to edit.", true, true),
                new OptionData(OptionType.STRING, "content", "The new content of this tag (or embed name)", true),
                new OptionData(OptionType.BOOLEAN, "embed", "Whether or not this is an embed.", false)
            ),
            new SubcommandData("delete", "Deletes an existing tag").addOptions(
                    new OptionData(OptionType.STRING, "name", "The name of the tag to delete.", true, true)
            ),
            new SubcommandData("list", "List the available tags"));
    }
    
    @Override
    public String getAccess() {
        return "Create, Edit, Delete - Moderator, Get, List - Everyone";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Creates a tag that any user can use to get information about a certain thing.";
    }

    @Override
    public String getHowToUse() {
        return "/tag get [name]\n/tag create [name] [content]\n/tag create [name] [content] [isEmbed]\n/tag edit [name]\n/tag delete [name]\n/tag list";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public String getName() {
        return "tag";
    }

    @Override
    public String getRichName() {
        return "Create Tag";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()) || event.getGuild() == null)
            return;

        final String optionName = event.getFocusedOption().getName();
        if (!"name".equals(optionName))
            return;

        final String subcommand = event.getSubcommandName();
        if (!"edit".equals(subcommand) && !"delete".equals(subcommand) && !"get".equals(subcommand))
            return;

        final String given = event.getFocusedOption().getValue();

        Bson filter;

        if (!"get".equals(subcommand)) {
            filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                Filters.eq("user", event.getUser().getIdLong()));
        } else {
            filter = Filters.eq("guild", event.getGuild().getIdLong());
        }

        final List<Tag> tags = new ArrayList<>();
        Database.getDatabase().tags.find(filter).forEach(tags::add);

        final List<String> results = tags.stream()
                .filter(tag -> tag.getName().contains(given))
                .limit(25)
                .map(Tag::getName)
                .toList();
        event.replyChoiceStrings(results).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getGuild() == null) {
            event.deferReply(true).setContent("❌ You must be in a server to use this modal!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final ModalMapping nameMapping = event.getValues().stream()
            .filter(mapping -> mapping.getId().endsWith("-name_input")).findFirst().orElse(null);
        final ModalMapping contentMapping = event.getValues().stream()
            .filter(mapping -> mapping.getId().endsWith("-content_input")).findFirst().orElse(null);
        if (nameMapping == null || contentMapping == null) {
            event.deferReply(true).setContent("❌ You must provide a name and content!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final String name = nameMapping.getAsString();
        final String content = contentMapping.getAsString();
        final long user = event.getUser().getIdLong();
        Database.getDatabase().tags.insertOne(new Tag(event.getGuild().getIdLong(), user, name,
            "{" + "\"message\":\"" + content.replace("\"", "\\\"") + "\"}"));
        final var embed = new EmbedBuilder();
        embed.setColor(Color.GREEN);
        embed.setDescription("✅ Tag `" + name + "` has been created!");
        embed.setTimestamp(Instant.now());
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final Message message = event.getTarget();
        final String content = message.getContentRaw();
        final Modal.Builder builder = Modal.create("tag-" + message.getIdLong() + "-" + event.getUser().getIdLong(),
            "Create Tag");
        final var nameInput = TextInput.create(builder.getId() + "-name_input", "Tag Name:", TextInputStyle.SHORT)
            .setRequired(true).setPlaceholder("Name").setRequiredRange(2, 64).build();
        final var contentInput = TextInput
            .create(builder.getId() + "-content_input", "Content:", TextInputStyle.PARAGRAPH).setRequired(true)
            .setPlaceholder("Content").setValue(content).setMinLength(2).setMaxLength(2000).build();
        final Modal modal = builder.addComponents(ActionRow.of(nameInput), ActionRow.of(contentInput)).build();
        event.replyModal(modal).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            reply(event, "You can only use this command inside of a server!", false);
            return;
        }
        
        final String subcommand = event.getSubcommandName();
        if (subcommand == null || subcommand.isBlank()) {
            reply(event, "❌ You must provide a valid subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "get" -> {
                final OptionMapping tagName = event.getOption("name");
                if (tagName == null) {
                    reply(event, "This is not a valid action!", true, true);
                    return;
                }

                final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                        Filters.eq("name", tagName.getAsString()));
                final Tag tag = Database.getDatabase().tags.find(filter).first();
                if (tag == null) {
                    reply(event, String.format("❌ I could not find a tag by the name '%s'!", tagName.getAsString()),
                            false, true);
                    return;
                }

                sendData(event, tag.getData());
            }
            case "create" -> {
                final OptionMapping tagName0 = event.getOption("name");
                if (tagName0 == null) {
                    reply(event, "This is not a valid action!", true, true);
                    return;
                }
                final Bson createFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                        Filters.eq("name", tagName0.getAsString()));
                final OptionMapping embedOption = event.getOption("embed");
                if (Database.getDatabase().tags.find(createFilter).first() != null) {
                    reply(event, "❌ A tag with the name `" + tagName0.getAsString() + "` already exists!", false, true);
                    return;
                }

                // TODO: Check user permission
                if (embedOption == null || !embedOption.getAsBoolean()) {
                    final String content = event.getOption("content", null, OptionMapping::getAsString);
                    Database.getDatabase().tags
                            .insertOne(new Tag(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                                    tagName0.getAsString(), "{" + "\"message\":\"" + content.replace("\"", "\\\"") + "\"}"));
                    final var embed = new EmbedBuilder();
                    embed.setColor(Color.GREEN);
                    embed.setDescription("✅ Tag `" + tagName0.getAsString() + "` has been created!");
                    embed.setTimestamp(Instant.now());
                    reply(event, embed);
                    return;
                }

                final String content = event.getOption("content", null, OptionMapping::getAsString);
                long userId = event.getUser().getIdLong();
                UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
                if (userEmbeds == null) {
                    reply(event, "❌ You do not have any embeds! Create one with `/embed create`!", false, true);
                    return;
                }

                if (userEmbeds.getEmbed(content).isEmpty()) {
                    reply(event, "❌ You do not have an embed with that name! Create one with `/embed create`!", false, true);
                    return;
                }

                Database.getDatabase().tags.insertOne(new Tag(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                        tagName0.getAsString(), "{" + "\"embed\":\"" + URLEncoder.encode(content, StandardCharsets.UTF_8) + "\"}"));
                final var embed = new EmbedBuilder();
                embed.setColor(Color.GREEN);
                embed.setDescription("✅ Tag `" + tagName0.getAsString() + "` has been created!");
                embed.setTimestamp(Instant.now());
                reply(event, embed);
            }
            case "edit" -> {
                final OptionMapping tagName1 = event.getOption("name");
                if (tagName1 == null) {
                    reply(event, "❌ This is not a valid action!", false, true);
                    return;
                }

                final Bson editFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                        Filters.eq("user", event.getUser().getIdLong()), Filters.eq("name", tagName1.getAsString()));
                final Tag found = Database.getDatabase().tags.find(editFilter).first();
                if (found == null) {
                    reply(event, "❌ No tag was found by the name of `" + tagName1.getAsString() + "`!", false, true);
                    return;
                }

                final String content = event.getOption("content", "", OptionMapping::getAsString);
                if (content.isBlank()) {
                    reply(event, "❌ You must supply some non-blank content!", false, true);
                    return;
                }

                boolean isEmbed = event.getOption("embed", false, OptionMapping::getAsBoolean);
                if(!isEmbed) {
                    found.setData("{" + "\"message\":\"" + content.replace("\"", "\\\"") + "\"}");
                    final Bson update = Updates.set("data", found.getData());
                    Database.getDatabase().tags.updateOne(editFilter, update);
                    reply(event, "✅ Tag `" + found.getName() + "` has successfully been updated!");
                    return;
                }

                long userId = event.getUser().getIdLong();
                UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
                if (userEmbeds == null) {
                    reply(event, "❌ You do not have any embeds! Create one with `/embed create`!", false, true);
                    return;
                }

                if (userEmbeds.getEmbed(content).isEmpty()) {
                    reply(event, "❌ You do not have an embed with that name! Create one with `/embed create`!", false, true);
                    return;
                }

                found.setData("{" + "\"embed\":\"" + URLEncoder.encode(content, StandardCharsets.UTF_8) + "\"}");
                final Bson update = Updates.set("data", found.getData());
                Database.getDatabase().tags.updateOne(editFilter, update);
                reply(event, "✅ Tag `" + found.getName() + "` has successfully been updated!");
            }
            case "delete" -> {
                final OptionMapping tagName2 = event.getOption("name");
                if (tagName2 == null) {
                    reply(event, "❌ This is not a valid action!", false, true);
                    return;
                }
                final Bson deleteFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                        Filters.eq("user", event.getUser().getIdLong()), Filters.eq("name", tagName2.getAsString()));
                final DeleteResult result = Database.getDatabase().tags.deleteOne(deleteFilter);
                if (result.getDeletedCount() < 1) {
                    reply(event, "❌ No tag was found by the name of `" + tagName2.getAsString() + "`!", false, true);
                    return;
                }
                reply(event, "✅ Tag `" + tagName2.getAsString() + "` has successfully been deleted!");
            }
            case "list" -> {
                final Bson listFilter = Filters.eq("guild", event.getGuild().getIdLong());
                List<Tag> tags = Database.getDatabase().tags.find(listFilter).into(new ArrayList<>());
                if (tags.isEmpty()) {
                    reply(event, "❌ This server has no tags!", false, true);
                    return;
                }

                event.deferReply().queue();

                var contents = new PaginatedEmbed.ContentsBuilder();
                for (Tag tag : tags) {
                    User user = event.getJDA().getUserById(tag.getUser());
                    String name = user == null ? "Unknown" : user.getName();
                    contents.field(tag.getName(), "Created by: " + name);
                }

                PaginatedEmbed embed = new PaginatedEmbed.Builder(15, contents)
                        .title("Tags for " + event.getGuild().getName())
                        .description("Use `/tag <name>` to view a tag!")
                        .color(Color.GREEN)
                        .timestamp(Instant.now())
                        .authorOnly(event.getUser().getIdLong())
                        .footer("Requested by " + event.getUser().getName(), event.getMember() == null ? event.getUser().getEffectiveAvatarUrl() : event.getMember().getEffectiveAvatarUrl())
                        .thumbnail(event.getGuild().getIconUrl())
                        .build(event.getJDA());

                embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ No tags were found!").mentionRepliedUser(false).queue());
            }
            default -> reply(event, "⚠️ This command is still a Work In Progress!", false, true);
        }
    }

    private static void sendData(SlashCommandInteractionEvent event, String data) {
        final JsonObject json = Constants.GSON.fromJson(data, JsonObject.class);
        if (json.has("message")) {
            reply(event, json.get("message").getAsString());
        } else if (json.has("embed")) {
            long userId = event.getUser().getIdLong();
            UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", userId)).first();
            if (userEmbeds == null) {
                reply(event, "❌ You do not have any embeds! Create one with `/embed create`!", false, true);
                return;
            }

            EmbedBuilder embed = userEmbeds.getEmbed(json.get("embed").getAsString()).orElse(null);
            if (embed == null) {
                reply(event, "❌ You do not have an embed with that name! Create one with `/embed create`!", false, true);
                return;
            }

            reply(event, embed);
        }
    }
}
