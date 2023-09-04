package dev.darealturtywurty.superturtybot.commands.music.manager.filter;

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChannelMixConfig extends FilterConfig {
    public static final float DEFAULT_LEFT_TO_LEFT = 1f;
    public static final float DEFAULT_LEFT_TO_RIGHT = 0f;
    public static final float DEFAULT_RIGHT_TO_LEFT = 0f;
    public static final float DEFAULT_RIGHT_TO_RIGHT = 1f;

    private float leftToLeft = DEFAULT_LEFT_TO_LEFT;
    private float leftToRight = DEFAULT_LEFT_TO_RIGHT;
    private float rightToLeft = DEFAULT_RIGHT_TO_LEFT;
    private float rightToRight = DEFAULT_RIGHT_TO_RIGHT;

    public float leftToLeft() {
        return leftToLeft;
    }

    public void setLeftToLeft(float leftToLeft) {
        this.leftToLeft = leftToLeft;
    }

    public float leftToRight() {
        return leftToRight;
    }

    public void setLeftToRight(float leftToRight) {
        this.leftToRight = leftToRight;
    }

    public float rightToLeft() {
        return rightToLeft;
    }

    public void setRightToLeft(float rightToLeft) {
        this.rightToLeft = rightToLeft;
    }

    public float rightToRight() {
        return rightToRight;
    }

    public void setRightToRight(float rightToRight) {
        this.rightToRight = rightToRight;
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public String name() {
        return "channelmix";
    }

    @CheckReturnValue
    @Override
    public boolean enabled() {
        return FilterConfig.isSet(leftToLeft,  1.0f) || FilterConfig.isSet(leftToRight,  0.0f) ||
                FilterConfig.isSet(rightToLeft, 0.0f) || FilterConfig.isSet(rightToRight, 1.0f);
    }

    @CheckReturnValue
    @Nullable
    @Override
    public AudioFilter create(AudioDataFormat format, FloatPcmAudioFilter output) {
        return new ChannelMixPcmAudioFilter(output)
                .setLeftToLeft(leftToLeft)
                .setLeftToRight(leftToRight)
                .setRightToLeft(rightToLeft)
                .setRightToRight(rightToRight);
    }

    @CheckReturnValue
    @Nonnull
    @Override
    public JsonObject encode() {
        JsonObject json = new JsonObject();
        json.addProperty("leftToLeft", leftToLeft);
        json.addProperty("leftToRight", leftToRight);
        json.addProperty("rightToLeft", rightToLeft);
        json.addProperty("rightToRight", rightToRight);
        return json;
    }

    @Override
    public void reset() {
        leftToLeft = DEFAULT_LEFT_TO_LEFT;
        leftToRight = DEFAULT_LEFT_TO_RIGHT;
        rightToLeft = DEFAULT_RIGHT_TO_LEFT;
        rightToRight = DEFAULT_RIGHT_TO_RIGHT;
    }
}