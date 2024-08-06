package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.graalvm.polyglot.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RunCodeCommand extends CoreCommand {
    private static final Engine JAVASCRIPT_ENGINE = Engine.newBuilder("js")
            .option("js.ecmascript-version", "2022")
            .option("js.console", "true")
            .option("log.level", "OFF")
            .build();

    private static final Engine PYTHON_ENGINE = Engine.newBuilder("python")
            .option("log.level", "OFF")
            .build();

    static {
        ShutdownHooks.register(JAVASCRIPT_ENGINE::close);
    }

    public RunCodeCommand() {
        super(new Types(false, false, true, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Executes the code you provide in the specified language (as long as it is supported)! (Note: This command is still in beta)";
    }

    @Override
    public String getName() {
        return "runcode";
    }

    @Override
    public String getRichName() {
        return "Run Code";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        event.deferReply().queue();

        Message message = event.getTarget();
        CodeBlock codeBlock = findCodeBlock(message);
        if (codeBlock == null) {
            event.getHook().sendMessage("❌ No code block found!").mentionRepliedUser(false).queue();
            return;
        }

        codeBlock.code().thenAccept(codeContent -> {
            if (codeContent == null) {
                event.getHook().sendMessage("❌ Failed to read code block!").mentionRepliedUser(false).queue();
                return;
            }

            EvaluationResult result = evaluateCode(codeBlock.language(), codeContent);
            var embed = new EmbedBuilder()
                    .setTitle(codeBlock.language().getName() + " Code Execution")
                    .setDescription(result.message())
                    .setColor(result.success() ? 0x00CC00 : 0xCC0000)
                    .setTimestamp(event.getTimeCreated())
                    .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

            event.getHook().sendMessageEmbeds(embed.build()).mentionRepliedUser(false).queue();
        });
    }

    private static EvaluationResult evaluateCode(ProgrammingLanguage language, String code) {
        switch (language) {
            case JAVASCRIPT -> {
                try(Context context = Context.newBuilder()
                        .engine(JAVASCRIPT_ENGINE)
                        .allowEnvironmentAccess(EnvironmentAccess.NONE)
                        .allowValueSharing(false)
                        .allowHostAccess(HostAccess.NONE)
                        .allowHostClassLoading(false)
                        .allowHostClassLookup(str -> false)
                        .allowPolyglotAccess(PolyglotAccess.NONE)
                        .allowCreateThread(false)
                        .allowCreateProcess(false)
                        .allowNativeAccess(false)
                        .build()) {
                    try {
                        Value bindings = context.getBindings("js");
                        bindings.removeMember("load");
                        bindings.removeMember("loadWithNewGlobal");
                        bindings.removeMember("eval");
                        bindings.removeMember("exit");
                        bindings.removeMember("quit");
                        bindings.removeMember("print");
                        bindings.removeMember("console");

                        Value value = context.eval("js", code);
                        if(value.hasArrayElements()) {
                            var builder = new StringBuilder();
                            for (int i = 0; i < value.getArraySize(); i++) {
                                builder.append(value.getArrayElement(i).toString()).append("\n");
                            }

                            return new EvaluationResult(true, builder.toString());
                        }

                        return new EvaluationResult(true, value.toString());
                    } catch (PolyglotException exception) {
                        return new EvaluationResult(false, exception.getMessage());
                    }
                } catch (Exception exception) {
                    Constants.LOGGER.error("An error occurred while trying to evaluate the code!", exception);
                    return new EvaluationResult(false, "An error occurred while trying to evaluate the code!");
                }
            }
            case PYTHON -> {
                try(Context context = Context.newBuilder()
                        .engine(PYTHON_ENGINE)
                        .allowEnvironmentAccess(EnvironmentAccess.NONE)
                        .allowValueSharing(false)
                        .allowHostAccess(HostAccess.NONE)
                        .allowHostClassLoading(false)
                        .allowHostClassLookup(str -> false)
                        .allowPolyglotAccess(PolyglotAccess.NONE)
                        .allowCreateThread(false)
                        .allowCreateProcess(false)
                        .allowNativeAccess(false)
                        .build()) {
                    try {
                        Value bindings = context.getBindings("python");
                        bindings.removeMember("exit");
                        bindings.removeMember("quit");
                        bindings.removeMember("console");

                        Value value = context.eval("python", code);
                        return new EvaluationResult(true, value.toString());
                    } catch (PolyglotException exception) {
                        return new EvaluationResult(false, exception.getMessage());
                    }
                } catch (Exception exception) {
                    Constants.LOGGER.error("An error occurred while trying to evaluate the code!", exception);
                    return new EvaluationResult(false, "An error occurred while trying to evaluate the code!");
                }
            }
            default -> {
                return new EvaluationResult(false, "Unknown language!");
            }
        }
    }

    private static CodeBlock findCodeBlock(Message message) {
        List<Message.Attachment> attachments = message.getAttachments();
        if (!attachments.isEmpty()) {
            // find one with a code file
            for (Message.Attachment attachment : attachments) {
                if(attachment.isImage() || attachment.isVideo()) continue;

                String fileName = attachment.getFileExtension();
                if (fileName == null) continue;

                for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
                    for (String alias : language.getAliases()) {
                        if (!fileName.endsWith(alias))
                            continue;

                        return new CodeBlock(language, attachment.getProxy().download().thenApply(inputStream -> {
                            try {
                                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                            } catch (IOException exception) {
                                return null;
                            }
                        }));
                    }
                }
            }
        }

        String content = message.getContentRaw();
        // look for ```language\ncontent\n```
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            for (String alias : language.getAliases()) {
                String codeBlock = "```" + alias + "\n";
                int start = content.indexOf(codeBlock);
                if (start == -1) continue;

                int end = content.indexOf("```", start + codeBlock.length());
                if (end == -1) continue;

                return new CodeBlock(language, CompletableFuture.completedFuture(content.substring(start + codeBlock.length(), end)));
            }
        }

        return null;
    }

    public record EvaluationResult(boolean success, String message) {}

    public record CodeBlock(ProgrammingLanguage language, CompletableFuture<String> code) { }

    @Getter
    public enum ProgrammingLanguage {
        JAVASCRIPT("JavaScript", "javascript", "js"),
        PYTHON("Python", "python", "py");

        private final String name;
        private final String[] aliases;

        ProgrammingLanguage(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
        }
    }
}
