package dev.darealturtywurty.superturtybot.commands.image;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Instant;
import java.util.function.BiConsumer;

public class NasaCommand implements BiConsumer<SlashCommandInteractionEvent, ImageCommandType> {
    @Override
    public void accept(SlashCommandInteractionEvent event, ImageCommandType imageCommandType) {
        try {
            if (Environment.INSTANCE.nasaApiKey().isEmpty()) {
                Constants.LOGGER.warn("Nasa API key is not set!");
                event.reply("❌ This command has been disabled by the bot owner!").mentionRepliedUser(false).queue();
                return;
            }

            event.deferReply().queue();

            final String nasaUrl = "https://api.nasa.gov/planetary/apod?api_key=%s"
                    .formatted(Environment.INSTANCE.nasaApiKey().get());
            final Request request = new Request.Builder().url(nasaUrl).get().build();
            try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    event.getHook()
                            .sendMessage("❌ Failed to get response!")
                            .mentionRepliedUser(false)
                            .queue();
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    event.getHook()
                            .sendMessage("❌ Failed to get response!")
                            .mentionRepliedUser(false)
                            .queue();
                    return;
                }

                String bodyString = body.string();
                if (bodyString.isBlank()) {
                    event.getHook()
                            .sendMessage("❌ Failed to get response!")
                            .mentionRepliedUser(false)
                            .queue();
                    return;
                }

                NasaResponseData nasaResponseDataResponse = NasaResponseData.fromJsonString(bodyString);
                var embed = new EmbedBuilder()
                        .setTitle(nasaResponseDataResponse.getTitle())
                        .setImage(nasaResponseDataResponse.getHdurl())
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x2278c2)
                        .build();

                event.getHook()
                        .sendMessageEmbeds(embed)
                        .mentionRepliedUser(false)
                        .queue();
            }
        } catch (final IOException exception) {
            Constants.LOGGER.error("❌ Something went wrong with the NasaCommand!", exception);
            event.getHook()
                    .sendMessage("❌ Something went wrong with the NasaCommand!")
                    .mentionRepliedUser(false)
                    .queue();
        }
    }

    @Data
    public static class NasaResponseData {
        private String copyright;
        private String date;
        private String explanation;
        private String hdurl;
        private String media_type;
        private String image;
        private String service_version;
        private String title;
        private String url;

        private static NasaResponseData fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, NasaResponseData.class);
        }
    }
}
