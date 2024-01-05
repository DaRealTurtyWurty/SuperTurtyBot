package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RainbowSixStatusCommand extends CoreCommand {
    private static final String R6_STATUS_URL = "https://game-status-api.ubisoft.com/v1/instances?appIds=e3d5ea9e-50bd-43b7-88bf-39794f4e3d40,fb4cc4c9-2063-461d-a1e8-84a7d36525fc,4008612d-3baf-49e4-957a-33066726a7bc";

    public RainbowSixStatusCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets the status of Rainbow Six Siege servers.";
    }

    @Override
    public String getName() {
        return "r6status";
    }

    @Override
    public String getRichName() {
        return "Rainbow Six Siege Status";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        Request request = new Request.Builder().url(R6_STATUS_URL).get().build();
        try(Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if(!response.isSuccessful()) throw new IOException("Unexpected code " + response.code());

            ResponseBody body = response.body();
            if(body == null) throw new IOException("Response body is null!");

            String bodyString = body.string();
            JsonArray array = Constants.GSON.fromJson(bodyString, JsonArray.class);
            List<GameInstance> instances = new ArrayList<>();
            for (JsonElement element : array) {
                instances.add(Constants.GSON.fromJson(element, GameInstance.class));
            }

            var embed = new EmbedBuilder();
            embed.setTitle("Rainbow Six Siege Status");
            embed.setColor(new Color(0xff930b));
            embed.setTimestamp(Instant.now());
            embed.setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
            embed.setThumbnail("https://pbs.twimg.com/media/FFjdfYeXIAMkL8e?format=jpg&name=large");

            var description = new StringBuilder("The status of Rainbow Six Siege servers.\n\n");

            for (GameInstance instance : instances) {
                String status = switch (instance.getStatus().toLowerCase(Locale.ROOT)) {
                    case "online" -> "🟢 Online";
                    case "degraded" -> "🟠 Degraded";
                    case "maintenance" -> "🟡 Maintenance";
                    case "offline" -> "🔴 Offline";
                    default -> "🔵 Unknown";
                };

                List<String> features = instance.getImpactedFeatures();
                String impactedFeatures = (features == null || features.isEmpty() ?
                                "None" :
                                "* " + String.join("\n* ", features)
                        );

                String name = instance.getName()
                        .replace("Rainbow Six Siege - ", "")
                        .replace("XBOXONE", "Xbox One")
                        .replace("PS4", "PlayStation 4");

                description.append("**").append(name).append("**\n");
                description.append("Status: ").append(status).append("\n");
                description.append("Impacted Features: \n").append(impactedFeatures).append("\n\n");
            }

            embed.setDescription(description);

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while getting the status of Rainbow Six Siege servers!").queue();
            Constants.LOGGER.error("An error occurred while getting the status of Rainbow Six Siege servers!", exception);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameInstance {
        private String AppID;
        private String MDM;
        private String SpaceID;
        private String Category;
        private String Name;
        private String Platform;
        private String Status;
        private boolean Maintenance;
        private List<String> ImpactedFeatures;
    }
}
