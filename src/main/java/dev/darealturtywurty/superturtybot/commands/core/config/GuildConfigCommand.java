package dev.darealturtywurty.superturtybot.commands.core.config;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.text.WordUtils;
import org.bson.conversions.Bson;

import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class GuildConfigCommand extends CoreCommand {
    public GuildConfigCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("get", "Gets the current server config")
                        .addOption(OptionType.STRING, "key", "The data key to get", false, true),
                new SubcommandData("set", "Sets a value into the server config")
                        .addOption(OptionType.STRING, "key", "The data key to change", true, true)
                        .addOption(OptionType.STRING, "value", "The piece of data to assign to this key", true, true));
    }

    @Override
    public String getAccess() {
        return "Server Owner";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Sets up this server's config";
    }

    @Override
    public String getHowToUse() {
        return "/serverconfig get\n/serverconfig get [key]\n/serverconfig set [key] [value]";
    }

    @Override
    public String getName() {
        return "serverconfig";
    }

    @Override
    public String getRichName() {
        return "Server Config";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(getName())
                || !"key".equals(event.getFocusedOption().getName()))
            return;

        final String term = event.getFocusedOption().getValue();

        final List<String> keys = GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry()
                .values()
                .stream()
                .map(GuildConfigOption::getSaveName)
                .filter(key -> key.contains(term))
                .limit(25)
                .toList();
        event.replyChoiceStrings(keys).queue();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        final Guild guild = event.getGuild();
        if (guild.getDefaultChannel() == null || guild.getDefaultChannel().getType() != ChannelType.TEXT)
            return;

        final TextChannel defaultChannel = guild.getDefaultChannel().asTextChannel();

        final Bson filter = Filters.eq("guild", guild.getIdLong());
        final AtomicReference<GuildData> found = new AtomicReference<>(
                Database.getDatabase().guildData.find(filter).first());
        if (found.get() == null) {
            found.set(new GuildData(guild.getIdLong()));
            Database.getDatabase().guildData.insertOne(found.get());
        }

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());
        embed.setTitle("Config Setup");
        embed.setDescription(
                "Thank you for adding me to your server! The following are the default config settings. You can change them with `/serverconfig set [key] [value]`\n");
        embed.setColor(Color.CYAN);
        GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry()
                .values()
                .stream()
                .sorted(Comparator.comparing(GuildConfigOption::getRichName))
                .forEach(option -> embed.appendDescription(
                        "**" + option.getRichName() + "**: `" + option.getValueFromConfig().apply(found.get()) + "`\n"));
        defaultChannel.sendMessageEmbeds(embed.build()).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null || event.getUser().getIdLong() != event.getGuild().getOwnerIdLong()) {
            reply(event, "❌ You do not have permission to use this command here!", false, true);
            return;
        }

        final String subcommand = event.getSubcommandName();
        if ("get".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", null, OptionMapping::getAsString);

            final Bson filter = getFilter(event.getGuild());
            final GuildData config = get(filter, event.getGuild());

            if (key == null) {
                final var embed = new EmbedBuilder();
                GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry()
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(GuildConfigOption::getRichName))
                        .forEach(option -> {
                            final String name = option.getRichName();
                            final Object value = option.getValueFromConfig().apply(config);
                            embed.appendDescription("**" + name + "**:" + (String.valueOf(value).isBlank() ? "" : (" `" + value + "`")) + "\n");
                        });
                embed.setFooter(event.getUser().getEffectiveName(), event.getMember().getEffectiveAvatarUrl());
                embed.setColor(event.getMember().getColorRaw());
                embed.setTitle("Server Config for: " + event.getGuild().getName(), event.getGuild().getVanityUrl());
                embed.setTimestamp(Instant.now());
                embed.setThumbnail(event.getGuild().getIconUrl());
                reply(event, embed);
                return;
            }

            // Get data by the given key
            final String copyKey = key.trim();
            final Optional<GuildConfigOption> found = GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry()
                    .values()
                    .stream()
                    .filter(guildConfigOption -> guildConfigOption.getSaveName().equals(copyKey))
                    .findFirst();

            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for the server config!", false, true);
                return;
            }

            final GuildConfigOption option = found.get();
            final Object value = option.getValueFromConfig().apply(config);
            reply(event, "✅ The value assigned to `" + option.getRichName() + "` is `" + value + "`!");
        }

        if ("set".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", "", OptionMapping::getAsString);
            final String value = event.getOption("value", "", OptionMapping::getAsString);

            final Bson filter = getFilter(event.getGuild());
            final GuildData config = get(filter, event.getGuild());

            final Optional<Entry<String, GuildConfigOption>> found = GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getSaveName().equals(key))
                    .findFirst();

            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for the server config!", false, true);
                return;
            }

            final GuildConfigOption option = found.get().getValue();
            if (Boolean.FALSE.equals(option.getDataType().validator.apply(value))) {
                reply(event,
                        "❌ `" + value + "` is not the right data type! `" + option.getRichName() + "` requires a `"
                                + WordUtils.capitalize(option.getDataType().name().toLowerCase().replace("_", " ")) + "`!",
                        false, true);
                return;
            }

            if (!option.validate(event, value)) {
                reply(event, "❌ `" + value + "` is not a valid input for `" + option.getRichName() + "`!", false, true);
                return;
            }

            option.serialize(config, value);
            UpdateResult result = Database.getDatabase().guildData.replaceOne(filter, config);
            if (result.getModifiedCount() == 0) {
                reply(event, "❌ Failed to update the server config!", false, true);
                return;
            }

            reply(event, "✅ Successfully updated `" + option.getRichName() + "` to `" + option.getValueFromConfig().apply(config) + "`!");
        }
    }

    public static GuildData get(Bson filter, Guild guild) {
        GuildData found = Database.getDatabase().guildData.find(filter).first();
        if (found == null) {
            found = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(found);
        }

        return found;
    }

    public static Bson getFilter(Guild guild) {
        return Filters.eq("guild", guild.getIdLong());
    }
}