package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.filter.*;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FilterCommand extends CoreCommand {
    private static final Map<String, Class<? extends FilterConfig>> FILTERS = Map.of(
            "channel_mix", ChannelMixConfig.class,
            "equalizer", EqualizerConfig.class,
            "karaoke", KaraokeConfig.class,
            "low_pass", LowPassConfig.class,
            "rotation", RotationConfig.class,
            "timescale", TimescaleConfig.class,
            "tremolo", TremoloConfig.class,
            "vibrato", VibratoConfig.class
    );

    public FilterCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("add", "Adds a filter to the music player.")
                        .addOptions(
                                new OptionData(OptionType.STRING, "filter", "The filter to add to the music player.", true)
                                        .addChoices(
                                                new Command.Choice("Channel Mix", "channel_mix"),
                                                new Command.Choice("Equalizer", "equalizer"),
                                                new Command.Choice("Karaoke", "karaoke"),
                                                new Command.Choice("Low Pass", "low_pass"),
                                                new Command.Choice("Rotation", "rotation"),
                                                new Command.Choice("Timescale", "timescale"),
                                                new Command.Choice("Tremolo", "tremolo"),
                                                new Command.Choice("Vibrato", "vibrato")
                                        )
                        ),
                new SubcommandData("remove", "Removes a filter from the music player.")
                        .addOption(OptionType.STRING, "filter", "The filter to remove from the music player.", true, true),
                new SubcommandData("clear", "Clears all filters from the music player."),
                new SubcommandData("list", "Lists all filters on the music player."),
                new SubcommandData("reset", "Resets the filter configuration on the music player.").addOptions(
                        new OptionData(OptionType.STRING, "filter", "The filter to reset", false)
                )
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Adds a filter to the music player.";
    }

    @Override
    public String getName() {
        return "filter";
    }

    @Override
    public String getRichName() {
        return "Filter";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel() || voiceState.getChannel() == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = voiceState.getChannel();
        if (!event.getGuild().getAudioManager().isConnected() || event.getGuild().getSelfMember()
                .getVoiceState() == null || event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }

        if (event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        FilterChainConfiguration config = AudioManager.getFilterConfiguration(event.getGuild());
        if (config == null) {
            reply(event, "❌ There was an error getting the filter configuration!", false, true);
            return;
        }

        switch (subcommand) {
            case "add" -> {
                String filter = event.getOption("filter", "N/A", OptionMapping::getAsString);
                if(!FILTERS.containsKey(filter)) {
                    reply(event, "❌ You must specify a valid filter!", false, true);
                    return;
                }

                Class<? extends FilterConfig> clazz = FILTERS.get(filter);
                if(!config.hasConfig(clazz)) {
                    try {
                        config.putConfig(clazz, clazz.getDeclaredConstructor().newInstance());
                    } catch (Exception ignored) {
                        reply(event, "❌ You must specify a valid filter!", false, true);
                        return;
                    }
                }

                FilterConfig filterConfig = config.getConfig(clazz);
                if(filterConfig == null || filterConfig.isEnabled()) {
                    reply(event, "❌ That filter is already enabled!", false, true);
                    return;
                }

                filterConfig.setEnabled(true);
                config.putConfig(clazz, filterConfig);

                reply(event, "✅ Successfully enabled the filter!");
                AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
            }

            case "remove" -> {
                String filter = event.getOption("filter", "N/A", OptionMapping::getAsString);
                if(!FILTERS.containsKey(filter)) {
                    reply(event, "❌ You must specify a valid filter!", false, true);
                    return;
                }

                Class<? extends FilterConfig> clazz = FILTERS.get(filter);
                if(!config.hasConfig(clazz)) {
                    try {
                        config.putConfig(clazz, clazz.getDeclaredConstructor().newInstance());
                    } catch (Exception ignored) {
                        reply(event, "❌ You must specify a valid filter!", false, true);
                        return;
                    }
                }

                FilterConfig filterConfig = config.getConfig(clazz);
                if(filterConfig == null || !filterConfig.isEnabled()) {
                    reply(event, "❌ That filter is already disabled!", false, true);
                    return;
                }

                filterConfig.setEnabled(false);
                reply(event, "✅ Successfully disabled the filter!");
                AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
            }

            case "clear" -> {
                config.disableAll();
                reply(event, "✅ Successfully cleared all filters!");
                AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
            }

            case "list" -> {
                Map<Class<? extends FilterConfig>, ? extends FilterConfig> filters = config.getFilters();
                if (filters.isEmpty()) {
                    reply(event, "❌ There are no filters!", false, true);
                    return;
                }

                var builder = new EmbedBuilder()
                        .setTitle("Filters in " + event.getGuild().getName())
                        .setDescription("Here are all the filters in " + event.getGuild().getName() + ".")
                        .setColor(Color.YELLOW)
                        .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

                for (Map.Entry<Class<? extends FilterConfig>, ? extends FilterConfig> entry : filters.entrySet()) {
                    AtomicReference<String> name = new AtomicReference<>("N/A");
                    FILTERS.entrySet().stream().filter(e -> e.getValue().equals(entry.getKey())).findFirst().ifPresent(e -> name.set(e.getKey()));
                    builder.addField(name.get(), "Enabled: " + entry.getValue().isEnabled(), false);
                }

                reply(event, builder);
                AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
            }

            case "reset" -> {
                String filter = event.getOption("filter", null, OptionMapping::getAsString);
                if(filter == null) {
                    config.getFilters().values().forEach(f -> {
                        f.setEnabled(false);
                        f.reset();
                    });

                    reply(event, "✅ Successfully reset all filters!");
                    return;
                }

                if(!FILTERS.containsKey(filter)) {
                    reply(event, "❌ You must specify a valid filter!", false, true);
                    return;
                }

                Class<? extends FilterConfig> clazz = FILTERS.get(filter);
                if(!config.hasConfig(clazz)) {
                    try {
                        config.putConfig(clazz, clazz.getDeclaredConstructor().newInstance());
                    } catch (Exception ignored) {
                        reply(event, "❌ You must specify a valid filter!", false, true);
                        return;
                    }
                }

                FilterConfig filterConfig = config.getConfig(clazz);
                if(filterConfig == null) {
                    reply(event, "❌ You must specify a valid filter!", false, true);
                    return;
                }

                filterConfig.setEnabled(false);
                filterConfig.reset();
                reply(event, "✅ Successfully reset the filter!");
                AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
            }
        }
    }
}
