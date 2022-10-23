package dev.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.conversions.Bson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public class TagCommand extends CoreCommand {
    public TagCommand() {
        super(new Types(true, false, true, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("get", "Retrives an existing tag").addOptions(
            new OptionData(OptionType.STRING, "name", "The name of the tag to retrieve.", true).setAutoComplete(true)),

            new SubcommandData("create", "Creates a new tag").addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to create.", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "content", "The content of this tag", true),
                new OptionData(OptionType.BOOLEAN, "embed", "Whether or not this is an embed.", false)),

            new SubcommandData("edit", "Edits an existing tag").addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to edit.", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "content", "The new content of this tag", true)),

            new SubcommandData("delete", "Deletes an existing tag")
                .addOptions(new OptionData(OptionType.STRING, "name", "The name of the tag to delete.", true)
                    .setAutoComplete(true)),

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
        if (!event.getName().equals(getName()) || !event.isFromGuild())
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

        final List<String> results = tags.stream().filter(tag -> tag.getName().contains(given)).limit(25)
            .map(Tag::getName).toList();
        event.replyChoiceStrings(results).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this modal!").mentionRepliedUser(false)
                .queue();
            return;
        }

        final ModalMapping nameMapping = event.getValues().stream()
            .filter(mapping -> mapping.getId().endsWith("-name_input")).findFirst().get();
        final ModalMapping contentMapping = event.getValues().stream()
            .filter(mapping -> mapping.getId().endsWith("-content_input")).findFirst().get();
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
        final Modal modal = builder.addActionRows(ActionRow.of(nameInput), ActionRow.of(contentInput)).build();
        event.replyModal(modal).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply().setContent("You can only use this command inside of a server!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "get":
                final OptionMapping tagName = event.getOption("name");
                if (tagName == null) {
                    event.deferReply(true).setContent("This is not a valid action!").mentionRepliedUser(true).queue();
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
                break;
            case "create":
                final OptionMapping tagName0 = event.getOption("name");
                if (tagName0 == null) {
                    event.deferReply(true).setContent("This is not a valid action!").mentionRepliedUser(true).queue();
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
                    final String content = event.getOption("content").getAsString();
                    Database.getDatabase().tags
                        .insertOne(new Tag(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                            tagName0.getAsString(), "{" + "\"message\":\"" + content.replace("\"", "\\\"") + "\"}"));
                    final var embed = new EmbedBuilder();
                    embed.setColor(Color.GREEN);
                    embed.setDescription("✅ Tag `" + tagName0.getAsString() + "` has been created!");
                    embed.setTimestamp(Instant.now());
                    reply(event, embed);
                }
                
                break;
            case "edit":
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
                
                found.setData("{" + "\"message\":\"" + content.replace("\"", "\\\"") + "\"}");
                final Bson update = Updates.set("data", found.getData());
                Database.getDatabase().tags.updateOne(editFilter, update);
                reply(event, "✅ Tag `" + found.getName() + "` has successfully been updated!");
                break;
            case "delete":
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
                break;
            case "list":
                final Bson listFilter = Filters.eq("guild", event.getGuild().getIdLong());
                final List<Tag> tags = new ArrayList<>();
                Database.getDatabase().tags.find(listFilter).forEach(tags::add);
                if (tags.isEmpty()) {
                    reply(event, "❌ This server has no tags!", false, true);
                    return;
                }
                
                final var embed = new EmbedBuilder();
                embed.setColor(Color.BLUE);
                embed.setTimestamp(Instant.now());
                embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                    event.getMember().getEffectiveAvatarUrl());
                embed.setTitle("Tags: " + event.getGuild().getName(), event.getGuild().getVanityUrl());
                embed.setThumbnail(event.getGuild().getIconUrl());
                
                final var future = new CompletableFuture<Boolean>();
                final var counter = new AtomicInteger();
                tags.forEach(t -> event.getJDA().retrieveUserById(t.getUser()).queue(user -> {
                    embed.appendDescription("**" + t.getName() + "** - Created By: " + user.getName() + "#"
                        + user.getDiscriminator() + "\n");
                    if (counter.incrementAndGet() >= tags.size()) {
                        future.complete(true);
                    }
                }, error -> future.complete(false)));
                
                future.thenAccept(bool -> reply(event, embed));
                break;
            default:
                reply(event, "⚠️ This command is still a Work In Progress!", false, true);
                break;
        }
    }

    private static void sendData(SlashCommandInteractionEvent event, String data) {
        final JsonObject json = Constants.GSON.fromJson(data, JsonObject.class);
        if (json.has("message")) {
            reply(event, json.get("message").getAsString());
        } else if (json.has("embed")) {
            final JsonObject jsonEmbed = json.getAsJsonObject("embed");
            final var embed = new EmbedBuilder();

            if (jsonEmbed.has("author")) {
                final JsonElement authorElement = jsonEmbed.get("author");
                String text = "", url = null, iconUrl = null;
                if (authorElement.isJsonObject()) {
                    final JsonObject author = authorElement.getAsJsonObject();
                    if (author.has("text")) {
                        text = author.get("text").getAsString();
                    }

                    if (author.has("url")) {
                        url = author.get("url").getAsString();
                    }

                    if (author.has("iconUrl")) {
                        iconUrl = author.get("iconUrl").getAsString();
                    }
                } else {
                    text = authorElement.getAsString();
                }

                embed.setAuthor(text, url, iconUrl);
            }

            if (jsonEmbed.has("color")) {
                final JsonElement colorElement = jsonEmbed.get("color");
                Color color = Color.BLACK;
                if (colorElement.isJsonObject()) {
                    final JsonObject colorObj = colorElement.getAsJsonObject();
                    color = new Color(colorObj.get("red").getAsInt(), colorObj.get("green").getAsInt(),
                        colorObj.get("blue").getAsInt(),
                        colorObj.has("alpha") ? colorObj.get("alpha").getAsInt() : 255);
                } else if (colorElement.isJsonPrimitive()) {
                    final JsonPrimitive primitive = colorElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        color = new Color(primitive.getAsInt());
                    } else {
                        color = Color.decode(primitive.getAsString());
                    }
                }

                embed.setColor(color);
            }

            if (jsonEmbed.has("description")) {
                embed.setDescription(jsonEmbed.get("description").getAsString());
            }

            if (jsonEmbed.has("footer")) {
                final JsonElement footerElement = jsonEmbed.get("footer");
                String text = "", iconUrl = null;
                if (footerElement.isJsonObject()) {
                    final JsonObject footer = footerElement.getAsJsonObject();
                    if (footer.has("text")) {
                        text = footer.get("text").getAsString();
                    }

                    if (footer.has("iconUrl")) {
                        iconUrl = footer.get("iconUrl").getAsString();
                    }
                } else {
                    text = footerElement.getAsString();
                }

                embed.setFooter(text, iconUrl);
            }

            if (jsonEmbed.has("image")) {
                embed.setImage(jsonEmbed.get("image").getAsString());
            }

            if (jsonEmbed.has("thumbnail")) {
                embed.setThumbnail(jsonEmbed.get("thumbnail").getAsString());
            }

            if (jsonEmbed.has("timestamp")) {
                embed.setTimestamp(Instant.ofEpochMilli(jsonEmbed.get("timestamp").getAsLong()));
            }

            if (jsonEmbed.has("title")) {
                final JsonElement title = jsonEmbed.get("title");
                if (title.isJsonObject()) {
                    final JsonObject titleJson = title.getAsJsonObject();
                    if (titleJson.has("url")) {
                        embed.setTitle(titleJson.get("text").getAsString(), titleJson.get("url").getAsString());
                    } else {
                        embed.setTitle(titleJson.get("text").getAsString());
                    }
                } else {
                    embed.setTitle(jsonEmbed.get("title").getAsString());
                }
            }
        }
    }
}
