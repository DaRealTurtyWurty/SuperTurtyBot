package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EvalCommand extends CoreCommand {

    private static final Engine ENGINE = Engine.newBuilder("js")
            .option("js.ecmascript-version", "2022")
            .allowExperimentalOptions(true)
            .option("js.console", "true")
            .option("js.nashorn-compat", "true")
            .option("js.disable-eval", "true")
            .option("js.load", "false")
            .option("log.level", "OFF")
            .build();

    static {
        ShutdownHooks.register(ENGINE::close);
    }

    public EvalCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Evaluates the given code.";
    }

    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public String getRichName() {
        return "Eval";
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    private static ProxyObject emojiStatic() {
        return ProxyObject.fromMap(Map.of(
                "fromUnicode", function(values -> Emoji.fromUnicode(values.getFirst().asString())),
                "fromFormatted", function(values -> Emoji.fromFormatted(values.getFirst().asString()))
        ));
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if(event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId().orElse(0L)) {
            reply(event, "❌ You do not have permission to use this command!");
            return;
        }

        String content = event.getMessage().getContentRaw();
        String code = content.substring(content.indexOf(" ") + 1);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("event", event);
        bindings.put("say", function(values -> {
            if(values.isEmpty() || !values.getFirst().isString())
                return null;

            event.getChannel().sendMessage(values.getFirst().asString()).queue();
            return "delete me!";
        }));
        bindings.put("database", Database.getDatabase());

        try(Context context = createContext(bindings)) {
            Value value = context.eval("js", code);
            if(value.hasArrayElements()) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < value.getArraySize(); i++) {
                    builder.append(value.getArrayElement(i).toString()).append("\n");
                }

                reply(event, builder.toString());
                return;
            }

            if(value.toString().equals("undefined") || value.toString().equals("null")) {
                event.getMessage().addReaction(Emoji.fromUnicode("✅")).
                        queue(ignored -> event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            } else if(value.toString().equals("delete me!")) {
                event.getMessage().delete().queue();
                return;
            }

            reply(event, value.toString());
        } catch (PolyglotException exception) {
            reply(event, "❌ " + exception.getMessage());
        }
    }

    private static Context createContext(Map<String, Object> additionalBindings) {
        Context ctx = Context.newBuilder("js")
                .engine(ENGINE)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowValueSharing(true)
                .allowHostAccess(HostAccess.ALL)
                .build();

        Value bindings = ctx.getBindings("js");
        bindings.removeMember("load");
        bindings.removeMember("loadWithNewGlobal");
        bindings.removeMember("eval");
        bindings.removeMember("exit");
        bindings.removeMember("quit");
        bindings.removeMember("print");
        bindings.removeMember("console");
        for (Map.Entry<String, Object> binding : additionalBindings.entrySet()) {
            bindings.putMember(binding.getKey(), binding.getValue());
        }

        return ctx;
    }

    private static ProxyExecutable function(Function<List<Value>, Object> function) {
        return args -> function.apply(Arrays.asList(args));
    }
}
