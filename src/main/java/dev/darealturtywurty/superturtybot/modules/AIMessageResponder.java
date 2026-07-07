package dev.darealturtywurty.superturtybot.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.helpers.ResponseAccumulator;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.*;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AIMessageResponder extends ListenerAdapter {
    public static final AIMessageResponder INSTANCE = new AIMessageResponder();

    private static final OpenAIClientAsync OPEN_AI_CLIENT;
    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();
    private static final Encoding ENCODING = ENCODING_REGISTRY.getEncodingForModel(ModelType.GPT_4O_MINI);
    private static final boolean INCLUDE_IMAGES = false;
    private static final long STREAM_EDIT_INTERVAL_MILLIS = 1000L;
    private static final int STREAM_EDIT_MIN_CHARS = 120;
    private static final int DISCORD_MESSAGE_LIMIT = 2000;
    private static final String DISCORD_CONTEXT_TOOL_NAME = "get_discord_context";
    private static final int MAX_LOCAL_TOOL_ROUNDS = 3;
    public static final String AI_PROMPT = """
            You are a fun, witty Discord bot. Be playful, enthusiastic, humorous, and occasionally a little chaotic, but always remain coherent and helpful. Match the user's tone and keep conversations feeling natural.
            
            Add a bit of excitement when it fits. Use occasional emojis to make responses feel more lively, usually no more than 1-3 per message, and skip them for serious, technical, or sensitive topics.
            
            Prefer English for responses unless the user clearly asks for another language.
            
            Keep responses concise. Simple questions should receive short, direct answers rather than long explanations. Avoid repeating yourself or explaining obvious things unless the user asks for more detail.
            
            Avoid asking follow-up questions or offering alternate formats unless they are genuinely necessary to answer the user's request. If you're unsure about something, say so instead of making things up.
            
            Use web search for current events, prices, releases, schedules, or anything time-sensitive. When the user asks for a current answer, search and answer directly instead of offering to search.
            
            When using web search, avoid repeating the same source link after every sentence or bullet. Cite a repeated source once near the relevant claim, or put a short "Source:" line at the end when the whole answer comes from one source.
            
            Resolve placeholders before answering. If a source gives an indirect label, unresolved reference, bracket entry, code, or dependency instead of the thing the user asked for, search or infer from reliable context to identify the actual thing. Only mention the placeholder if it genuinely cannot be resolved after checking.
            
            Never produce random gibberish, meaningless text, mascot-style filler, or spam. Prefer one clear, well-written message over multiple rambling paragraphs.
            
            Prefer responses that fit under Discord's 2,000-character message limit. If the answer needs to be longer, make it well-structured because it may be uploaded as a text file.
            """;

    static {
        Optional<String> openAIKey = Environment.INSTANCE.openAIKey();
        OPEN_AI_CLIENT = openAIKey.map(key -> {
                    var builder = OpenAIOkHttpClientAsync.builder()
                            .apiKey(key);
                    Environment.INSTANCE.openAIOrganizationId().ifPresent(builder::organization);
                    Environment.INSTANCE.openAIProjectId().ifPresent(builder::project);
                    return builder.build();
                })
                .orElse(null);
    }

    private final Cache<Long, List<UserChatMessage>> chatMessages = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .concurrencyLevel(4)
            .weigher((Long _, List<UserChatMessage> value) -> value.stream()
                    .mapToInt(message -> ENCODING.countTokensOrdinary(message.message().toString().replace(message.message().getClass().getSimpleName(), "")) - 2)
                    .sum())
            .maximumWeight(5_000)
            .build();

    private final Map<Long, Integer> tokensUsed = new HashMap<>(); // Map of user ID to tokens used

    public int getTokens(User user) {
        return tokensUsed.getOrDefault(user.getIdLong(), 0);
    }

    public AIMessageResponder() {
        DailyTaskScheduler.addTask(new DailyTask(() ->
                tokensUsed.forEach((userId, tokens) -> {
                    if (tokens > 0) {
                        tokensUsed.put(userId, Math.max(0, tokens - 200));
                    }
                }), 8, 0));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (OPEN_AI_CLIENT == null)
            return;

        Message message = event.getMessage();
        if (event.getAuthor().isBot() ||
                event.getAuthor().isSystem() ||
                event.isWebhookMessage() ||
                !event.isFromGuild() ||
                event.getMember() == null ||
                message.getContentRaw().length() < 10 ||
                !message.getContentRaw().contains(event.getJDA().getSelfUser().getAsMention()))
            return;

        Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        if (!config.isAiEnabled())
            return;

        long channelId = event.getChannel().getIdLong();
        List<Long> whitelistedChannels = GuildData.getLongs(config.getAiChannelWhitelist());
        if (whitelistedChannels.isEmpty() || !whitelistedChannels.contains(channelId))
            return;

        long userId = event.getAuthor().getIdLong();
        List<Long> blacklistedUsers = GuildData.getLongs(config.getAiUserBlacklist());
        if (blacklistedUsers.contains(userId) || tokensUsed.getOrDefault(userId, 0) >= 500)
            return;

        event.getChannel().sendTyping().queue();

        String content = message.getContentRaw().replace(event.getJDA().getSelfUser().getAsMention(), "");
        int tokens = ENCODING.countTokensOrdinary(content);
        if (INCLUDE_IMAGES) {
            tokens += message.getAttachments().stream()
                    .filter(Message.Attachment::isImage)
                    .mapToInt(AIMessageResponder::countTokens)
                    .sum();
        }
        tokensUsed.put(userId, tokensUsed.getOrDefault(userId, 0) + tokens);

        List<UserChatMessage> chat = chatMessages.asMap().computeIfAbsent(channelId, k -> new ArrayList<>());
        List<ResponseInputContent> contentParts = new ArrayList<>();
        contentParts.add(ResponseInputContent.ofInputText(ResponseInputText.builder()
                .text("%s: %s".formatted(
                        event.getMember().getEffectiveName().replaceAll("[^a-zA-Z0-9]", ""),
                        content))
                .build()));
        if (INCLUDE_IMAGES && !message.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                if (!attachment.isImage())
                    return;

                try {
                    InputStream stream = attachment.getProxy().download().get();
                    String base64 = Base64.getEncoder().encodeToString(stream.readAllBytes());
                    String url = "data:image/" + attachment.getFileExtension().toLowerCase(Locale.ROOT) + ";base64," + base64;
                    contentParts.add(ResponseInputContent.ofInputImage(ResponseInputImage.builder()
                            .imageUrl(url)
                            .build()));
                } catch (IOException | InterruptedException | ExecutionException exception) {
                    Constants.LOGGER.error("Failed to download attachment!", exception);
                }
            }
        }

        var chatMessage = new UserChatMessage(userId,
                ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.USER)
                        .contentOfResponseInputMessageContentList(contentParts)
                        .build()));
        chat.add(chatMessage);

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-5.4-mini")
                .instructions(AI_PROMPT)
                .input(ResponseCreateParams.Input.ofResponse(chat.stream()
                        .map(UserChatMessage::message)
                        .toList()))
                .addTool(createDiscordContextTool())
                .addTool(WebSearchTool.builder()
                        .type(WebSearchTool.Type.WEB_SEARCH)
                        .build())
                .maxToolCalls(5L)
                .reasoning(Reasoning.builder()
                        .effort(ReasoningEffort.MEDIUM)
                        .build())
                .build();

        List<Message.MentionType> allowedMentions = new ArrayList<>(TurtyBot.DEFAULT_ALLOWED_MENTIONS);
        allowedMentions.remove(Message.MentionType.USER);
        allowedMentions.remove(Message.MentionType.ROLE);

        event.getMessage().reply("Thinking...")
                .mentionRepliedUser(false)
                .setAllowedMentions(allowedMentions)
                .queue(reply -> streamResponse(event, chat, chatMessage, params, reply, allowedMentions, 0),
                        throwable -> {
                            chat.remove(chatMessage);
                            Constants.LOGGER.error("Failed to create AI placeholder response!", throwable);
                            CoreCommand.reply(event, "I'm sorry, I don't know how to respond to that.");
                        });

        chatMessages.put(channelId, chat);
    }

    private static void streamResponse(MessageReceivedEvent event, List<UserChatMessage> chat, UserChatMessage chatMessage,
                                       ResponseCreateParams params, Message reply,
                                       List<Message.MentionType> allowedMentions, int localToolRounds) {
        ResponseAccumulator accumulator = ResponseAccumulator.create();
        var responseContent = new StringBuilder();
        var lastEditTime = new AtomicLong(System.currentTimeMillis());
        var lastEditLength = new AtomicLong(0);
        var latestStatus = new AtomicReference<>("Thinking...");

        OPEN_AI_CLIENT.responses()
                .createStreaming(params)
                .subscribe(new AsyncStreamResponse.Handler<>() {
                    @Override
                    public void onNext(ResponseStreamEvent event) {
                        accumulator.accumulate(event);

                        event.webSearchCallInProgress().ifPresent(_ -> updateReply(reply, "Searching the web...", allowedMentions,
                                latestStatus, lastEditTime, lastEditLength, true));
                        event.webSearchCallSearching().ifPresent(_ -> updateReply(reply, "Searching the web...", allowedMentions,
                                latestStatus, lastEditTime, lastEditLength, true));
                        event.webSearchCallCompleted().ifPresent(_ -> updateReply(reply, "Writing answer...", allowedMentions,
                                latestStatus, lastEditTime, lastEditLength, true));

                        event.outputTextDelta().ifPresent(delta -> {
                            responseContent.append(delta.delta());
                            updateReply(reply, responseContent.toString(), allowedMentions, latestStatus, lastEditTime, lastEditLength, false);
                        });
                    }

                    @Override
                    public void onComplete(Optional<Throwable> error) {
                        if (error.isPresent()) {
                            chat.remove(chatMessage);
                            Constants.LOGGER.error("Failed to generate AI response!", error.get());
                            reply.editMessage("I'm sorry, I don't know how to respond to that.")
                                    .setAllowedMentions(allowedMentions)
                                    .queue();
                            return;
                        }

                        String finalContent = responseContent.toString();
                        Response response;
                        try {
                            response = accumulator.response();
                            if (finalContent.isBlank()) {
                                finalContent = getOutputText(response);
                            }
                        } catch (RuntimeException exception) {
                            Constants.LOGGER.error("Failed to read streamed AI response!", exception);
                            response = null;
                        }

                        boolean generatedResponse = !finalContent.isBlank();
                        if (!generatedResponse && response != null && localToolRounds < MAX_LOCAL_TOOL_ROUNDS) {
                            List<ResponseFunctionToolCall> functionCalls = response.output().stream()
                                    .map(ResponseOutputItem::functionCall)
                                    .flatMap(Optional::stream)
                                    .toList();
                            if (!functionCalls.isEmpty()) {
                                updateReply(reply, "Checking Discord context...", allowedMentions,
                                        latestStatus, lastEditTime, lastEditLength, true);
                                streamResponse(event, chat, chatMessage,
                                        appendLocalToolOutputs(params, chat, functionCalls, event),
                                        reply, allowedMentions, localToolRounds + 1);
                                return;
                            }
                        }

                        if (!generatedResponse) {
                            if (response != null) {
                                Constants.LOGGER.warn("OpenAI response had no output text. status={}, error={}, incompleteDetails={}, output={}",
                                        response.status(), response.error(), response.incompleteDetails(), response.output());
                            } else {
                                Constants.LOGGER.warn("OpenAI response stream completed without output text.");
                            }

                            finalContent = "I'm sorry, I don't know how to respond to that.";
                        }

                        if (generatedResponse) {
                            chat.add(new UserChatMessage(event.getJDA().getSelfUser().getIdLong(),
                                    ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                                            .role(EasyInputMessage.Role.ASSISTANT)
                                            .content(finalContent)
                                            .build())));
                        }

                        finishReply(reply, finalContent, allowedMentions);
                    }
                });
    }

    private static void updateReply(Message reply, String content, List<Message.MentionType> allowedMentions,
                                    AtomicReference<String> latestStatus, AtomicLong lastEditTime,
                                    AtomicLong lastEditLength, boolean force) {
        if (content.isBlank())
            return;

        String editContent = content.length() > DISCORD_MESSAGE_LIMIT
                ? "Still writing... I'll upload the full response as a text file when it's done."
                : content;

        if (Objects.equals(latestStatus.get(), editContent))
            return;

        long now = System.currentTimeMillis();
        long previousEditTime = lastEditTime.get();
        long previousEditLength = lastEditLength.get();
        if (!force && now - previousEditTime < STREAM_EDIT_INTERVAL_MILLIS &&
                Math.abs(editContent.length() - previousEditLength) < STREAM_EDIT_MIN_CHARS)
            return;

        latestStatus.set(editContent);
        lastEditTime.set(now);
        lastEditLength.set(editContent.length());
        reply.editMessage(editContent)
                .setAllowedMentions(allowedMentions)
                .queue();
    }

    private static void finishReply(Message reply, String content, List<Message.MentionType> allowedMentions) {
        if (content.length() <= DISCORD_MESSAGE_LIMIT) {
            reply.editMessage(content)
                    .setAllowedMentions(allowedMentions)
                    .queue();
            return;
        }

        FileUpload upload = FileUpload.fromData(content.getBytes(StandardCharsets.UTF_8), "ai-response.txt");
        reply.editMessage("Response was too long for Discord, so I uploaded it as a text file.")
                .setAllowedMentions(allowedMentions)
                .setFiles(upload)
                .queue();
    }

    private static String getOutputText(Response response) {
        var output = new StringBuilder();
        response.output().stream()
                .map(ResponseOutputItem::message)
                .flatMap(Optional::stream)
                .map(ResponseOutputMessage::content)
                .flatMap(Collection::stream)
                .map(ResponseOutputMessage.Content::outputText)
                .flatMap(Optional::stream)
                .map(ResponseOutputText::text)
                .forEach(output::append);

        return output.toString();
    }

    private static FunctionTool createDiscordContextTool() {
        return FunctionTool.builder()
                .name(DISCORD_CONTEXT_TOOL_NAME)
                .description("Returns the Discord context for the current message: server name, channel name, member count, member name, and current date/time.")
                .parameters(FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties", JsonValue.from(Map.of()))
                        .putAdditionalProperty("required", JsonValue.from(List.of()))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build())
                .strict(true)
                .build();
    }

    private static ResponseCreateParams appendLocalToolOutputs(ResponseCreateParams params, List<UserChatMessage> chat,
                                                               List<ResponseFunctionToolCall> functionCalls,
                                                               MessageReceivedEvent event) {
        List<ResponseInputItem> input = new ArrayList<>(chat.stream()
                .map(UserChatMessage::message)
                .toList());

        for (ResponseFunctionToolCall functionCall : functionCalls) {
            input.add(ResponseInputItem.ofFunctionCall(functionCall));
            input.add(ResponseInputItem.ofFunctionCallOutput(ResponseInputItem.FunctionCallOutput.builder()
                    .callId(functionCall.callId())
                    .output(executeLocalTool(functionCall, event))
                    .status(ResponseInputItem.FunctionCallOutput.Status.COMPLETED)
                    .build()));
        }

        return params.toBuilder()
                .input(ResponseCreateParams.Input.ofResponse(input))
                .build();
    }

    private static String executeLocalTool(ResponseFunctionToolCall functionCall, MessageReceivedEvent event) {
        if (!DISCORD_CONTEXT_TOOL_NAME.equals(functionCall.name())) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Unknown local tool: " + functionCall.name());
            return error.toString();
        }

        JsonObject context = new JsonObject();
        context.addProperty("serverName", event.getGuild().getName());
        context.addProperty("channelName", event.getChannel().getName());
        context.addProperty("memberCount", event.getGuild().getMemberCount());
        context.addProperty("memberName", event.getMember().getEffectiveName());
        context.addProperty("currentDateTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return context.toString();
    }

    private static int countTokens(Message.Attachment attachment) {
        return (int) Math.ceil(attachment.getSize() / 65536.0);
    }

    public record UserChatMessage(long userId, ResponseInputItem message) {
    }
}
