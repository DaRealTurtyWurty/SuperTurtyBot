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

        return Optional.of(EmbedBuilder.fromData(DataObject.fromJson(embed)));
    }

    public void setEmbed(String name, MessageEmbed embed) {
        this.embeds.put(name, embed.toData().toString());
    }
}
