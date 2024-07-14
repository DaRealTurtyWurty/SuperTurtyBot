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
import java.util.regex.Pattern;

public class HelloResponseManager extends ListenerAdapter {
    private static final List<String> RESPOND_TO =
            List.of("hello+", "hi+", "hey+", "(s)up+", "yo+", "wassup+",
                    "hai+", "heya*", "howdy+", "hola+", "bonjour", "greetings", "(s)alutations", "good morning",
                    "good afternoon", "good evening", "good night", "good day", "good day to you");

    private static final List<Pattern> COMPILED_RESPOND_TO =
            RESPOND_TO.stream().map(s -> "\\b" + s + "\\b").map(Pattern::compile).toList();

    private static final List<String> RESPONSES =
            List.of("Hello!", "Hi!", "Hey!", "Sup", "Yo!", "Wassup", "Hai!", "Heya!");

    private static final List<String> EMOJIS =
            List.of("👋", "😉", "😜", "😳", "🙏", "😘", "😊", "😅", "😀", "😗", "🤗", "🫡", "😶‍🌫️", "😌",
                    "😝", "🙃", "😭", "😵", "🥺", "🥹", "🫣", "🫢", "🤭", "🤫", "🫨", "😈", "😸", "😼", "🙀", "😹");

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

        if(LAST_RESPONDED.getOrDefault(author.getIdLong(), 0L) >= System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))
            return;

        if (shouldRespond(message)) {
            message.getChannel().sendTyping().queue();
            String response = RESPONSES.get((int) (Math.random() * RESPONSES.size()));

            // spread emojis
            if (ThreadLocalRandom.current().nextBoolean())
                response += " " + EMOJIS.get((int) (Math.random() * EMOJIS.size()));

            if (ThreadLocalRandom.current().nextBoolean())
                response = EMOJIS.get((int) (Math.random() * EMOJIS.size())) + " " + response;

            if (ThreadLocalRandom.current().nextBoolean())
                response = author.getAsMention() + " " + response;

            message.reply(response).mentionRepliedUser(false).queueAfter(3, TimeUnit.SECONDS);
            LAST_RESPONDED.put(author.getIdLong(), System.currentTimeMillis());
            return;
        }

        String content = message.getContentRaw().trim().toLowerCase(Locale.ROOT);
        if (content.contains("turtybot") || content.contains("turty bot")) {
            message.addReaction(Emoji.fromFormatted("👀")).queue();
        }

        for (Pattern pattern : COMPILED_RESPOND_TO) {
            if (pattern.matcher(content).find() && ThreadLocalRandom.current().nextBoolean()) {
                message.addReaction(Emoji.fromFormatted("👋")).queue();
                return;
            }
        }
    }

    private static boolean shouldRespond(Message message) {
        long selfId = message.getJDA().getSelfUser().getIdLong();

        // If it's replying to me
        boolean isMentioned = message.getContentRaw().contains("<@" + selfId + ">");

        // If it mentions me
        if(message.getMentions().isMentioned(message.getJDA().getSelfUser()))
            isMentioned = true;

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
        for (Pattern pattern : COMPILED_RESPOND_TO) {
            if (pattern.matcher(content).find())
                return true;
        }

        return false;
    }
}
