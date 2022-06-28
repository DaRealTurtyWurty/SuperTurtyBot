package io.github.darealturtywurty.superturtybot.commands.core.config;

import java.awt.Color;
import java.text.DateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.Nullable;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class UserConfigCommand extends CoreCommand {
    public UserConfigCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("start", "Creates your user specific config"),
            new SubcommandData("get", "Get a value from your config.").addOption(OptionType.STRING, "option",
                "The config option to retrieve", false, true),
            new SubcommandData("set", "Set a value in your config")
                .addOption(OptionType.STRING, "option", "The config option to assign a value to", true, true)
                .addOption(OptionType.STRING, "value", "The value to assign to the prevously given key", true, true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Configures bot settings about you.";
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String function = event.getSubcommandName();
        switch (function) {
            case "start": {
                if (ConfigRetriever.CONFIGS.containsKey(event.getInteraction().getUser().getIdLong())) {
                    event.deferReply(true).setContent("❌ You already have a config created!").mentionRepliedUser(false)
                        .queue();
                    return;
                }

                ConfigRetriever.CONFIGS.put(event.getInteraction().getUser().getIdLong(), new UserConfig());
                event.deferReply(true)
                    .setContent(
                        "✅ Your config has been created! Use `/userconfig set [key] [value]` to alter your settings!")
                    .mentionRepliedUser(false).queue();
                break;
            }
            
            case "get": {
                final OptionMapping option = event.getOption("option");
                if (option == null) {
                    final EmbedBuilder embed = createUserConfigEmbed(event.getInteraction().getUser());
                    event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                    return;
                }
                
                final EmbedBuilder embed = createUserConfigEmbed(event.getInteraction().getUser(),
                    option.getAsString());
                if (embed == null) {
                    event.deferReply(true).setContent("Option: `" + option.getAsString() + "` is not a valid option!")
                        .mentionRepliedUser(false).queue();
                    return;
                }

                event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(false).queue();
                break;
            }

            case "set": {
                final String key = event.getOption("option").getAsString();
                final String value = event.getOption("value").getAsString();
                switch (key) {
                    case "locale", "language": {
                        final Locale[] locales = DateFormat.getAvailableLocales();
                        Locale found = null;
                        for (final var locale : locales) {
                            if (value.equalsIgnoreCase(locale.toString())) {
                                found = locale;
                                break;
                            }
                        }
                        
                        if (found == null) {
                            for (final var locale : locales) {
                                if (value.equalsIgnoreCase(locale.getDisplayName())) {
                                    found = locale;
                                    break;
                                }
                            }
                        }
                        
                        if (found == null) {
                            for (final var locale : locales) {
                                if (value.equalsIgnoreCase(locale.getLanguage())) {
                                    found = locale;
                                    break;
                                }
                            }
                        }
                        
                        if (found == null) {
                            for (final var locale : locales) {
                                if (value.equalsIgnoreCase(locale.getCountry())) {
                                    found = locale;
                                    break;
                                }
                            }
                        }
                        
                        if (found == null) {
                            for (final var locale : locales) {
                                if (value.equalsIgnoreCase(locale.getScript())) {
                                    found = locale;
                                    break;
                                }
                            }
                        }
                        
                        if (found == null) {
                            for (final var locale : locales) {
                                if (value.equalsIgnoreCase(locale.getVariant())) {
                                    found = locale;
                                    break;
                                }
                            }
                        }
                        
                        if (found == null) {
                            event.deferReply(true).setContent(
                                "❌ This is not a valid locale! If you want to see a list of valid locales, refer to the autocomplete that this command offers.")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        final UserConfig config = ConfigRetriever.CONFIGS
                            .computeIfAbsent(event.getInteraction().getUser().getIdLong(), id -> new UserConfig());
                        config.language = found;
                        ConfigRetriever.CONFIGS.put(event.getInteraction().getUser().getIdLong(), config);
                        event.deferReply(true)
                            .setContent("✅ Your locale has now been set to `" + config.language.toString() + "`!")
                            .mentionRepliedUser(false).queue();

                        break;
                    }

                    default: {
                        event.deferReply(true).setContent("Option: `" + key + "` is not a valid option!")
                            .mentionRepliedUser(false).queue();
                        break;
                    }
                }
            }
        }
    }
    
    private static EmbedBuilder createUserConfigEmbed(User user) {
        final var embed = new EmbedBuilder();
        embed.setTitle("Config for user: " + user.getName() + user.getDiscriminator());
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        final UserConfig config = ConfigRetriever.CONFIGS.get(user.getIdLong());
        embed.addField("Locale", config.language.toString(), false);
        return embed;
    }
    
    @Nullable
    private static EmbedBuilder createUserConfigEmbed(User user, String option) {
        final var embed = new EmbedBuilder();
        embed.setTitle("Config for user: " + user.getName() + user.getDiscriminator());
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        
        final var success = new AtomicBoolean(true);
        final UserConfig config = ConfigRetriever.CONFIGS.get(user.getIdLong());
        embed.setDescription(switch (option) {
            case "locale", "language" -> "Locale: " + config.getLanguage().getDisplayName();
            default -> {
                success.set(false);
                yield "";
            }
        });
        
        if (!success.get())
            return null;

        return embed;
    }

    public static class ConfigRetriever {
        private static final Map<Long, UserConfig> CONFIGS = new HashMap<>();
    }
}
