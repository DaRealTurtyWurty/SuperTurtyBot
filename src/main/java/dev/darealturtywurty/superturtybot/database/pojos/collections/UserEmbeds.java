package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@AllArgsConstructor
public class UserEmbeds {
    public long user;
    public Map<String, String> embeds;

    public UserEmbeds() {
        this(-1, new HashMap<>());
    }

    public UserEmbeds(final long user) {
        this(user, new HashMap<>());
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
