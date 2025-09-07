package dev.darealturtywurty.superturtybot.server;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.core.BotInfoCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JsonMapper;
import io.prometheus.client.hotspot.DefaultExports;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TurtyBotServer {
    private static Javalin javalin;

    public static void init() {
        if (Environment.INSTANCE.serverPort().isEmpty())
            throw new IllegalStateException("Server port is not set in the environment variables.");

        DefaultExports.initialize();

        var gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return Constants.GSON.fromJson(json, targetType);
            }

            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return Constants.GSON.toJson(obj, type);
            }
        };

        javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.useVirtualThreads = true;
            config.jsonMapper(gsonMapper);
        });

        new TurtyBotServer().start();
    }

    private final JvmStatsService jvmStatsService = new JvmStatsService();

    private TurtyBotServer() {
    }

    public void start() {
        javalin.before("/*", ctx -> {
            if (Environment.INSTANCE.serverUsername().isEmpty() || Environment.INSTANCE.serverPassword().isEmpty()) {
                ctx.status(503).result("Server authentication is not configured.");
                return;
            }

            requireAuth(ctx);
        });

        javalin.get("/status", ctx -> ctx.result("Server is running!")); // TODO: Report some kind of info?

        javalin.get("/commands", ctx -> {
            long slashCommands = BotInfoCommand.getCommandCount(BotInfoCommand.CommandType.SLASH);
            long prefixCommands = BotInfoCommand.getCommandCount(BotInfoCommand.CommandType.PREFIX);
            long userCommands = BotInfoCommand.getCommandCount(BotInfoCommand.CommandType.USER_CONTEXT);
            long messageCommands = BotInfoCommand.getCommandCount(BotInfoCommand.CommandType.MSG_CONTEXT);

            ctx.json(Map.of(
                    "slashCommands", slashCommands,
                    "prefixCommands", prefixCommands,
                    "userContextCommands", userCommands,
                    "messageContextCommands", messageCommands
            ));
        });

        javalin.get("/uptime", ctx ->
                ctx.json(Map.of(
                        "uptime", System.currentTimeMillis() - TurtyBot.START_TIME,
                        "startTime", TurtyBot.START_TIME
                )));

        javalin.get("/server_count", ctx ->
                ctx.json(Map.of("serverCount", TurtyBot.getJDA().getGuildCache().size())));

        javalin.get("/user_count", ctx ->
                ctx.json(Map.of("userCount", TurtyBot.getJDA().getUserCache().size())));

        javalin.get("/member_count", ctx -> {
            long guildId = ctx.queryParamAsClass("guildId", Long.class).getOrDefault(0L);
            if (guildId == 0)
                ctx.json(Map.of("memberCount", TurtyBot.getJDA().getGuilds().stream()
                        .mapToLong(Guild::getMemberCount).sum()));
            else {
                var guild = TurtyBot.getJDA().getGuildById(guildId);
                if (guild == null) {
                    ctx.status(404).result("Guild not found");
                    return;
                }

                ctx.json(Map.of("memberCount", guild.getMemberCount()));
            }
        });

        javalin.get("/health", ctx -> {
            ctx.json(Map.of(
                    "status", TurtyBot.getJDA().getStatus().name(),
                    "isInit", TurtyBot.getJDA().getStatus().isInit()
            ));
        });

        javalin.get("/commands_used", ctx -> {
            long totalCommandsUsed = CommandHook.getTotalCommandsUsed();
            ctx.json(Map.of("totalCommandsUsed", totalCommandsUsed));
        });

        javalin.get("/jvm", ctx -> ctx.json(jvmStatsService.snapshot()));
        javalin.get("/jvm/memory", ctx -> ctx.json(jvmStatsService.memory()));
        javalin.get("/jvm/threads", ctx -> ctx.json(jvmStatsService.threads()));
        javalin.get("/jvm/gc", ctx -> ctx.json(jvmStatsService.gc()));
        javalin.get("/jvm/classes", ctx -> ctx.json(jvmStatsService.classes()));
        javalin.get("/jvm/process", ctx -> ctx.json(jvmStatsService.process()));

        javalin.start(Environment.INSTANCE.serverPort().orElseThrow());
    }

    private static void requireAuth(Context context) {
        String authHeader = context.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            unauthorized(context);
            return;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(authHeader.substring("Basic ".length()));
        } catch (IllegalArgumentException ignored) {
            unauthorized(context);
            return;
        }

        var credentials = new String(decoded, StandardCharsets.UTF_8);
        int colonIndex = credentials.indexOf(':');
        if (colonIndex == -1) {
            unauthorized(context);
            return;
        }

        String username = credentials.substring(0, colonIndex);
        String password = credentials.substring(colonIndex + 1);

        if (!username.equals(Environment.INSTANCE.serverUsername().orElse("")) ||
                !password.equals(Environment.INSTANCE.serverPassword().orElse(""))) {
            unauthorized(context);
            return;
        }

        context.attribute("authenticated", true);
    }

    private static void unauthorized(Context context) {
        context.header("WWW-Authenticate", "Basic realm=\"TurtyBot API\"");
        context.status(401).result("Unauthorized");
    }
}
