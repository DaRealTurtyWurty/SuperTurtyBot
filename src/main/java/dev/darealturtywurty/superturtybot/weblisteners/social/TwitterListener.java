package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.google.gson.JsonObject;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import okhttp3.*;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Base64;

public class TwitterListener {
    private static TwitterApi TWITTER;

    static {
        Environment.INSTANCE.twitterBearerToken().ifPresentOrElse(
                token -> TWITTER = new TwitterApi(new TwitterCredentialsBearer(token)),
                () -> Constants.LOGGER.warn("No Twitter Bearer Token was provided!"));
    }

    private static final String JSON_RESPONSE = """
        {
            "response_token": "sha256=%s"
        }""";
    private static final String REGISTER_CALLBACK_URL = "https://api.twitter.com/1.1/account_activity/all/prod/webhooks.json?url=%s";

    @SuppressWarnings("resource")
    public static void setup() {
        if(TWITTER == null
                || Environment.INSTANCE.twitterAppId().isEmpty()
                || Environment.INSTANCE.twitterApiKey().isEmpty()
                || Environment.INSTANCE.twitterAPIKeySecret().isEmpty())
            return;

        final var javalin = Javalin.create();
        javalin.get("/webhooks/twitter", ctx -> {
            final byte[] hash = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, Environment.INSTANCE.twitterAPIKeySecret().get())
                .hmac(ctx.queryParam("crc_token"));
            final String response = JSON_RESPONSE.formatted(Base64.getEncoder().encodeToString(hash));
            Constants.LOGGER.debug(response);
            ctx.status(HttpStatus.OK).json(Constants.GSON.toJson(response, JsonObject.class));
        });
        
        javalin.start();

        Constants.HTTP_CLIENT.newCall(new Request.Builder().url(REGISTER_CALLBACK_URL.formatted("<insert server ip>"))
            .post(RequestBody.create(new byte[0])).build()).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                    throw new IllegalStateException("Sending a request to register the callback has failed!",
                        exception);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (!response.isSuccessful())
                        throw new IllegalStateException("The response from registering the callback was unsuccessful!");

                    ResponseBody body = response.body();
                    if (body == null)
                        throw new IllegalStateException("The response body from registering the callback was null!");

                    final JsonObject json = Constants.GSON.fromJson(body.string(), JsonObject.class);
                    if (!json.has("id"))
                        throw new IllegalStateException("No 'id' was provided!");

                    System.out.println(json.get("id").getAsString());
                }
            });
        
        ShutdownHooks.register(javalin::close);
    }
}
