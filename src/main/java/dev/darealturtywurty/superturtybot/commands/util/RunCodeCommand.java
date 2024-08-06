package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RunCodeCommand extends CoreCommand {
    static {
        requestLanguages();
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
                    .setTitle(codeBlock.language().name() + " Code Execution")
                    .setDescription(result.message())
                    .setColor(result.success() ? 0x00CC00 : 0xCC0000)
                    .setTimestamp(event.getTimeCreated())
                    .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

            event.getHook().sendMessageEmbeds(embed.build()).mentionRepliedUser(false).queue();
        });
    }

    private static String buildExecutionJson(String code, String languageId) {
        JsonObject object = new JsonObject();
        object.addProperty("source", code);
        JsonObject options = new JsonObject();

        JsonObject compilerOptions = new JsonObject();
        compilerOptions.addProperty("executorRequest", true);
        options.add("compilerOptions", compilerOptions);

        JsonObject filters = new JsonObject();
        filters.addProperty("execute", true);
        options.add("filters", filters);

        JsonArray tools = new JsonArray();
        options.add("tools", tools);

        object.add("options", options);
        object.addProperty("lang", languageId);

        return Constants.GSON.toJson(object);
    }

    private static EvaluationResult evaluateCode(ProgrammingLanguage language, String code) {
        String compiler = language.compiler();
        if(Objects.equals(compiler, "custom")) {
            return language.customCompiler().apply(code);
        }
        String endpoint = "https://godbolt.org/api/compiler/" + compiler + "/compile";

        var request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(buildExecutionJson(code, language.id()), MediaType.get("application/json")))
                .addHeader("Accept", "application/json")
                .build();

        try(Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to evaluate code! Response code: {}", response.code());
                return new EvaluationResult(false, "Failed to evaluate code! Response code: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                Constants.LOGGER.error("Failed to evaluate code! Response body is null!");
                return new EvaluationResult(false, "Failed to evaluate code! Response body is null!");
            }

            String content = responseBody.string();
            if (content.isBlank() || content.contains("404 Not Found")) {
                Constants.LOGGER.error("Failed to evaluate code! Response body is empty!");
                return new EvaluationResult(false, "Failed to evaluate code! Response body is empty!");
            }

            Constants.LOGGER.debug("Got evaluation result: {}", content);

            JsonObject object = Constants.GSON.fromJson(content, JsonObject.class);
            JsonArray allStdout = object.get("stdout").getAsJsonArray();
            JsonArray allStderr = object.get("stderr").getAsJsonArray();

            var stdout = new StringBuilder();
            for (JsonElement element : allStdout) {
                stdout.append(element.getAsJsonObject().get("text").getAsString()).append("\n");
            }

            var stderr = new StringBuilder();
            for (JsonElement element : allStderr) {
                stderr.append(element.getAsJsonObject().get("text").getAsString()).append("\n");
            }

            String result = stdout.toString().trim() + "\n" + stderr.toString().trim();
            return new EvaluationResult(stderr.isEmpty(), result);
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while trying to evaluate code!", exception);
            return new EvaluationResult(false, "An error occurred while trying to evaluate code!");
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
                    if (!fileName.endsWith(language.id()))
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

        String content = message.getContentRaw();
        // look for ```language\ncontent\n```
        for (var language : ProgrammingLanguage.values()) {
            String codeBlock = "```" + language.id() + "\n";
            int start = content.indexOf(codeBlock);
            if (start == -1) continue;

            int end = content.indexOf("```", start + codeBlock.length());
            if (end == -1) continue;

            return new CodeBlock(language, CompletableFuture.completedFuture(content.substring(start + codeBlock.length(), end)));
        }

        return null;
    }

    public record EvaluationResult(boolean success, String message) {}

    public record CodeBlock(ProgrammingLanguage language, CompletableFuture<String> code) {}

    public static void requestLanguages() {
        String endpoint = "https://godbolt.org/api/languages";

        var request = new Request.Builder().url(endpoint).addHeader("Accept", "application/json").build();
        try(Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to get the languages! Response code: {}", response.code());
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                Constants.LOGGER.error("Failed to get the languages! Response body is null!");
                return;
            }

            String content = body.string();
            if (content.isBlank() || content.contains("404 Not Found")) {
                Constants.LOGGER.error("Failed to get the languages! Response body is empty!");
                return;
            }

            JsonArray array = Constants.GSON.fromJson(content, JsonArray.class);
            for (JsonElement jsonElement : array) {
                JsonObject object = jsonElement.getAsJsonObject();

                String id = object.get("id").getAsString();
                String name = object.get("name").getAsString();

                JsonArray extensions = object.get("extensions").getAsJsonArray();
                List<String> aliasList = new ArrayList<>();
                for (JsonElement extension : extensions) {
                    aliasList.add(extension.getAsString().substring(1));
                }

                List<String> compilers = requestCompilers(id);

                new ProgrammingLanguage(id, name, aliasList, compilers);
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while trying to get the languages!", exception);
        }
    }

    public static List<String> requestCompilers(String languageId) {
        String url = "https://godbolt.org/api/compilers/" + languageId;

        var request = new Request.Builder().url(url).addHeader("Accept", "application/json").build();
        try(Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to get the compilers for language {}! Response code: {}", languageId, response.code());
                return List.of();
            }

            ResponseBody body = response.body();
            if (body == null) {
                Constants.LOGGER.error("Failed to get the compilers for language {}! Response body is null!", languageId);
                return List.of();
            }

            String content = body.string();
            if (content.isBlank() || content.contains("404 Not Found")) {
                Constants.LOGGER.error("Failed to get the compilers for language {}! Response body is empty!", languageId);
                return List.of();
            }

            JsonArray array = Constants.GSON.fromJson(content, JsonArray.class);
            List<String> compilers = new ArrayList<>();
            for (JsonElement jsonElement : array) {
                JsonObject object = jsonElement.getAsJsonObject();
                compilers.add(object.get("id").getAsString());
            }

            return compilers;
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while trying to get the compilers for language {}!", languageId, exception);
            return List.of();
        }
    }

    public record ProgrammingLanguage(String id, String name, List<String> aliases, List<String> compilers) {
        private static final List<ProgrammingLanguage> VALUES = new ArrayList<>();

        private static final Map<String, String> PREFERRED_COMPILERS = new HashMap<>();
        private static final Map<String, Function<String, EvaluationResult>> CUSTOM_COMPILERS = new HashMap<>();

        static {
            PREFERRED_COMPILERS.put("csharp", "dotnet707csharp");
            PREFERRED_COMPILERS.put("fsharp", "dotnet707fsharp");
            PREFERRED_COMPILERS.put("vb", "dotnet707vb");
            PREFERRED_COMPILERS.put("cuda", "nvcc124u1");
            PREFERRED_COMPILERS.put("c", "rv64-cgcc1410");
            PREFERRED_COMPILERS.put("c++", "rv64-cgcc1410");
            PREFERRED_COMPILERS.put("fortran", "gfortran141");
            PREFERRED_COMPILERS.put("assembly", "nasm21601");
            PREFERRED_COMPILERS.put("gimple", "rv64-gimplegcc1320");
            PREFERRED_COMPILERS.put("objc++", "objcppmips64g1410");
            PREFERRED_COMPILERS.put("go", "gl1221");
            PREFERRED_COMPILERS.put("objc", "objcg141");
            PREFERRED_COMPILERS.put("android-java", "java-dex2oat-latest");
            PREFERRED_COMPILERS.put("android-kotlin", "kotlin-dex2oat-latest");
            PREFERRED_COMPILERS.put("rust", "r1800");
            PREFERRED_COMPILERS.put("circle", "circlelatest");
            PREFERRED_COMPILERS.put("circt", "circtopt-trunk");
            PREFERRED_COMPILERS.put("hlsl", "dxc_1_8_2403_2");
            PREFERRED_COMPILERS.put("cppx", "cppx_p1240r1");
            PREFERRED_COMPILERS.put("crystal", "crystal1131");
            PREFERRED_COMPILERS.put("dart", "dart322");
            PREFERRED_COMPILERS.put("erlang", "erl2416");
            PREFERRED_COMPILERS.put("carbon", "carbon-trunk");
            PREFERRED_COMPILERS.put("cobol", "gnucobol32");
            PREFERRED_COMPILERS.put("hook", "hook010");
            PREFERRED_COMPILERS.put("hylo", "hylotrunk");
            PREFERRED_COMPILERS.put("julia", "julia_1_10_0");
            PREFERRED_COMPILERS.put("tablegen", "llvmtblgen1810");
            PREFERRED_COMPILERS.put("cppx_blue", "cppx_blue_trunk");
            PREFERRED_COMPILERS.put("cppx_gold", "cppx_gold_trunk");
            PREFERRED_COMPILERS.put("mlir", "mliropt1600");
            PREFERRED_COMPILERS.put("analysis", "llvm-mcatrunk");
            PREFERRED_COMPILERS.put("python", "python312");
            PREFERRED_COMPILERS.put("racket", "racketnightly");
            PREFERRED_COMPILERS.put("ruby", "ruby334");
            PREFERRED_COMPILERS.put("ada", "gnat141");
            PREFERRED_COMPILERS.put("typescript", "tsc_0_0_35_gc");
            PREFERRED_COMPILERS.put("v", "v04");
            PREFERRED_COMPILERS.put("vala", "valac05606");
            PREFERRED_COMPILERS.put("wasm", "wasmtime2001");
            PREFERRED_COMPILERS.put("cpp_for_opencl", "armv8-full-cpp4oclclang-trunk");
            PREFERRED_COMPILERS.put("openclc", "armv8-full-oclcclang-trunk");
            PREFERRED_COMPILERS.put("c3", "c3c061");
            PREFERRED_COMPILERS.put("llvm", "llctrunk");
            PREFERRED_COMPILERS.put("cmakescript", "cmake-3_28_0");
            PREFERRED_COMPILERS.put("cpp2_cppfront", "cppfront_trunk");
            PREFERRED_COMPILERS.put("d", "ldc1_39");
            PREFERRED_COMPILERS.put("ispc", "ispc1240");
            PREFERRED_COMPILERS.put("java", "java2200");
            PREFERRED_COMPILERS.put("kotlin", "kotlinc2000");
            PREFERRED_COMPILERS.put("llvm_mir", "mirllctrunk");
            PREFERRED_COMPILERS.put("pascal", "fpc322");
            PREFERRED_COMPILERS.put("nim", "nim200");
            PREFERRED_COMPILERS.put("pony", "p0511");
            PREFERRED_COMPILERS.put("scala", "scalac2136");
            PREFERRED_COMPILERS.put("snowball", "snowballv010");
            PREFERRED_COMPILERS.put("solidity", "solc0821");
            PREFERRED_COMPILERS.put("spice", "spice02003");
            PREFERRED_COMPILERS.put("javascript", "v8trunk");
            PREFERRED_COMPILERS.put("clean", "clean30_64");
            PREFERRED_COMPILERS.put("modula2", "gm2141");
            PREFERRED_COMPILERS.put("haskell", "ghc961");
            PREFERRED_COMPILERS.put("ocaml", "ocaml5200");
            PREFERRED_COMPILERS.put("swift", "swift510");
            PREFERRED_COMPILERS.put("zig", "z0120");
            addCustomCompiler("brainfuck", "Brainf*ck", List.of(".b", ".bf"), ProgrammingLanguage::brainfuckCompiler);
        }

        private static EvaluationResult brainfuckCompiler(String code) {
            try {
                code = code.replaceAll("[^+\\-.<>\\[\\]]", "");
                final int length = 65535;
                StringBuilder resultBuilder = new StringBuilder();

                byte[] array = new byte[length];
                int index = 0;
                int c = 0;
                for(int currentChar = 0; currentChar < code.length(); currentChar++) {
                    char ch = code.charAt(currentChar);
                    switch (ch) {
                        case '>' -> {
                            if (index == length - 1)
                                index = 0;
                            else
                                index++;
                        }
                        case '<' -> {
                            if (index == 0)
                                index = length - 1;
                            else
                                index--;
                        }
                        case '+' -> array[index]++;
                        case '-' -> array[index]--;
                        case '.' -> resultBuilder.append((char)(array[index]));
                        case '[' -> {
                            if (array[index] == 0)
                            {
                                currentChar++;
                                while (c > 0 || code.charAt(currentChar) != ']')
                                {
                                    if (code.charAt(currentChar) == '[')
                                        c++;
                                    else if (code.charAt(currentChar) == ']')
                                        c--;
                                    currentChar++;
                                }
                            }
                        }
                        case ']' -> {
                            if (array[index] != 0)
                            {
                                currentChar--;
                                while (c > 0 || code.charAt(currentChar) != '[')
                                {
                                    if (code.charAt(currentChar) == ']')
                                        c ++;
                                    else if (code.charAt(currentChar) == '[')
                                        c --;
                                    currentChar--;
                                }
                            }
                        }
                        default -> {
                            return new EvaluationResult(false, "Unknown character: \"%s\" at %d, this shouldn't be possible".formatted(ch, currentChar+1));
                        }
                    }
                }
                return new EvaluationResult(true, resultBuilder.toString());
            } catch (Exception e) {
                return new EvaluationResult(false, "Something went wrong: %s".formatted(e.getMessage()));
            }
        }

        public ProgrammingLanguage {
            VALUES.add(this);
        }

        public static void addCustomCompiler(String id, String name, List<String> fileExtensions, Function<String, EvaluationResult> compiler) {
            new ProgrammingLanguage(id, name, fileExtensions, List.of("custom"));
            PREFERRED_COMPILERS.put(id, "custom");
            CUSTOM_COMPILERS.put(id, compiler);
        }

        public static ProgrammingLanguage fromString(String id) {
            for (ProgrammingLanguage language : VALUES) {
                if (language.id().equalsIgnoreCase(id)) {
                    return language;
                }
            }

            return null;
        }

        public String compiler() {
            return PREFERRED_COMPILERS.getOrDefault(id, compilers.getFirst());
        }

        public Function<String, EvaluationResult> customCompiler() {
            return CUSTOM_COMPILERS.get(id);
        }

        public static List<ProgrammingLanguage> values() {
            return VALUES;
        }
    }
}
