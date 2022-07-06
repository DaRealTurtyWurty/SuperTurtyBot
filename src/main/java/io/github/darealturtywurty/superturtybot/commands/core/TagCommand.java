package io.github.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import org.bson.conversions.Bson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

// TODO: Autocomplete
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
                new OptionData(OptionType.STRING, "name", "The name of the tag to edit.", true).setAutoComplete(true)),
            
            new SubcommandData("delete", "Deletes an existing tag")
                .addOptions(new OptionData(OptionType.STRING, "name", "The name of the tag to delete.", true)
                    .setAutoComplete(true)),
            
            new SubcommandData("list", "List the available tags"));
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
    public String getName() {
        return "tag";
    }
    
    @Override
    public String getRichName() {
        return "Create Tag";
    }
    
    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply().setContent("You can only use this command inside of a server!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final OptionMapping tagName = event.getOption("name");
        if (tagName == null) {
            event.deferReply(true).setContent("This is not a valid action!").mentionRepliedUser(true).queue();
            return;
        }
        
        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getUser().getIdLong()), Filters.eq("name", tagName.getAsString()));
        
        final String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "get":
                final Tag tag = Database.getDatabase().tags.find(filter).first();
                
                if (tag == null) {
                    reply(event, String.format("❌ I could not find a tag by the name '%s'!", tagName.getAsString()),
                        false, true);
                    return;
                }
                
                sendData(event, tag.getData());
                break;
            case "create":
                final OptionMapping embedOption = event.getOption("embed");
                
                if (Database.getDatabase().tags.find(filter).first() != null) {
                    reply(event, "❌ A tag with the name `" + tagName.getAsString() + "` already exists!", false, true);
                    return;
                }
                
                // TODO: Check user permission
                if (embedOption == null || !embedOption.getAsBoolean()) {
                    final String content = event.getOption("content").getAsString();
                    Database.getDatabase().tags.insertOne(new Tag(event.getGuild().getIdLong(),
                        event.getUser().getIdLong(), tagName.getAsString(), "{" + "\"message\":\"" + content + "\"}"));
                    final var embed = new EmbedBuilder();
                    embed.setColor(Color.GREEN);
                    embed.setDescription("✅ Tag `" + tagName.getAsString() + "` has been created!");
                    embed.setTimestamp(Instant.now());
                    reply(event, embed);
                }
                
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
