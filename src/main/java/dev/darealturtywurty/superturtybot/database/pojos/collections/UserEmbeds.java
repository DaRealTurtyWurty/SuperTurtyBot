package dev.darealturtywurty.superturtybot.database.pojos.collections;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE;

public class UserEmbeds {
    public long user;
    public Map<String, String> embeds;

    public UserEmbeds() {
        this(-1, new HashMap<>());
    }

    public UserEmbeds(final long user) {
        this(user, new HashMap<>());
    }

    public UserEmbeds(final long user, final Map<String, String> embeds) {
        this.user = user;
        this.embeds = embeds;
    }

    public long getUser() {
        return this.user;
    }

    public void setUser(final long user) {
        this.user = user;
    }

    public Map<String, String> getEmbeds() {
        return this.embeds;
    }

    public void setEmbeds(final Map<String, String> embeds) {
        this.embeds = embeds;
    }

    public void addEmbed(final String name, final String embed) {
        this.embeds.put(name, embed);
    }

    public void addEmbed(final String name, final MessageEmbed embed) {
        this.embeds.put(name, embed.toData().toString());
    }

    public void removeEmbed(final String name) {
        this.embeds.remove(name);
    }

    public Optional<EmbedBuilder> getEmbed(final String name) {
        String embed = this.embeds.get(name);
        if (embed == null) {
            return Optional.empty();
        }

        return Optional.of(fromData(DataObject.fromJson(embed)));
    }

    // TODO: Remove when https://github.com/discord-jda/JDA/pull/2471 is merged
    @NotNull
    private static EmbedBuilder fromData(@NotNull DataObject data) {
        Checks.notNull(data, "DataObject");
        var builder = new EmbedBuilder();

        builder.setTitle(data.getString("title", null));
        builder.setUrl(data.getString("url", null));
        builder.setDescription(data.getString("description", ""));
        builder.setTimestamp(data.isNull("timestamp") ? null : OffsetDateTime.parse(data.getString("timestamp")));
        builder.setColor(data.getInt("color", Role.DEFAULT_COLOR_RAW));

        data.optObject("thumbnail").ifPresent(thumbnail ->
                builder.setThumbnail(thumbnail.getString("url"))
        );

        data.optObject("author").ifPresent(author ->
                builder.setAuthor(
                        author.getString("name", ""),
                        author.getString("url", null),
                        author.getString("icon_url", null)
                )
        );

        data.optObject("footer").ifPresent(footer ->
                builder.setFooter(
                        footer.getString("text", ""),
                        footer.getString("icon_url", null)
                )
        );

        data.optObject("image").ifPresent(image ->
                builder.setImage(image.getString("url"))
        );

        data.optArray("fields").ifPresent(arr ->
                arr.stream(DataArray::getObject).forEach(field ->
                        builder.addField(
                                field.getString("name", ZERO_WIDTH_SPACE),
                                field.getString("value", ZERO_WIDTH_SPACE),
                                field.getBoolean("inline", false)
                        )
                )
        );

        return builder;
    }

    public void setEmbed(String name, MessageEmbed embed) {
        this.embeds.put(name, embed.toData().toString());
    }
}
