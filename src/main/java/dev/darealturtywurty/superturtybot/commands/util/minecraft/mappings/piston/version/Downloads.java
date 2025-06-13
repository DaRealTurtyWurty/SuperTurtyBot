package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record Downloads(@Nullable Download client, @Nullable Download clientMappings, @Nullable Download server,
                        @Nullable Download serverMappings) {
    public static Downloads fromJson(JsonObject json) {
        Download client = null, clientMappings = null, server = null, serverMappings = null;
        if (json.has("client")) {
            client = Download.fromJson(json.getAsJsonObject("client"));
        }

        if (json.has("client_mappings")) {
            clientMappings = Download.fromJson(json.getAsJsonObject("client_mappings"));
        }

        if (json.has("server")) {
            server = Download.fromJson(json.getAsJsonObject("server"));
        }

        if (json.has("server_mappings")) {
            serverMappings = Download.fromJson(json.getAsJsonObject("server_mappings"));
        }

        return new Downloads(client, clientMappings, server, serverMappings);
    }
}
