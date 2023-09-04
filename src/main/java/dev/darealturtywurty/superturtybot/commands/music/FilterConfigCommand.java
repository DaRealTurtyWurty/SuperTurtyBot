package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.filter.*;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class FilterConfigCommand extends CoreCommand {
    public FilterConfigCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("channel_mix", "Sets the channel mix of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "left_to_left", "The left to left value.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "left_to_right", "The left to right value.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "right_to_left", "The right to left value.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "right_to_right", "The right to right value.").setMinValue(0)
                ),
                new SubcommandData("equalizer", "Sets the equalizer of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "band", "The band to set.").setRequiredRange(0, 15),
                        new OptionData(OptionType.NUMBER, "gain", "The gain to set.")
                ),
                new SubcommandData("karaoke", "Sets the karaoke of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "level", "The level to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "mono_level", "The mono level to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "filter_band", "The filter band to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "filter_width", "The filter width to set.").setMinValue(0)
                ),
                new SubcommandData("low_pass", "Sets the low pass of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "smoothing", "The smoothing to set.").setMinValue(0)
                ),
                new SubcommandData("rotation", "Sets the rotation of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "rotation_hz", "The rotation hz to set.").setMinValue(0)
                ),
                new SubcommandData("timescale", "Sets the timescale of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "speed", "The speed to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "pitch", "The pitch to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "rate", "The rate to set.").setMinValue(0)
                ),
                new SubcommandData("tremolo", "Sets the tremolo of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "frequency", "The frequency to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "depth", "The depth to set.").setMinValue(0)
                ),
                new SubcommandData("vibrato", "Sets the vibrato of the player.").addOptions(
                        new OptionData(OptionType.NUMBER, "frequency", "The frequency to set.").setMinValue(0),
                        new OptionData(OptionType.NUMBER, "depth", "The depth to set.").setMinValue(0)
                )
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Configures the filters of the player.";
    }

    @Override
    public String getName() {
        return "filter_config";
    }

    @Override
    public String getRichName() {
        return "Filter Config";
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

        switch (subcommand) {
            case "channel_mix" -> {
                ChannelMixConfig channelMix = config.getConfig(ChannelMixConfig.class);
                if (channelMix == null) {
                    reply(event, "❌ The channel mix filter is not enabled!", false, true);
                    return;
                }

                float leftToLeft = event.getOption("left_to_left", channelMix.leftToLeft(), OptionMapping::getAsDouble).floatValue();
                float leftToRight = event.getOption("left_to_right", channelMix.leftToRight(), OptionMapping::getAsDouble).floatValue();
                float rightToLeft = event.getOption("right_to_left", channelMix.rightToLeft(), OptionMapping::getAsDouble).floatValue();
                float rightToRight = event.getOption("right_to_right", channelMix.rightToRight(), OptionMapping::getAsDouble).floatValue();
                channelMix.setLeftToLeft(leftToLeft);
                channelMix.setLeftToRight(leftToRight);
                channelMix.setRightToLeft(rightToLeft);
                channelMix.setRightToRight(rightToRight);

                config.putConfig(ChannelMixConfig.class, channelMix);

                reply(event, "✅ Successfully set the channel mix!");
            }

            case "equalizer" -> {
                EqualizerConfig equalizer = config.getConfig(EqualizerConfig.class);
                if (equalizer == null) {
                    reply(event, "❌ The equalizer filter is not enabled!", false, true);
                    return;
                }

                int band = event.getOption("band", 0, OptionMapping::getAsInt);
                float gain = event.getOption("gain", equalizer.getBand(band), OptionMapping::getAsDouble).floatValue();
                equalizer.setBand(band, gain);

                config.putConfig(EqualizerConfig.class, equalizer);

                reply(event, "✅ Successfully set the equalizer!");
            }

            case "karaoke" -> {
                KaraokeConfig karaoke = config.getConfig(KaraokeConfig.class);
                if (karaoke == null) {
                    reply(event, "❌ The karaoke filter is not enabled!", false, true);
                    return;
                }

                float level = event.getOption("level", karaoke.level(), OptionMapping::getAsDouble).floatValue();
                float monoLevel = event.getOption("mono_level", karaoke.monoLevel(), OptionMapping::getAsDouble).floatValue();
                float filterBand = event.getOption("filter_band", karaoke.filterBand(), OptionMapping::getAsDouble).floatValue();
                float filterWidth = event.getOption("filter_width", karaoke.filterWidth(), OptionMapping::getAsDouble).floatValue();
                karaoke.setLevel(level);
                karaoke.setMonoLevel(monoLevel);
                karaoke.setFilterBand(filterBand);
                karaoke.setFilterWidth(filterWidth);

                config.putConfig(KaraokeConfig.class, karaoke);

                reply(event, "✅ Successfully set the karaoke!");
            }

            case "low_pass" -> {
                LowPassConfig lowPass = config.getConfig(LowPassConfig.class);
                if (lowPass == null) {
                    reply(event, "❌ The low pass filter is not enabled!", false, true);
                    return;
                }

                float smoothing = event.getOption("smoothing", lowPass.smoothing(), OptionMapping::getAsDouble).floatValue();
                lowPass.setSmoothing(smoothing);

                config.putConfig(LowPassConfig.class, lowPass);

                reply(event, "✅ Successfully set the low pass!");
            }

            case "rotation" -> {
                RotationConfig rotation = config.getConfig(RotationConfig.class);
                if (rotation == null) {
                    reply(event, "❌ The rotation filter is not enabled!", false, true);
                    return;
                }

                float rotationHz = event.getOption("rotation_hz", rotation.rotationHz(), OptionMapping::getAsDouble).floatValue();
                rotation.setRotationHz(rotationHz);

                config.putConfig(RotationConfig.class, rotation);

                reply(event, "✅ Successfully set the rotation!");
            }

            case "timescale" -> {
                TimescaleConfig timescale = config.getConfig(TimescaleConfig.class);
                if (timescale == null) {
                    reply(event, "❌ The timescale filter is not enabled!", false, true);
                    return;
                }

                float speed = event.getOption("speed", timescale.speed(), OptionMapping::getAsDouble).floatValue();
                float pitch = event.getOption("pitch", timescale.pitch(), OptionMapping::getAsDouble).floatValue();
                float rate = event.getOption("rate", timescale.rate(), OptionMapping::getAsDouble).floatValue();
                timescale.setSpeed(speed);
                timescale.setPitch(pitch);
                timescale.setRate(rate);

                config.putConfig(TimescaleConfig.class, timescale);

                reply(event, "✅ Successfully set the timescale!");
            }

            case "tremolo" -> {
                TremoloConfig tremolo = config.getConfig(TremoloConfig.class);
                if (tremolo == null) {
                    reply(event, "❌ The tremolo filter is not enabled!", false, true);
                    return;
                }

                float frequency = event.getOption("frequency", tremolo.frequency(), OptionMapping::getAsDouble).floatValue();
                float depth = event.getOption("depth", tremolo.depth(), OptionMapping::getAsDouble).floatValue();
                tremolo.setFrequency(frequency);
                tremolo.setDepth(depth);

                config.putConfig(TremoloConfig.class, tremolo);

                reply(event, "✅ Successfully set the tremolo!");
            }

            case "vibrato" -> {
                VibratoConfig vibrato = config.getConfig(VibratoConfig.class);
                if (vibrato == null) {
                    reply(event, "❌ The vibrato filter is not enabled!", false, true);
                    return;
                }

                float frequency = event.getOption("frequency", vibrato.frequency(), OptionMapping::getAsDouble).floatValue();
                float depth = event.getOption("depth", vibrato.depth(), OptionMapping::getAsDouble).floatValue();
                vibrato.setFrequency(frequency);
                vibrato.setDepth(depth);

                config.putConfig(VibratoConfig.class, vibrato);

                reply(event, "✅ Successfully set the vibrato!");
            }

            default -> reply(event, "❌ Unknown subcommand!", false, true);
        }

        AudioManager.getOrCreate(event.getGuild()).getPlayer().setFilterFactory(config.factory());
    }
}
