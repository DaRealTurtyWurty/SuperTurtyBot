package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TremoloConfig extends FilterConfig {
    public static final float DEFAULT_FREQUENCY = 2f;
    public static final float DEFAULT_DEPTH = 0.5f;

    private float frequency = DEFAULT_FREQUENCY;
    private float depth = DEFAULT_DEPTH;

    public float frequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("Frequency <= 0");
        }
        this.frequency = frequency;
    }

    public float depth() {
        return depth;
    }

    public void setDepth(float depth) {
        if (depth <= 0) {
            throw new IllegalArgumentException("Depth <= 0");
        }
        if (depth > 1) {
            throw new IllegalArgumentException("Depth > 1");
        }
        this.depth = depth;
    }

    @Nonnull
    @Override
    public String name() {
        return "tremolo";
    }

    @Override
    public boolean enabled() {
        return FilterConfig.isSet(frequency, 2f) || FilterConfig.isSet(depth, 0.5f);
    }

    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new TremoloPcmAudioFilter(output, format.channelCount, format.sampleRate)
                .setFrequency(frequency)
                .setDepth(depth);
    }

    @Nonnull
    @Override
    public JsonObject encode() {
        JsonObject json = new JsonObject();
        json.addProperty("frequency", frequency);
        json.addProperty("depth", depth);
        return json;
    }

    @Override
    public void reset() {
        frequency = DEFAULT_FREQUENCY;
        depth = DEFAULT_DEPTH;
    }
}
