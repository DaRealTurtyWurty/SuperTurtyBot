package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EqualizerConfig extends FilterConfig {
    public static final float[] DEFAULT_BANDS = new float[Equalizer.BAND_COUNT];
    private final float[] equalizerBands = DEFAULT_BANDS;

    public float getBand(int band) {
        return equalizerBands[band];
    }

    public void setBand(int band, float gain) {
        equalizerBands[band] = gain;
    }

    @Nonnull
    @Override
    public String name() {
        return "equalizer";
    }

    @Override
    public boolean enabled() {
        for (var band : equalizerBands) {
            if (FilterConfig.isSet(band, 0f)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return Equalizer.isCompatible(format) ? new Equalizer(format.channelCount, output, equalizerBands) : null;
    }

    @Nonnull
    @Override
    public JsonObject encode() {
        var array = new JsonArray();
        for (var i = 0; i < Equalizer.BAND_COUNT; i++) {
            JsonObject band = new JsonObject();
            band.addProperty("band", i);
            band.addProperty("gain", equalizerBands[i]);
            array.add(band);
        }

        var object = new JsonObject();
        object.add("bands", array);
        return object;
    }

    @Override
    public void reset() {
        System.arraycopy(DEFAULT_BANDS, 0, equalizerBands, 0, Equalizer.BAND_COUNT);
    }
}