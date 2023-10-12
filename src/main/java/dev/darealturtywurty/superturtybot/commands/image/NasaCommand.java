package dev.darealturtywurty.superturtybot.commands.image;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.OkHttpClient;
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
            final String NasaUrl = "https://api.nasa.gov/planetary/apod?api_key=%s"
                    .formatted(Environment.INSTANCE.nasaApiKey().get());

            if(Environment.INSTANCE.nasaApiKey().isEmpty()) {
                event.reply("❌ This command has been disabled by the bot owner!").mentionRepliedUser(false).queue();
                Constants.LOGGER.warn("Nasa API key is not set!");
                return;
            }

            event.deferReply().queue();
            final Request request = new Request.Builder().url(NasaUrl).get().build();

            try(Response response = new OkHttpClient().newCall(request).execute()){
                if(!response.isSuccessful()) {
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



                Nasa nasaResponse = Nasa.fromJsonString(bodyString);

                var embed = new EmbedBuilder()
                        .setTitle(nasaResponse.title)
                        .setImage(nasaResponse.hdurl)
                        .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x2278c2)
                        .build();

                event.getHook()
                        .sendMessageEmbeds(embed)
                        .mentionRepliedUser(false)
                        .queue();
            }
        }catch(final IOException exception){
            Constants.LOGGER.error("❌ Something went wrong with the NasaCommand!", exception);
            event.reply("❌ There has been an issue gathering this astronomy image.")
                    .mentionRepliedUser(false).queue();
        }
    }
    public static class Nasa{
        private String copyright;
        private String date;
        private String explanation;
        private String hdurl;
        private String media_type;
        private String image;
        private String service_version;
        private String title;
        private String url;
        private static Nasa fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, Nasa.class);
        }

    }
}
