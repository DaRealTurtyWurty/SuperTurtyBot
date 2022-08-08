package io.github.darealturtywurty.superturtybot.commands.core.config;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.text.WordUtils;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class ServerConfigCommand extends CoreCommand {
    public ServerConfigCommand() {
        super(new Types(true, false, false, false));
    }

    //@formatter:off
    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("get", "Gets the current server config")
                .addOption(OptionType.STRING, "key","The data key to get", false, true),
            new SubcommandData("set", "Sets a value into the server config")
                .addOption(OptionType.STRING, "key", "The data key to change", true, true)
                .addOption(OptionType.STRING, "value", "The piece of data to assign to this key", true, true));
    }
    //@formatter:on

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
        if (!event.isFromGuild() || !event.getName().equals(getName()))
            return;
        
        final String term = event.getFocusedOption().getValue();
        
        final List<String> keys = ServerConfigRegistry.SERVER_CONFIG_OPTIONS.getRegistry().entrySet().stream()
            .map(Entry::getValue).map(ServerConfigOption::getSaveName).filter(key -> key.contains(term)).limit(25)
            .toList();
        event.replyChoiceStrings(keys).queue();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        final Guild guild = event.getGuild();
        final TextChannel defaultChannel = guild.getDefaultChannel().asTextChannel();

        final Bson filter = Filters.eq("guild", guild.getIdLong());
        final AtomicReference<GuildConfig> found = new AtomicReference<>(
            Database.getDatabase().guildConfig.find(filter).first());
        if (found.get() == null) {
            found.set(new GuildConfig(guild.getIdLong()));
            Database.getDatabase().guildConfig.insertOne(found.get());
        }

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());
        embed.setTitle("Config Setup");
        embed.setDescription(
            "Thank you for adding me to your server! The following are the default config settings. You can change them with `/serverconfig set [key] [value]`\n");
        embed.setColor(Color.CYAN);
        final List<ServerConfigOption> keys = ServerConfigRegistry.SERVER_CONFIG_OPTIONS.getRegistry().entrySet()
            .stream().map(Entry::getValue).toList();
        keys.forEach(option -> embed.appendDescription(
            "**" + option.getRichName() + "**: `" + option.getValueFromConfig().apply(found.get()) + "`\n"));
        defaultChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getUser().getIdLong() != event.getGuild().getOwnerIdLong())
            return;

        final String subcommand = event.getSubcommandName();
        if ("get".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", null, OptionMapping::getAsString);

            final Bson filter = getFilter(event.getGuild());
            final GuildConfig config = get(filter, event.getGuild());

            if (key == null) {
                // Get all data
                final Map<String, Object> configValues = new HashMap<>();
                ServerConfigRegistry.SERVER_CONFIG_OPTIONS.getRegistry().entrySet().stream().map(Entry::getValue)
                    .forEach(
                        option -> configValues.put(option.getRichName(), option.getValueFromConfig().apply(config)));
                final var embed = new EmbedBuilder();
                configValues.forEach((name, value) -> embed.appendDescription("**" + name + "**: `" + value + "`\n"));
                embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                    event.getMember().getEffectiveAvatarUrl());
                embed.setColor(event.getMember().getColorRaw());
                embed.setTitle("Server Config for: " + event.getGuild().getName(), event.getGuild().getVanityUrl());
                embed.setTimestamp(Instant.now());
                embed.setThumbnail(event.getGuild().getIconUrl());
                reply(event, embed);
                return;
            }

            // Get data by the given key
            final String copyKey = key.trim();
            final Optional<ServerConfigOption> found = ServerConfigRegistry.SERVER_CONFIG_OPTIONS.getRegistry()
                .entrySet().stream().filter(entry -> entry.getValue().getSaveName().equals(copyKey))
                .map(Entry::getValue).findFirst();
            
            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for the server config!", false, true);
                return;
            }
            
            final ServerConfigOption option = found.get();
            final Object value = option.getValueFromConfig().apply(config);
            reply(event, "✅ The value assigned to `" + option.getRichName() + "` is `" + value + "`!");
        }

        if ("set".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", "", OptionMapping::getAsString);
            final String value = event.getOption("value", "", OptionMapping::getAsString);

            final Bson filter = getFilter(event.getGuild());
            final GuildConfig config = get(filter, event.getGuild());
            
            final Optional<Entry<String, ServerConfigOption>> found = ServerConfigRegistry.SERVER_CONFIG_OPTIONS
                .getRegistry().entrySet().stream().filter(entry -> entry.getValue().getSaveName().equals(key))
                .findFirst();
            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for the server config!", false, true);
                return;
            }

            final ServerConfigOption option = found.get().getValue();
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
            final Bson update = Updates.set(option.getSaveName(), option.getValueFromConfig().apply(config));
            final UpdateResult result = Database.getDatabase().guildConfig.updateOne(filter, update);
            if (result.getModifiedCount() > 0) {
                reply(event, "✅ `" + option.getRichName() + "` has successfully been set to `" + value + "`!");
                return;
            }

            reply(event, "❌ `" + value + "` was already the value assigned to `" + option.getRichName() + "`!", false,
                true);
        }
    }

    public static GuildConfig get(Bson filter, Guild guild) {
        GuildConfig found = Database.getDatabase().guildConfig.find(filter).first();
        if (found == null) {
            found = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(found);
        }

        return found;
    }

    public static Bson getFilter(Guild guild) {
        return Filters.eq("guild", guild.getIdLong());
    }
}