package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TimescaleConfig extends FilterConfig {
    public static final boolean TIMESCALE_AVAILABLE = tryLoad(TimescaleNativeLibLoader::loadTimescaleLibrary);

    private static boolean tryLoad(Runnable load) {
        try {
            load.run();
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    public static final float DEFAULT_SPEED = 1f;
    public static final float DEFAULT_PITCH = 1f;
    public static final float DEFAULT_RATE = 1f;

    private float speed = DEFAULT_SPEED;
    private float pitch = DEFAULT_PITCH;
    private float rate = DEFAULT_RATE;

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        if (speed <= 0) {
            throw new IllegalArgumentException("speed <= 0");
        }
        this.speed = speed;
    }

    public float pitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        if (pitch <= 0) {
            throw new IllegalArgumentException("pitch <= 0");
        }
        this.pitch = pitch;
    }

    public float rate() {
        return rate;
    }

    public void setRate(float rate) {
        if (rate <= 0) {
            throw new IllegalArgumentException("rate <= 0");
        }
        this.rate = rate;
    }

    @Nonnull
    @Override
    public String name() {
        return "timescale";
    }

    @Override
    public boolean enabled() {
        return TIMESCALE_AVAILABLE &&
                (FilterConfig.isSet(speed, 1f) ||
                        FilterConfig.isSet(pitch, 1f) ||
                        FilterConfig.isSet(rate, 1f));
    }

    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate)
                .setSpeed(speed)
                .setPitch(pitch)
                .setRate(rate);
    }

    @Nonnull
    @Override
    public JsonObject encode() {
        JsonObject json = new JsonObject();
        json.addProperty("speed", speed);
        json.addProperty("pitch", pitch);
        json.addProperty("rate", rate);
        return json;
    }

    @Override
    public void reset() {
        speed = DEFAULT_SPEED;
        pitch = DEFAULT_PITCH;
        rate = DEFAULT_RATE;
    }
}
