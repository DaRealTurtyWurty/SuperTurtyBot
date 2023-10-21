package dev.darealturtywurty.superturtybot.modules;

import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class HelloResponseManager extends ListenerAdapter {
    private static final List<String> RESPOND_TO =
            List.of("hello*", "hi*", "hey*", "sup", "yo*", "wassup", "hai*", "heya*", "howdy", "hola",
                    "bonjour", "greetings", "salutations", "good morning", "good afternoon", "good evening", "good night",
                    "good day", "good day to you");

    private static final List<String> RESPONSES =
            List.of("Hello!", "Hi!", "Hey!", "Sup", "Yo!", "Wassup", "Hai!", "Heya!");

    private static final Map<Long, Long> LAST_RESPONDED = new HashMap<>();

    public static final HelloResponseManager INSTANCE = new HelloResponseManager();
    private HelloResponseManager() {}

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User author = event.getAuthor();
        Message message = event.getMessage();
        if (author.isBot() ||
                author.isSystem() ||
                event.isWebhookMessage() ||
                message.getType() == MessageType.GUILD_MEMBER_JOIN)
            return;

        if(LAST_RESPONDED.getOrDefault(author.getIdLong(), 0L) >= System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
            return;

        if (shouldRespond(message)) {
            String response = RESPONSES.get((int) (Math.random() * RESPONSES.size()));
            message.reply(ThreadLocalRandom.current().nextBoolean() ?
                            author.getAsMention() + " " + response :
                            response)
                    .mentionRepliedUser(false)
                    .queue();
            LAST_RESPONDED.put(author.getIdLong(), System.currentTimeMillis());
            return;
        }

        String content = message.getContentRaw().trim().toLowerCase(Locale.ROOT);
        if (content.contains("turtybot") || content.contains("turty bot")) {
            message.addReaction(Emoji.fromFormatted("👀")).queue();
        }

        for (String word : RESPOND_TO) {
            if (content.matches(word.replace("*", ".*")) &&
                    ThreadLocalRandom.current().nextBoolean()) {
                message.addReaction(Emoji.fromFormatted("👋")).queue();
                return;
            }
        }
    }

    private static boolean shouldRespond(Message message) {
        long selfId = message.getJDA().getSelfUser().getIdLong();

        // If its replying to me
        if (message.getReferencedMessage() != null && message.getReferencedMessage().getAuthor().getIdLong() == selfId)
            return true;

        // If it mentions me
        boolean isMentioned = message.getMentions().isMentioned(message.getJDA().getSelfUser());

        String content = message.getContentRaw().trim().toLowerCase(Locale.ROOT);
        // strip away any user mentions
        for (String mention : message.getMentions().getUsers().stream().map(IMentionable::getAsMention).toList()) {
            content = content.replace(mention, "");
        }

        // strip away any roles
        for (String mention : message.getMentions().getRoles().stream().map(IMentionable::getAsMention).toList()) {
            content = content.replace(mention, "");
        }

        // strip away any channels
        for (String mention : message.getMentions().getChannels().stream().map(IMentionable::getAsMention).toList()) {
            content = content.replace(mention, "");
        }

        // strip away any emotes
        for (String mention : message.getMentions().getCustomEmojis().stream().map(IMentionable::getAsMention).toList()) {
            content = content.replace(mention, "");
        }

        // strip away any slash commands
        for (String mention : message.getMentions().getSlashCommands().stream().map(IMentionable::getAsMention).toList()) {
            content = content.replace(mention, "");
        }

        content = content.trim();

        // check if starts with 'turtybot' or 'turty bot'
        if (!isMentioned) {
            if (content.startsWith("turtybot") || content.startsWith("turty bot")) {
                content = content.replace("turtybot", "").replace("turty bot", "");
            } else {
                return false;
            }
        }



        // check if it contains any of the words in RESPOND_TO with '*' being a wildcard
        for (String word : RESPOND_TO) {
            if (content.matches(".*" + word.replace("*", ".*") + ".*"))
                return true;
        }

        return false;
    }
}
