package dev.darealturtywurty.superturtybot.weblisteners.social;

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import com.google.gson.JsonObject;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TwitterListener {
    private static final TwitterApi TWITTER = new TwitterApi(
        new TwitterCredentialsBearer(Environment.INSTANCE.twitterBearerToken()));
    private static final String JSON_RESPONSE = """
        {
            "response_token": "sha256=%s"
        }""";
    private static final String REGISTER_CALLBACK_URL = "https://api.twitter.com/1.1/account_activity/all/prod/webhooks.json?url=%s";
    
    public static void setup() {
        final var javalin = Javalin.create();
        javalin.get("/webhooks/twitter", ctx -> {
            final byte[] hash = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, Environment.INSTANCE.twitterAPIKeySecret())
                .hmac(ctx.queryParam("crc_token"));
            final String response = JSON_RESPONSE.formatted(Base64.getEncoder().encodeToString(hash));
            Constants.LOGGER.debug(response);
            ctx.status(HttpStatus.OK).json(Constants.GSON.toJson(response, JsonObject.class));
        });
        
        javalin.start();

        Constants.HTTP_CLIENT.newCall(new Request.Builder().url(REGISTER_CALLBACK_URL.formatted("<insert server ip>"))
            .post(RequestBody.create(new byte[0])).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException exception) {
                    throw new IllegalStateException("Sending a request to register the callback has failed!",
                        exception);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful())
                        throw new IllegalStateException("The response from registering the callback was unsuccessful!");

                    final JsonObject json = Constants.GSON.fromJson(response.body().string(), JsonObject.class);
                    if (!json.has("id"))
                        throw new IllegalStateException("No 'id' was provided!");

                    System.out.println(json.get("id").getAsString());
                }
            });
        
        ShutdownHooks.register(javalin::close);
    }
}
