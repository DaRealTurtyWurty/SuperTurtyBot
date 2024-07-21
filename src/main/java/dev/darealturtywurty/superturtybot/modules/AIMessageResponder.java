package dev.darealturtywurty.superturtybot.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.content.ContentPart;
import io.github.sashirestela.openai.common.tool.ToolCall;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class AIMessageResponder extends ListenerAdapter {
    public static final AIMessageResponder INSTANCE = new AIMessageResponder();

    private static final SimpleOpenAI OPEN_AI_CLIENT;
    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();
    private static final Encoding ENCODING = ENCODING_REGISTRY.getEncodingForModel(ModelType.GPT_4O_MINI);
    private static final boolean INCLUDE_IMAGES = false;

    static {
        Optional<String> openAIKey = Environment.INSTANCE.openAIKey();
        OPEN_AI_CLIENT = openAIKey.map(key ->
                        SimpleOpenAI.builder()
                                .apiKey(key)
                                .projectId(Environment.INSTANCE.openAIProjectId().orElse(null))
                                .organizationId(Environment.INSTANCE.openAIOrganizationId().orElse(null))
                                .build())
                .orElse(null);
    }

    private final Cache<Long, List<UserChatMessage>> chatMessages = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .concurrencyLevel(4)
            .weigher((Long key, List<UserChatMessage> value) -> value.stream()
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
        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(config);
            return;
        }

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
        List<ContentPart> contentParts = new ArrayList<>();
        contentParts.add(ContentPart.ContentPartText.of(content));
        if (INCLUDE_IMAGES && !message.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                if (!attachment.isImage())
                    return;

                try {
                    InputStream stream = attachment.getProxy().download().get();
                    String base64 = Base64.getEncoder().encodeToString(stream.readAllBytes());
                    contentParts.add(ContentPart.ContentPartImageUrl.of(ContentPart.ContentPartImageUrl.ImageUrl.of("data:image/" + attachment.getFileExtension() + ";base64," + base64)));
                } catch (IOException | InterruptedException | ExecutionException exception) {
                    Constants.LOGGER.error("Failed to download attachment!", exception);
                }
            }
        }

        var chatMessage = new UserChatMessage(userId,
                ChatMessage.UserMessage.of(
                        contentParts,
                        event.getMember().getEffectiveName().replaceAll("[^a-zA-Z0-9]", "")));
        chat.add(chatMessage);

        OPEN_AI_CLIENT.chatCompletions()
                .createStream(ChatRequest.builder()
                        .model("gpt-4o-mini")
                        .message(ChatMessage.SystemMessage.of("Act as a fun discord bot, you can be as silly and playful as you want. Avoid saying too much for simple questions. Avoid asking questions and do not go over the character limit (4000 chars). Do not respond with random gibberish."))
                        .message(ChatMessage.SystemMessage.of(createArgs(event).toString()))
                        .messages(chat.stream()
                                .map(UserChatMessage::message)
                                .toList())
                        .temperature(1.2)
                        .maxTokens(300)
                        .build())
                .thenAccept(chatStream -> {
                    var args = new StringBuilder();
                    Chat.Choice response = getResponse(chatStream, args);
                    String responseContent = response.getMessage() != null ?
                            response.getMessage().getContent() :
                            "I'm sorry, I don't know how to respond to that.";
                    chat.add(new UserChatMessage(event.getJDA().getSelfUser().getIdLong(), ChatMessage.AssistantMessage.of(responseContent)));

                    List<Message.MentionType> allowedMentions = new ArrayList<>(TurtyBot.DEFAULT_ALLOWED_MENTIONS);
                    allowedMentions.remove(Message.MentionType.USER);
                    allowedMentions.remove(Message.MentionType.ROLE);
                    event.getMessage().reply(responseContent).mentionRepliedUser(false).setAllowedMentions(allowedMentions).queue();
                }).exceptionally(throwable -> {
                    chat.remove(chatMessage);
                    CoreCommand.reply(event, "I'm sorry, I don't know how to respond to that.");
                    return null;
                });

        chatMessages.put(channelId, chat);
    }

    private Chat.Choice getResponse(Stream<Chat> chatStream, StringBuilder args) {
        var choice = new Chat.Choice();
        choice.setIndex(0);
        var chatMsgResponse = new ChatMessage.ResponseMessage();
        List<ToolCall> toolCalls = new ArrayList<>();

        var indexTool = new AtomicInteger(-1);
        var content = new StringBuilder();
        chatStream.forEach(responseChunk -> {
            List<Chat.Choice> choices = responseChunk.getChoices();
            if (!choices.isEmpty()) {
                Chat.Choice innerChoice = choices.getFirst();
                ChatMessage.ResponseMessage delta = innerChoice.getMessage();
                if (delta.getRole() != null) {
                    chatMsgResponse.setRole(delta.getRole());
                } else if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                    content.append(delta.getContent());
                } else if (delta.getToolCalls() != null) {
                    ToolCall toolCall = delta.getToolCalls().getFirst();
                    if (toolCall.getIndex() != indexTool.get()) {
                        if (!toolCalls.isEmpty()) {
                            toolCalls.getLast().getFunction().setArguments(args.toString());
                        }

                        toolCalls.add(toolCall);
                        indexTool.getAndIncrement();
                    } else {
                        args.append(toolCall.getFunction().getArguments());
                    }
                } else {
                    if (!content.isEmpty()) {
                        chatMsgResponse.setContent(content.toString());
                    }

                    if (!toolCalls.isEmpty()) {
                        toolCalls.getLast().getFunction().setArguments(args.toString());
                        chatMsgResponse.setToolCalls(toolCalls);
                    }

                    choice.setMessage(chatMsgResponse);
                    choice.setFinishReason(innerChoice.getFinishReason());
                }
            }
        });

        return choice;
    }

    private static StringBuilder createArgs(MessageReceivedEvent event) {
        return new StringBuilder()
                .append("serverName is:").append(event.getGuild().getName()).append(",")
                .append("channelName is:").append(event.getChannel().getName()).append(",")
                .append("memberCount is:").append(event.getGuild().getMemberCount()).append(",")
                .append("memberName is:").append(event.getMember().getEffectiveName()).append(",");
    }

    // 512x512 = 40 tokens
    private static int countTokens(Message.Attachment attachment) {
        int width = attachment.getWidth();
        int height = attachment.getHeight();

        Constants.LOGGER.debug("Tokens: {}", (width * height) / 512);
        return (width * height) / 512;
    }

    public record UserChatMessage(long userId, ChatMessage message) {
    }
}
