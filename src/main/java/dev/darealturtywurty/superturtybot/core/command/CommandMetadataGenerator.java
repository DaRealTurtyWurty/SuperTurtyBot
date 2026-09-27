package dev.darealturtywurty.superturtybot.core.command;

import com.google.gson.*;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Exports the command definitions used by {@link CommandHook} without starting the bot.
 */
public final class CommandMetadataGenerator {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private CommandMetadataGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1)
            throw new IllegalArgumentException("Expected at most one output path for command metadata JSON");

        Path output = Path.of(args.length == 0 ? "commands.json" : args[0]);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(output, generate(), StandardCharsets.UTF_8);
        // Some command classes start non-daemon background workers during initialization.
        // This CLI has finished its work and should not wait for those bot runtime workers.
        System.exit(0);
    }

    private static String generate() {
        List<CoreCommand> commands = CommandHook.createCommands().stream()
                .sorted(Comparator.comparing((CoreCommand command) -> command.getCategory().getName())
                        .thenComparing(CoreCommand::getName))
                .toList();

        var root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        var entries = new JsonArray();
        for (CoreCommand command : commands) {
            entries.add(toJson(command));
        }
        root.add("commands", entries);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private static JsonObject toJson(CoreCommand command) {
        var entry = new JsonObject();
        entry.addProperty("name", command.getName());
        entry.addProperty("richName", command.getRichName());
        entry.addProperty("description", command.getDescription());
        entry.addProperty("category", command.getCategory().getName());
        entry.addProperty("access", command.getAccess());
        entry.addProperty("usage", command.getHowToUse());
        entry.addProperty("serverOnly", command.isServerOnly());

        var types = new JsonObject();
        types.addProperty("slash", command.types.slash());
        types.addProperty("prefix", command.types.normal());
        types.addProperty("messageContext", command.types.messageCtx());
        types.addProperty("userContext", command.types.userCtx());
        entry.add("types", types);

        Pair<TimeUnit, Long> rateLimit = command.getRatelimit();
        var rateLimitJson = new JsonObject();
        rateLimitJson.addProperty("duration", rateLimit.getRight());
        rateLimitJson.addProperty("unit", rateLimit.getLeft().name());
        entry.add("rateLimit", rateLimitJson);

        var registrations = new JsonArray();
        for (CommandData registration : CommandHook.createCommandData(command)) {
            registrations.add(sortObjectKeys(JsonParser.parseString(registration.toData().toString())));
        }
        entry.add("registrations", registrations);
        return entry;
    }

    private static JsonElement sortObjectKeys(JsonElement element) {
        if (element.isJsonObject()) {
            var sorted = new JsonObject();
            Map<String, JsonElement> members = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(member -> members.put(member.getKey(), member.getValue()));
            members.forEach((key, value) -> sorted.add(key, sortObjectKeys(value)));
            return sorted;
        }

        if (element.isJsonArray()) {
            var sorted = new JsonArray();
            element.getAsJsonArray().forEach(value -> sorted.add(sortObjectKeys(value)));
            return sorted;
        }

        return element;
    }
}
