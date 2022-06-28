package io.github.darealturtywurty.superturtybot.commands.core;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class TagCommand extends CoreCommand {
    private static final Map<Long, Set<Tag>> TAGS = new HashMap<>();
    
    public TagCommand() {
        super(new Types(true, false, true, false));
    }
    
    //@formatter:off
    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("get", "Retrives an existing tag")
            .addOptions(
                new OptionData(OptionType.STRING, "name", "The name of the tag to retrieve.", true).setAutoComplete(true)),

            new SubcommandData("create", "Creates a new tag")
                .addOptions(
                    new OptionData(OptionType.STRING, "name", "The name of the tag to create.", true).setAutoComplete(true),
                    new OptionData(OptionType.STRING, "content", "The content of this tag", true),
                    new OptionData(OptionType.BOOLEAN, "embed", "Whether or not this is an embed.", false)),

            new SubcommandData("edit", "Edits an existing tag")
                .addOptions(
                    new OptionData(OptionType.STRING, "name", "The name of the tag to edit.", true).setAutoComplete(true)),

            new SubcommandData("delete", "Deletes an existing tag")
                .addOptions(
                    new OptionData(OptionType.STRING, "name", "The name of the tag to delete.", true).setAutoComplete(true)),

            new SubcommandData("list", "List the available tags"));
    }
    //@formatter:on

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
        final String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "get":
                final Set<Tag> retTags = TAGS.computeIfPresent(event.getGuild().getIdLong(), (id, tags) -> {
                    final List<Tag> found = tags.stream()
                        .filter(tag -> tag.getName().equalsIgnoreCase(tagName.getAsString())).toList();

                    // TODO: List options of what it thinks the user meant
                    if (found.isEmpty()) {
                        event.deferReply(true)
                            .setContent(
                                String.format("I could not find a tag by the name '%s'!", tagName.getAsString()))
                            .mentionRepliedUser(false).queue();
                        return tags;
                    }

                    if (found.size() == 1) {
                        final Tag tag = found.get(0);
                        if (tag instanceof final MessageTag msgTag) {
                            event.deferReply().setContent(msgTag.message.toString()).mentionRepliedUser(false).queue();
                            return tags;
                        }

                        if (tag instanceof final EmbedTag embedTag) {
                            final var embed = new EmbedBuilder();
                            embed.setTitle(embedTag.title.toString(), embedTag.titleURL.toString());
                            embed.setDescription(embedTag.description.toString());
                            embed.setAuthor(embedTag.author.toString(), embedTag.authorURL.toString(),
                                embedTag.authorIconURL.toString());
                            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                            return tags;
                        }

                        // TODO: Send a message in log channel that pings server owner
                        Constants.LOGGER.error(
                            "What the hell? Something definitely broke.\nGuild: {}\nChannel: {}\nRan By: {}\nTag Name: {}\nTag: {}",
                            event.getGuild().getName() + " [" + event.getGuild().getName() + "]",
                            event.getChannel().getName() + " [" + event.getChannel().getIdLong() + "]",
                            event.getInteraction().getUser().getName()
                                + event.getInteraction().getUser().getDiscriminator() + " ["
                                + event.getInteraction().getUser().getIdLong() + "]",
                            tag.getName(), tag);
                    }

                    return tags;
                });
                
                if (retTags == null || retTags.isEmpty()) {
                    event.deferReply(true).setContent("This server currently has no tags!").mentionRepliedUser(false)
                        .queue();
                    TAGS.computeIfAbsent(event.getGuild().getIdLong(), id -> new HashSet<>());
                }
                
                break;
            case "create":
                final OptionMapping embedOption = event.getOption("embed");

                if (!TAGS.containsKey(event.getGuild().getIdLong())) {
                    TAGS.put(event.getGuild().getIdLong(), new HashSet<>());
                }
                
                final Set<Tag> tags = TAGS.get(event.getGuild().getIdLong());

                if (tags.stream().anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName.getAsString()))) {
                    event.deferReply(true)
                        .setContent("A tag with the name `" + tagName.getAsString() + "` already exists!")
                        .mentionRepliedUser(false).queue();
                    return;
                }

                // TODO: Check user permission
                if (embedOption == null || !embedOption.getAsBoolean()) {
                    final String content = event.getOption("content").getAsString();
                    tags.add(new MessageTag(tagName.getAsString(), content));
                    final var embed = new EmbedBuilder();
                    embed.setColor(Color.GREEN);
                    embed.setDescription("✅ Tag `" + tagName.getAsString() + "` has been created!");
                    embed.setTimestamp(Instant.now());
                    event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                    return;
                }

                //@formatter:off
                // TODO: Accept JSON formatted embed through either normal text or through an attachment and if I cant do that then the alternative is to request a pastebin link or whatever. These options will need to be created.
                //@formatter:on

                break;
            case "edit":
                break;
            case "delete":
                break;
            case "list":
                break;
            default:
                event.deferReply(true).setContent("This is not a valid action!").mentionRepliedUser(true).queue();
                break;
        }
    }
    
    public static class EmbedTag implements Tag {
        public final StringBuilder title = new StringBuilder(), description = new StringBuilder(),
            author = new StringBuilder(), footer = new StringBuilder();
        public final StringBuilder titleURL = new StringBuilder(), authorURL = new StringBuilder(),
            authorIconURL = new StringBuilder(), footerURL = new StringBuilder(), imageURL = new StringBuilder(),
            thumbnailURL = new StringBuilder();
        private Color color;
        private OffsetDateTime timestamp;
        public final List<Field> fields = new ArrayList<>();
        public final String name;

        public EmbedTag(String name, Builder builder) {
            this.name = name;
            this.title.append(builder.title);
            this.titleURL.append(builder.titleURL);
            this.description.append(builder.description);
            this.author.append(builder.author);
            this.authorURL.append(builder.authorURL);
            this.authorIconURL.append(builder.authorIconURL);
            this.footer.append(builder.footer);
            this.footerURL.append(builder.footerURL);
            this.imageURL.append(builder.imageURL);
            this.thumbnailURL.append(builder.thumbnailURL);
            this.color = builder.color;
            this.timestamp = builder.timestamp;
            this.fields.addAll(builder.fields);
        }
        
        public Color getColor() {
            return this.color;
        }
        
        @Override
        public String getName() {
            return this.name;
        }

        public OffsetDateTime getTimestamp() {
            return this.timestamp;
        }

        @Override
        public void read(JsonObject json) {
            reset();
            this.title.append(json.get("Title").getAsString());
            this.titleURL.append(json.get("TitleURL").getAsString());
            this.description.append(json.get("Description").getAsString());
            this.author.append(json.get("Author").getAsString());
            this.authorURL.append(json.get("AuthorURL").getAsString());
            this.authorIconURL.append(json.get("AuthorIconURL").getAsString());
            this.footer.append(json.get("Footer").getAsString());
            this.footerURL.append(json.get("FooterURL").getAsString());
            this.imageURL.append(json.get("ImageURL").getAsString());
            this.thumbnailURL.append(json.get("ThumbnailURL").getAsString());
            this.color = new Color(json.get("Color").getAsInt());
            this.timestamp = OffsetDateTime.parse(json.get("Timestamp").getAsString());
            
            final JsonArray fields = json.getAsJsonArray("Fields");
            for (final var fieldElem : fields) {
                final var fieldObj = fieldElem.getAsJsonObject();
                final var field = new Field(fieldObj.get("Name").getAsString(), fieldObj.get("Value").getAsString(),
                    fieldObj.get("Inline").getAsBoolean());
                this.fields.add(field);
            }
        }

        public EmbedTag setColor(Color color) {
            if (color != null) {
                this.color = color;
            }
            return this;
        }

        public EmbedTag setTimestamp(OffsetDateTime time) {
            if (time != null) {
                this.timestamp = time;
            }
            return this;
        }

        @Override
        public JsonObject write() {
            final var json = new JsonObject();
            json.addProperty("Title", this.title.toString());
            json.addProperty("TitleURL", this.titleURL.toString());
            json.addProperty("Description", this.description.toString());
            json.addProperty("Author", this.author.toString());
            json.addProperty("AuthorURL", this.authorURL.toString());
            json.addProperty("AuthorIconURL", this.authorIconURL.toString());
            json.addProperty("Footer", this.footer.toString());
            json.addProperty("FooterURL", this.footerURL.toString());
            json.addProperty("ImageURL", this.imageURL.toString());
            json.addProperty("ThumbnailURL", this.thumbnailURL.toString());
            json.addProperty("Color", this.color.getRGB());
            json.addProperty("Timestamp", this.timestamp.toString());

            final var fields = new JsonArray();
            for (final var field : this.fields) {
                final var fieldObj = new JsonObject();
                fieldObj.addProperty("Name", field.getName());
                fieldObj.addProperty("Value", field.getValue());
                fieldObj.addProperty("Inline", field.isInline());
                fields.add(fieldObj);
            }
            
            json.add("Fields", fields);
            return json;
        }

        protected void reset() {
            this.title.delete(0, this.title.length());
            this.description.delete(0, this.description.length());
            this.author.delete(0, this.author.length());
            this.footer.delete(0, this.footer.length());
            this.titleURL.delete(0, this.titleURL.length());
            this.authorURL.delete(0, this.authorURL.length());
            this.authorIconURL.delete(0, this.authorIconURL.length());
            this.footerURL.delete(0, this.footerURL.length());
            this.imageURL.delete(0, this.imageURL.length());
            this.thumbnailURL.delete(0, this.thumbnailURL.length());
            this.color = Color.BLACK;
            this.timestamp = Instant.now().atOffset(ZoneOffset.UTC);
            this.fields.clear();
        }
        
        public static final class Builder {
            private String title, description, author, footer;
            private String titleURL, authorURL, authorIconURL, footerURL, imageURL, thumbnailURL;
            private Color color = Color.BLACK;
            private OffsetDateTime timestamp = Instant.now().atOffset(ZoneOffset.UTC);
            private List<Field> fields = new ArrayList<>();

            public Builder(MessageEmbed embed) {
                this.title = embed.getTitle();
                this.titleURL = embed.getUrl();
                this.description = embed.getDescription();
                this.author = embed.getAuthor().getName();
                this.authorURL = embed.getAuthor().getUrl();
                this.authorIconURL = embed.getAuthor().getIconUrl();
                this.footer = embed.getFooter().toString();
                this.footerURL = embed.getFooter().getIconUrl();
                this.imageURL = embed.getImage().getUrl();
                this.thumbnailURL = embed.getThumbnail().getUrl();
                this.color = embed.getColor();
                this.timestamp = embed.getTimestamp();
                this.fields = embed.getFields();
            }
            
            public Builder addFields(Field... fields) {
                Collections.addAll(this.fields, fields);
                return this;
            }
            
            public Builder appendDescription(String toAppend) {
                this.description += toAppend;
                return this;
            }
            
            public Builder setAuthor(String author) {
                this.author = author;
                return this;
            }
            
            public Builder setAuthorIconURL(String authorIconURL) {
                this.authorIconURL = authorIconURL;
                return this;
            }
            
            public Builder setAuthorURL(String authorURL) {
                this.authorURL = authorURL;
                return this;
            }
            
            public Builder setColor(Color color) {
                this.color = color;
                return this;
            }
            
            public Builder setDescription(String description) {
                this.description = description;
                return this;
            }
            
            public Builder setFields(List<Field> fields) {
                this.fields = fields;
                return this;
            }
            
            public Builder setFooter(String footer) {
                this.footer = footer;
                return this;
            }
            
            public Builder setFooterURL(String footerURL) {
                this.footerURL = footerURL;
                return this;
            }
            
            public Builder setImageURL(String imageURL) {
                this.imageURL = imageURL;
                return this;
            }
            
            public Builder setThumbnailURL(String thumbnailURL) {
                this.thumbnailURL = thumbnailURL;
                return this;
            }
            
            public Builder setTimestamp(OffsetDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder setTitle(String title) {
                this.title = title;
                return this;
            }

            public Builder setTitleURL(String titleURL) {
                this.titleURL = titleURL;
                return this;
            }
        }
    }

    public static class MessageTag implements Tag {
        public final StringBuilder message = new StringBuilder();
        public final String name;

        public MessageTag(String name, String message) {
            this.name = name;
            this.message.append(message);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public void read(JsonObject json) {
            reset();
            this.message.append(json.get("Message").getAsString());
        }

        @Override
        public JsonObject write() {
            final var json = new JsonObject();
            json.addProperty("Message", this.message.toString());
            return json;
        }
        
        protected void reset() {
            this.message.delete(0, this.message.length());
        }
    }

    public interface Tag {
        String getName();

        void read(JsonObject json);

        JsonObject write();
    }
}
