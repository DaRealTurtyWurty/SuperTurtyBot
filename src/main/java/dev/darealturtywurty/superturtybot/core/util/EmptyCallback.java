package dev.darealturtywurty.superturtybot.core.util;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class EmptyCallback implements Callback {
    public static final EmptyCallback INSTANCE = new EmptyCallback();

    private EmptyCallback() {}

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException exception) {}

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {}
}
