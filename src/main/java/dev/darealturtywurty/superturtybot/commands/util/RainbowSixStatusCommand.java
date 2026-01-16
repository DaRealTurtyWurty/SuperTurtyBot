package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.javalin.http.ContentType;
import lombok.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RainbowSixStatusCommand extends CoreCommand {
    private static final String R6_STATUS_URL = "https://public-ubiservices.ubi.com/v1/applications/gameStatuses?applicationIds=e3d5ea9e-50bd-43b7-88bf-39794f4e3d40,fb4cc4c9-2063-461d-a1e8-84a7d36525fc,4008612d-3baf-49e4-957a-33066726a7bc,6e3c99c9-6c3f-43f4-b4f6-f1a3143f2764,76f580d5-7f50-47cc-bbc1-152d000bfe59";

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

        HttpUrl url = HttpUrl.parse(R6_STATUS_URL);
        if (url == null) {
            event.getHook().sendMessage("❌ An error occurred while building the request URL!").queue();
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Ubi-AppId", "39baebad-39e5-4552-8c25-2c9b919064e2")
                .addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response.code());

            ResponseBody body = response.body();
            if (body == null) throw new IOException("Response body is null!");

            String bodyString = body.string();
            JsonArray gameStatusesJson = Constants.GSON.fromJson(bodyString, JsonObject.class)
                    .getAsJsonArray("gameStatuses");
            List<GameStatus> statuses = new ArrayList<>();
            for (JsonElement status : gameStatusesJson) {
                statuses.add(Constants.GSON.fromJson(status, GameStatus.class));
            }

            var embed = new EmbedBuilder();
            embed.setTitle("Rainbow Six Siege Status");
            embed.setTimestamp(Instant.now());
            embed.setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

            var description = new StringBuilder("The status of Rainbow Six Siege servers.\n\n");

            StatusType worstStatus = StatusType.ONLINE;
            for (GameStatus status : statuses) {
                PlatformType platformType = PlatformType.fromId(status.getPlatformType());
                if (platformType == null)
                    continue;

                StatusType statusType = StatusType.fromStatus(status.getStatus());
                if (statusType == null) {
                    if(status.isMaintenance()) {
                        statusType = StatusType.MAINTENANCE;
                    } else {
                        continue;
                    }
                }

                if (statusType.ordinal() > worstStatus.ordinal()) {
                    worstStatus = statusType;
                }

                description.append("**")
                        .append(platformType.getDisplayName())
                        .append("**: ")
                        .append(statusType.getEmoji())
                        .append(" ")
                        .append(statusType.getDisplayName())
                        .append("\n");

                if (status.getImpactedFeatures().length > 0) {
                    description.append("_Impacted Features: ");
                    for (int i = 0; i < status.getImpactedFeatures().length; i++) {
                        description.append(status.getImpactedFeatures()[i]);
                        if (i < status.getImpactedFeatures().length - 1) {
                            description.append(", ");
                        }
                    }

                    description.append("_\n");
                }
            }

            embed.setDescription(description);
            embed.setColor(switch (worstStatus) {
                case ONLINE -> 0x00DD00;
                case DEGRADED -> 0xEEDD00;
                case MAINTENANCE -> 0xFFA500;
                case OFFLINE -> 0xDD0000;
            });

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while getting the status of Rainbow Six Siege servers!").queue();
            Constants.LOGGER.error("An error occurred while getting the status of Rainbow Six Siege servers!", exception);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameStatus {
        private String applicationId;
        private String spaceId;
        private String name;
        private String platformType;
        private String status;
        private boolean isMaintenance;
        private String[] impactedFeatures;
    }

    @Getter
    @RequiredArgsConstructor
    public enum PlatformType {
        PC("PC", "PC"),
        PS4("ORBIS", "PS4"),
        PS5("PS5", "PS5"),
        XBOX_SERIES_XS("XboxScarlett", "Xbox Series X|S"),
        XBOX_ONE("DURANGO", "Xbox One");

        private final String id;
        private final String displayName;

        public static PlatformType fromId(String id) {
            for (PlatformType type : values()) {
                if (type.id.equalsIgnoreCase(id))
                    return type;
            }

            return null;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum StatusType {
        ONLINE("🟢", "Online"),
        DEGRADED("🟠", "Degraded Performance"),
        MAINTENANCE("🟣", "Maintenance"),
        OFFLINE("🔴", "Offline");

        private final String emoji;
        private final String displayName;

        public static StatusType fromStatus(String status) {
            for (StatusType type : values()) {
                if (type.name().equalsIgnoreCase(status))
                    return type;
            }

            return null;
        }
    }
}
