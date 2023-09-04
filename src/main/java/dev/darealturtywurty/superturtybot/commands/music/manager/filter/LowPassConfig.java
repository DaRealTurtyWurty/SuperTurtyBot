package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LowPassConfig extends FilterConfig {
    public static final float DEFAULT_SMOOTHING = 20f;
    private float smoothing = DEFAULT_SMOOTHING;

    public float smoothing() {
        return smoothing;
    }

    public void setSmoothing(float smoothing) {
        this.smoothing = smoothing;
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public String name() {
        return "lowpass";
    }

    @CheckReturnValue
    @Override
    public boolean enabled() {
        return FilterConfig.isSet(smoothing, 20f);
    }

    @CheckReturnValue
    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new LowPassPcmAudioFilter(output, format.channelCount, 0)
                .setSmoothing(smoothing);
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public JsonObject encode() {
        final JsonObject json = new JsonObject();
        json.addProperty("smoothing", smoothing);
        return json;
    }

    @Override
    public void reset() {
        smoothing = DEFAULT_SMOOTHING;
    }
}
