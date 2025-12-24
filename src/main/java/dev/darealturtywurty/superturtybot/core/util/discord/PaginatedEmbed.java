package dev.darealturtywurty.superturtybot.core.util.discord;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE;

@Getter
public class PaginatedEmbed extends ListenerAdapter {
    private final int pageSize;
    private final List<Consumer<EmbedBuilder>> contents;
    private final JDA jda;

    private final String title;
    private final String description;
    private final String footer;
    private final String footerIconUrl;
    private final String thumbnail;
    private final String author;
    private final String authorUrl;
    private final String authorIcon;
    private final Color color;
    private final String url;
    private final String imageUrl;
    private final TemporalAccessor timestamp;
    private final long authorId;

    @Setter
    private Consumer<Message> onMessageUpdate = ignored -> {
    };
    private int page;

    private long guildId;
    private long channelId;
    private long messageId;

    private PaginatedEmbed(Builder builder, JDA jda) {
        this.pageSize = builder.pageSize;
        this.contents = builder.contents;
        this.jda = jda;

        this.title = builder.title;
        this.description = builder.description == null ? null : builder.description.toString();
        this.footer = builder.footer;
        this.footerIconUrl = builder.footerIconUrl;
        this.thumbnail = builder.thumbnail;
        this.author = builder.author;
        this.authorUrl = builder.authorUrl;
        this.authorIcon = builder.authorIcon;
        this.color = builder.color;
        this.url = builder.url;
        this.imageUrl = builder.imageUrl;
        this.timestamp = builder.timestamp;
        this.authorId = builder.authorId;

        this.page = 0;
    }

    public void send(MessageChannel channel, Runnable fallback) {
        this.jda.addEventListener(this);

        if (contents.isEmpty()) {
            fallback.run();
            return;
        }

        MessageEmbed embed = createEmbed();
        channel.sendMessageEmbeds(embed).queue(msg -> {
            Optional<ActionRow> optional = createActionRow(msg.getGuild().getIdLong(), msg.getChannel().getIdLong(), msg.getIdLong());
            optional.ifPresentOrElse(
                    row -> msg.editMessageComponents(row).queue(this.onMessageUpdate),
                    () -> this.onMessageUpdate.accept(msg)
            );
        });
    }

    public void send(WebhookClient<Message> hook, Runnable fallback) {
        this.jda.addEventListener(this);

        if (contents.isEmpty()) {
            fallback.run();
            return;
        }

        MessageEmbed embed = createEmbed();
        hook.sendMessageEmbeds(embed).queue(msg -> {
            Optional<ActionRow> optional = createActionRow(msg.getGuild().getIdLong(), msg.getChannel().getIdLong(), msg.getIdLong());
            optional.ifPresentOrElse(
                    row -> msg.editMessageComponents(row).queue(this.onMessageUpdate),
                    () -> this.onMessageUpdate.accept(msg)
            );
        });
    }

    public void send(MessageChannel channel) {
        send(channel, () -> {
        });
    }

    public void send(WebhookClient<Message> hook) {
        send(hook, () -> {
        });
    }

    public void finish() {
        this.jda.removeEventListener(this);
    }

    private MessageEmbed createEmbed() {
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, contents.size());

        if (fromIndex >= contents.size()) {
            page = 0; // Reset to the first page if the current page is out of range
            fromIndex = 0;
            toIndex = Math.min(fromIndex + pageSize, contents.size());
        }

        List<Consumer<EmbedBuilder>> pageContents = contents.subList(fromIndex, toIndex);

        var builder = new EmbedBuilder()
                .setTitle(this.title, this.url)
                .setDescription(this.description)
                .setFooter(this.footer, this.footerIconUrl)
                .setThumbnail(this.thumbnail)
                .setAuthor(this.author, this.authorUrl, this.authorIcon)
                .setColor(this.color)
                .setImage(this.imageUrl)
                .setTimestamp(this.timestamp);

        pageContents.forEach(builderConsumer -> builderConsumer.accept(builder));

