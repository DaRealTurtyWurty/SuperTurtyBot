package dev.darealturtywurty.superturtybot.commands.core.config;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UserConfigCommand extends CoreCommand {
    public UserConfigCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("get", "Gets the current user config")
                        .addOption(OptionType.STRING, "key", "The data key to get", false, true),
                new SubcommandData("set", "Sets a value into the user config")
                        .addOption(OptionType.STRING, "key", "The data key to change", true, true)
                        .addOption(OptionType.STRING, "value", "The piece of data to assign to this key", true, true));
    }


    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Sets up your user config";
    }

    @Override
    public String getHowToUse() {
        return "/userconfig get\n/userconfig get [key]\n/userconfig set [key] [value]";
    }

    @Override
    public String getName() {
        return "userconfig";
    }

    @Override
    public String getRichName() {
        return "User Config";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild() || !event.getName().equals(getName()))
            return;

        AutoCompleteQuery query = event.getFocusedOption();
        if (query.getName().equals("key")) {
            final String term = query.getValue();

            final List<String> keys = UserConfigRegistry.USER_CONFIG_OPTIONS.getRegistry()
                    .values()
                    .stream()
                    .map(UserConfigOption::getSaveName)
                    .filter(key -> key.contains(term))
                    .limit(25)
                    .toList();
            event.replyChoiceStrings(keys).queue();
        } else if (query.getName().equals("value")) {
            String key = event.getOption("key", null, OptionMapping::getAsString);
            if (key == null) {
                event.replyChoices().queue();
                return;
            }

            final Optional<UserConfigOption> found = UserConfigRegistry.USER_CONFIG_OPTIONS.getRegistry()
                    .values()
                    .stream()
                    .filter(userConfigOption -> userConfigOption.getSaveName().equalsIgnoreCase(key))
                    .findFirst();
            if (found.isEmpty()) {
                event.replyChoices().queue();
                return;
            }

            final UserConfigOption option = found.get();
            final List<Pair<String, String>> values = option.getAutoComplete().apply(query.getValue());
            if (values == null || values.isEmpty()) {
                event.replyChoices().queue();
                return;
            }

            event.replyChoices(values.stream()
                            .map(pair -> new Command.Choice(pair.getLeft(), pair.getRight()))
                            .toList())
                    .queue();
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        final String subcommand = event.getSubcommandName();
        if ("get".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", null, OptionMapping::getAsString);

            final UserConfig config = get(event.getUser());

            if (key == null) {
                // Get all data
                final Map<String, Object> configValues = UserConfigRegistry.USER_CONFIG_OPTIONS.getRegistry()
                        .values()
                        .stream()
                        .collect(Collectors.toMap(UserConfigOption::getRichName, option ->
                                option.getValueFromConfig().apply(config), (a, b) -> b));
                final var embed = new EmbedBuilder();
                configValues.forEach((name, value) -> embed.appendDescription("**" + name + "**:" + (String.valueOf(value).isBlank() ? "" : (" `" + value + "`")) + "\n"));
                embed.setFooter("For server: " + event.getGuild().getName(), event.getGuild().getIconUrl());
                embed.setColor(event.getMember().getColorRaw());
                embed.setTitle("User Config for: " + event.getUser().getEffectiveName());
                embed.setTimestamp(Instant.now());
                embed.setThumbnail(event.getMember().getEffectiveAvatarUrl());
                reply(event, embed);
                return;
            }

            // Get data by the given key
            final String copyKey = key.trim();
            final Optional<UserConfigOption> found = UserConfigRegistry.USER_CONFIG_OPTIONS.getRegistry()
                    .values()
                    .stream()
                    .filter(userConfigOption -> userConfigOption.getSaveName().equals(copyKey))
                    .findFirst();

            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for your user config!", false, true);
                return;
            }

            final UserConfigOption option = found.get();
            final Object value = option.getValueFromConfig().apply(config);
            reply(event, "✅ The value assigned to `" + option.getRichName() + "` is `" + value + "`!");
            return;
        }

        if ("set".equalsIgnoreCase(subcommand)) {
            final String key = event.getOption("key", "", OptionMapping::getAsString);
            final String value = event.getOption("value", "", OptionMapping::getAsString);

            final UserConfig config = get(event.getUser());

            final Optional<Entry<String, UserConfigOption>> found = UserConfigRegistry.USER_CONFIG_OPTIONS.getRegistry()
                    .entrySet().stream().filter(entry -> entry.getValue().getSaveName().equals(key)).findFirst();
            if (found.isEmpty()) {
                reply(event, "❌ `" + key + "` is not a valid option for your user config!", false, true);
                return;
            }

            final UserConfigOption option = found.get().getValue();
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

            event.deferReply().queue();

            option.serialize(config, value);
            final Bson update = Updates.set(option.getSaveName(), option.getValueFromConfig().apply(config));
            final UpdateResult result = Database.getDatabase().userConfig.updateOne(Filters.eq("user", event.getUser().getIdLong()), update);
            if (result.getModifiedCount() > 0) {
                event.getHook().editOriginal("✅ `" + option.getRichName() + "` has successfully been set to `" + value + "`!").mentionRepliedUser(false).queue();
                return;
            }

            event.getHook().editOriginal("❌ `" + value + "` was already the value assigned to `" + option.getRichName() + "`!").mentionRepliedUser(false).queue();
            return;
        }

        reply(event, "❌ `" + subcommand + "` is not a valid subcommand!", false, true);
    }

    private static UserConfig get(User user) {
        UserConfig found = Database.getDatabase().userConfig.find(Filters.eq("user", user.getIdLong())).first();
        if (found == null) {
            found = new UserConfig(user.getIdLong());
            Database.getDatabase().userConfig.insertOne(found);
        }

        return found;
    }
}
