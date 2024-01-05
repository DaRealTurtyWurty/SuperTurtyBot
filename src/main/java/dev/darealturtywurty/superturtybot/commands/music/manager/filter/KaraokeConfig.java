package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Setter
public class KaraokeConfig extends FilterConfig {
    public static final float DEFAULT_LEVEL = 1f;
    public static final float DEFAULT_MONO_LEVEL = 1f;
    public static final float DEFAULT_FILTER_BAND = 220f;
    public static final float DEFAULT_FILTER_WIDTH = 100f;

    private float level = DEFAULT_LEVEL;
    private float monoLevel = DEFAULT_MONO_LEVEL;
    private float filterBand = DEFAULT_FILTER_BAND;
    private float filterWidth = DEFAULT_FILTER_WIDTH;

    public float level() {
        return level;
    }

    public float monoLevel() {
        return monoLevel;
    }

    public float filterBand() {
        return filterBand;
    }

    public float filterWidth() {
        return filterWidth;
    }

    @Nonnull
    @Override
    public String name() {
        return "karaoke";
    }

    @Override
    public boolean enabled() {
        return FilterConfig.isSet(level, 1f) || FilterConfig.isSet(monoLevel, 1f) ||
                FilterConfig.isSet(filterBand, 220f) || FilterConfig.isSet(filterWidth, 100f);
    }

    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new KaraokePcmAudioFilter(output, format.channelCount, format.sampleRate)
                .setLevel(level)
                .setMonoLevel(monoLevel)
                .setFilterBand(filterBand)
                .setFilterWidth(filterWidth);
    }

    @Nonnull
    @Override
    public JsonObject encode() {
        JsonObject json = new JsonObject();
        json.addProperty("level", level);
        json.addProperty("monoLevel", monoLevel);
        json.addProperty("filterBand", filterBand);
        json.addProperty("filterWidth", filterWidth);
        return json;
    }

    @Override
    public void reset() {
        level = DEFAULT_LEVEL;
        monoLevel = DEFAULT_MONO_LEVEL;
        filterBand = DEFAULT_FILTER_BAND;
        filterWidth = DEFAULT_FILTER_WIDTH;
    }
}