        return builder.build();
    }

    private Optional<ActionRow> createActionRow(long guildId, long channelId, long messageId) {
        if (this.guildId == 0 && this.channelId == 0 && this.messageId == 0) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
        }

        Button first = Button.primary("pagination_first-%d-%d-%d-%d".formatted(guildId, channelId, messageId, authorId), Emoji.fromUnicode("⏮"));
        Button previous = Button.primary("pagination_previous-%d-%d-%d-%d".formatted(guildId, channelId, messageId, authorId), Emoji.fromUnicode("◀"));
        Button next = Button.primary("pagination_next-%d-%d-%d-%d".formatted(guildId, channelId, messageId, authorId), Emoji.fromUnicode("▶"));
        Button last = Button.primary("pagination_last-%d-%d-%d-%d".formatted(guildId, channelId, messageId, authorId), Emoji.fromUnicode("⏭"));

        boolean disabled = contents.size() <= pageSize;
        if (disabled)
            return Optional.empty();

        if (page == 0) {
            first = first.asDisabled();
            previous = previous.asDisabled();
        }

        if (page == (contents.size() - 1) / pageSize) {
            next = next.asDisabled();
            last = last.asDisabled();
        }

        return Optional.of(ActionRow.of(first, previous, next, last));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        String[] split = id.split("-");

        String description = split[0];
        if (!description.startsWith("pagination_")) return;

        String action = description.replace("pagination_", "").trim().toLowerCase(Locale.ROOT);
        long guildId = Long.parseLong(split[1]);
        long channelId = Long.parseLong(split[2]);
        long messageId = Long.parseLong(split[3]);
        long authorId = Long.parseLong(split[4]);

        Guild guild = event.getGuild();
        if (guildId == 0 && guild != null) return;
        if (guildId != 0 && guild != null && guild.getIdLong() != guildId || this.guildId != guildId) return;
        if (event.getChannel().getIdLong() != channelId || this.channelId != channelId) return;
        if (event.getMessageIdLong() != messageId || this.messageId != messageId) return;
        if (this.authorId > 0 && event.getUser().getIdLong() != authorId) {
            event.deferEdit().queue();
            return;
        }

        switch (action) {
            case "first" -> page = 0;
            case "previous" -> page--;
            case "next" -> page++;
            case "last" -> page = (contents.size() - 1) / pageSize;
        }

        event.deferEdit().queue();

        MessageEmbed embed = createEmbed();
        event.getHook().editOriginalEmbeds(embed).queue(msg -> {
            Optional<ActionRow> optional = createActionRow(guildId, channelId, messageId);
            optional.ifPresentOrElse(
                    row -> msg.editMessageComponents(row).queue(this.onMessageUpdate),
                    () -> this.onMessageUpdate.accept(msg)
            );
        });
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getGuild().getIdLong() != this.guildId) return;
        if (event.getChannel().getIdLong() != this.channelId) return;
        if (event.getMessageIdLong() != this.messageId) return;

        finish();
    }

    public static class Builder {
        private final int pageSize;
        private final List<Consumer<EmbedBuilder>> contents;

        private String title;
        private StringBuilder description;
        private String footer;
        private String footerIconUrl;
        private String thumbnail;
        private String author;
        private String authorUrl;
        private String authorIcon;
        private Color color;
        private String url;
        private String imageUrl;
        private TemporalAccessor timestamp;
        private long authorId = 0;

        public Builder(int pageSize, ContentsBuilder contentsBuilder) {
            this.pageSize = pageSize;
            this.contents = contentsBuilder.build();
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder title(String title, String url) {
            this.title = title;
            return url(url);
        }

        public Builder description(String description) {
            this.description = new StringBuilder(description);
            return this;
        }

        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        public Builder footer(String footer, String iconUrl) {
            this.footer = footer;
            return footerIcon(iconUrl);
        }

        public Builder footerIcon(String footerIconUrl) {
            this.footerIconUrl = footerIconUrl;
            return this;
        }

        public Builder thumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder author(String author, String url) {
            this.author = author;
            return authorUrl(url);
        }

        public Builder author(String author, String url, String icon) {
            this.author = author;
            return authorUrl(url).authorIcon(icon);
        }

        public Builder authorUrl(String authorUrl) {
            this.authorUrl = authorUrl;
            return this;
        }

        public Builder authorIcon(String authorIcon) {
            this.authorIcon = authorIcon;
            return this;
        }

        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        public Builder color(int color) {
            this.color = new Color(color);
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder timestamp(TemporalAccessor timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder authorOnly(long authorId) {
            this.authorId = authorId;
            return this;
        }

        public PaginatedEmbed build(JDA jda) {
            return new PaginatedEmbed(this, jda);
        }
    }

    public static class ContentsBuilder {
        private static final int FIELD_NAME_MAX = 256;
        private static final int FIELD_VALUE_MAX = 1024;
        private final List<Consumer<EmbedBuilder>> contents;

        public ContentsBuilder(List<Consumer<EmbedBuilder>> contents) {
            this.contents = contents;
        }

        public ContentsBuilder() {
            this.contents = new ArrayList<>();
        }

        public ContentsBuilder custom(Consumer<EmbedBuilder> embedBuilderConsumer) {
            this.contents.add(embedBuilderConsumer);
            return this;
        }

        public ContentsBuilder field(String name, String value, boolean inline) {
            String safeName = sanitizeFieldName(name);
            String safeValue = sanitizeFieldValue(value);
            this.contents.add(embedBuilder -> embedBuilder.addField(new MessageEmbed.Field(safeName, safeValue, inline)));
            return this;
        }

        public ContentsBuilder field(String name, String value) {
            return field(name, value, false);
        }

        public ContentsBuilder field(String name, boolean inline) {
            return field(name, ZERO_WIDTH_SPACE, inline);
        }

        public ContentsBuilder field(String name) {
            return field(name, false);
        }

        public ContentsBuilder field(boolean inline) {
            this.contents.add(embedBuilder -> embedBuilder.addField(new MessageEmbed.Field(ZERO_WIDTH_SPACE, ZERO_WIDTH_SPACE, inline)));
            return this;
        }

        private static String sanitizeFieldName(String name) {
            if (name == null || name.isBlank()) {
                return ZERO_WIDTH_SPACE;
            }

            if (name.length() > FIELD_NAME_MAX) {
                return name.substring(0, FIELD_NAME_MAX - 3) + "...";
            }

            return name;
        }

        private static String sanitizeFieldValue(String value) {
            if (value == null || value.isBlank()) {
                return ZERO_WIDTH_SPACE;
            }

            if (value.length() > FIELD_VALUE_MAX) {
                return value.substring(0, FIELD_VALUE_MAX - 3) + "...";
            }

            return value;
        }

        public List<Consumer<EmbedBuilder>> build() {
            return this.contents;
        }
    }
}
