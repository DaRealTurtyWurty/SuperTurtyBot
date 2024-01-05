package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import lombok.Setter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Setter
public class RotationConfig extends FilterConfig {
    public static final float DEFAULT_ROTATION_HZ = 5f;
    private float rotationHz = DEFAULT_ROTATION_HZ;

    public float rotationHz() {
        return rotationHz;
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public String name() {
        return "rotation";
    }

    @CheckReturnValue
    @Override
    public boolean enabled() {
        return FilterConfig.isSet(rotationHz, 5f);
    }

    @CheckReturnValue
    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new RotationPcmAudioFilter(output, format.sampleRate)
                .setRotationSpeed(rotationHz);
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public JsonObject encode() {
        final JsonObject json = new JsonObject();
        json.addProperty("rotationHz", rotationHz);
        return json;
    }

    @Override
    public void reset() {
        rotationHz = DEFAULT_ROTATION_HZ;
    }
}
